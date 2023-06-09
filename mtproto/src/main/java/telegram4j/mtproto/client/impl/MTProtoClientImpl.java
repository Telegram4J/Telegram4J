package telegram4j.mtproto.client.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.AttributeKey;
import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.publisher.Operators;
import reactor.core.publisher.Sinks;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.annotation.Nullable;
import reactor.util.concurrent.Queues;
import telegram4j.mtproto.*;
import telegram4j.mtproto.auth.AuthorizationException;
import telegram4j.mtproto.client.ImmutableStats;
import telegram4j.mtproto.client.MTProtoClient;
import telegram4j.mtproto.client.MTProtoClientGroup;
import telegram4j.mtproto.client.MTProtoOptions;
import telegram4j.mtproto.transport.Transport;
import telegram4j.mtproto.util.AES256IGECipher;
import telegram4j.tl.TlDeserializer;
import telegram4j.tl.TlSerialUtil;
import telegram4j.tl.TlSerializer;
import telegram4j.tl.Updates;
import telegram4j.tl.api.TlMethod;
import telegram4j.tl.api.TlObject;
import telegram4j.tl.auth.Authorization;
import telegram4j.tl.auth.LoginTokenSuccess;
import telegram4j.tl.mtproto.*;
import telegram4j.tl.request.InvokeWithLayer;
import telegram4j.tl.request.account.GetPassword;
import telegram4j.tl.request.auth.CheckPassword;
import telegram4j.tl.request.auth.ExportLoginToken;
import telegram4j.tl.request.auth.ImportLoginToken;
import telegram4j.tl.request.mtproto.*;
import telegram4j.tl.request.updates.GetState;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.netty.channel.ChannelHandler.Sharable;
import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;
import static telegram4j.mtproto.transport.Transport.QUICK_ACK_MASK;
import static telegram4j.mtproto.util.CryptoUtil.*;
import static telegram4j.mtproto.util.TlEntityUtil.schemaTypeName;

public class MTProtoClientImpl implements MTProtoClient {
    private static final String HANDSHAKE_CODEC = "mtproto.handshake.codec";
    private static final String HANDSHAKE       = "mtproto.handshake";
    private static final String TRANSPORT       = "mtproto.transport";
    private static final String ENCRYPTION      = "mtproto.encryption";
    private static final String CORE            = "mtproto.core";

    private static final Logger log = Loggers.getLogger("telegram4j.mtproto.MTProtoClient");
    private static final Logger rpcLog = Loggers.getLogger("telegram4j.mtproto.rpc");

    // limit for service container like a MsgsAck, MsgsStateReq
    private static final int MAX_IDS_SIZE = 8192;
    private static final int MAX_CONTAINER_SIZE = 1020; // count of messages
    private static final int MAX_CONTAINER_LENGTH = 1 << 15; // length in bytes

    private static final Duration PING_QUERY_PERIOD = Duration.ofSeconds(5);
    private static final Duration PING_QUERY_PERIOD_MEDIA = PING_QUERY_PERIOD.multipliedBy(2);
    private static final int PING_TIMEOUT = 60;

    private static final AttributeKey<MonoSink<Void>> NOTIFY = AttributeKey.valueOf("notify");

    private static final VarHandle CHANNEL_STATE;
    static {
        var lookup = MethodHandles.lookup();
        try {
            CHANNEL_STATE = lookup.findVarHandle(MTProtoClientImpl.class, "channelState", ChannelState.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private int oldState = ChannelState.DISCONNECTED;
    private volatile ChannelState channelState = ChannelState.DISCONNECTED_STATE;
    private boolean inflightPing;
    private Trigger pingEmitter;

    private final MTProtoClientGroup group;
    private final DcId.Type type;
    private final AuthData authData;
    private final ArrayList<Long> acknowledgments = new ArrayList<>();
    private final MpscArrayQueue<RpcRequest> pendingRequests = new MpscArrayQueue<>(Queues.SMALL_BUFFER_SIZE);
    private final HashMap<Long, Request> requests = new HashMap<>();
    private final String id = Integer.toHexString(hashCode());
    private final InnerStats stats = new InnerStats();
    private final Sinks.Empty<Void> onClose = Sinks.empty();

    private final MTProtoOptions options;
    private final Bootstrap bootstrap;

    public MTProtoClientImpl(MTProtoClientGroup group, DcId.Type type, DataCenter dc, MTProtoOptions options) {
        this.group = group;
        this.type = type;
        this.authData = new AuthData(dc);
        this.options = options;

        var tcpClientRes = options.tcpClientResources();
        this.bootstrap = new Bootstrap()
                .channelFactory(tcpClientRes.getEventLoopResources().getChannelFactory())
                .group(tcpClientRes.getEventLoopGroup())
                .remoteAddress(InetSocketAddress.createUnresolved(dc.getAddress(), dc.getPort()))
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(CORE, new MTProtoClientHandler());
                    }
                });
    }

    private MsgsAck collectAcks() {
        int count = Math.min(acknowledgments.size(), MAX_IDS_SIZE);
        var batch = acknowledgments.subList(0, count);
        var ack = ImmutableMsgsAck.of(batch);
        batch.clear();
        return ack;
    }

    @Sharable
    class MTProtoClientHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable t) {
            if (t instanceof IOException) {
                log.error("[C:0x" + id + "] Internal exception: " + t.getMessage());

                logStateChange(ChannelState.DISCONNECTED);
                channelState = ChannelState.DISCONNECTED_STATE;
                ctx.close();
                return;
            }

            if (t instanceof TransportException te) {
                log.error("[C:0x" + id + "] Transport exception, code: " + te.getCode());
            } else if (t instanceof AuthorizationException) {
                log.error("[C:0x" + id + "] Exception during auth key generation", t);
            } else if (t instanceof MTProtoException) {
                log.error("[C:0x" + id + "] Validation exception", t);
            } else {
                log.error("[C:0x" + id + "] Unexpected client exception", t);
            }

            logStateChange(ChannelState.CLOSED);
            channelState = ChannelState.CLOSED_STATE;
            ctx.close();

            var sinkAttr = ctx.channel().attr(NOTIFY);
            var sink = sinkAttr.get();
            if (sink != null) {
                sink.error(t);
                sinkAttr.set(null);
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            if (log.isDebugEnabled()) {
                log.debug("[C:0x{}] Sending transport identifier to the datacenter {}", id, authData.dc());
            }

            Transport tr = options.transportFactory().create(authData.dc());
            ctx.writeAndFlush(tr.identifier(ctx.alloc()))
                    .addListener(notify -> initializeChannel(ctx, tr));
        }

        private void reconnect() {
            var currentState = channelState;
            if (currentState == ChannelState.CLOSED_STATE) {
                closeClient();
                return;
            }

            var future = bootstrap.connect();
            future.addListener(notify -> {
                Throwable t = notify.cause();
                if (t != null) {
                    if (t instanceof TransportException te) {
                        log.error("[C:0x" + id + "] Transport exception, code: " + te.getCode());
                    } else if (t instanceof AuthorizationException) {
                        log.error("[C:0x" + id + "] Exception during auth key generation", t);
                    } else if (t instanceof MTProtoException) {
                        log.error("[C:0x" + id + "] Validation exception", t);
                    } else {
                        log.error("[C:0x" + id + "] Unexpected client exception", t);
                    }

                    var sink = future.channel().attr(NOTIFY).get();
                    if (sink != null) {
                        sink.error(t);
                    }
                }
            });
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            var oldState = channelState;

            if (pingEmitter != null) {
                pingEmitter.cancel();
            }

            if (oldState.state == ChannelState.CONNECTED) {
                // connection was reset by peer; consider it as reason for reconnection
                logStateChange(ChannelState.DISCONNECTED);
            }

            if (oldState == ChannelState.CLOSED_STATE) {
                closeClient();
            } else {
                channelState = ChannelState.DISCONNECTED_STATE;

                if (log.isDebugEnabled()) {
                    log.debug("[C:0x{}] Reconnecting to the datacenter {}", id, authData.dc());
                }

                authData.resetSessionId();

                ctx.executor().schedule(this::reconnect, 5, TimeUnit.SECONDS);
            }
        }

        private void closeClient() {
            if (log.isDebugEnabled()) {
                log.debug("[C:0x{}] Disconnected from the datacenter {}", id, authData.dc());
            }
            // TODO cancel all requests
            onClose.emitEmpty(FAIL_FAST);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt instanceof HandshakeCompleteEvent event) {
                authData.authKey(event.authKey());
                authData.serverSalt(event.serverSalt());
                authData.timeOffset(event.serverTimeDiff());

                options.storeLayout().updateAuthKey(authData.dc(), event.authKey())
                        .subscribe(null, ctx::fireExceptionCaught);

                ctx.pipeline().remove(HANDSHAKE);
                ctx.pipeline().remove(HANDSHAKE_CODEC);

                configure(ctx);
            } else {
                ctx.fireUserEventTriggered(evt);
            }
        }

        private void configure(ChannelHandlerContext ctx) {
            var tr = ctx.pipeline().get(TransportCodec.class).delegate();
            ctx.pipeline().addAfter(TRANSPORT, ENCRYPTION, new MTProtoEncryption(tr));

            if (type == DcId.Type.MAIN) {
                options.storeLayout().updateDataCenter(authData.dc())
                        .subscribe(null, ctx::fireExceptionCaught);
            }

            Duration period = switch (authData.dc().getType()) {
                case MEDIA, CDN -> PING_QUERY_PERIOD_MEDIA;
                case REGULAR -> PING_QUERY_PERIOD;
            };

            pingEmitter = Trigger.create(() -> {
                if (!inflightPing) {
                    inflightPing = true;
                    ctx.writeAndFlush(new RpcRequest(ImmutablePingDelayDisconnect.of(System.nanoTime(), PING_TIMEOUT)));
                    return;
                }

                log.debug("[C:0x{}] Closing by ping timeout", id);

                logStateChange(ChannelState.DISCONNECTED);
                channelState = ChannelState.DISCONNECTED_STATE;
                ctx.close();
            }, ctx.executor(), period);

            if (authData.oldSessionId() != 0) {
                send(ctx, ImmutableDestroySession.of(authData.oldSessionId()))
                        .subscribe(null, ctx::fireExceptionCaught);
            }

            send(ctx, options.initConnection())
                    .subscribe(notify -> {
                        logStateChange(ChannelState.CONNECTED);
                        channelState = new ChannelState(ctx.channel(), ChannelState.CONNECTED);

                        var sinkAttr = ctx.channel().attr(NOTIFY);
                        var sink = sinkAttr.get();

                        // Send all pending requests on next tick
                        ctx.executor().execute(() -> {
                            if (log.isDebugEnabled()) {
                                log.debug("[C:0x{}] Sending pending requests: {}", id, pendingRequests.size());
                            }

                            // TODO send all in container
                            pendingRequests.drain(ctx::writeAndFlush);
                        });

                        if (sink != null) {
                            sink.success();
                            sinkAttr.set(null);
                        }
                    }, ctx::fireExceptionCaught);
        }

        private void initializeChannel(ChannelHandlerContext ctx, Transport tr) {
            if (type == DcId.Type.MAIN) {
                log.info("[C:0x{}] Connected to main DC {}", id, authData.dc().getId());
            } else {
                log.info("[C:0x{}] Connected to {} DC {}", id,
                        authData.dc().getType().name().toLowerCase(Locale.US),
                        authData.dc().getId());
            }

            ctx.pipeline().addFirst(TRANSPORT, new TransportCodec(tr));

            if (authData.authKey() == null) {
                options.storeLayout().getAuthKey(authData.dc())
                        .switchIfEmpty(Mono.fromRunnable(() -> ctx.executor().execute(() -> {
                            ctx.pipeline().addAfter(TRANSPORT, HANDSHAKE_CODEC, new HandshakeCodec(authData));

                            var handshakeCtx = new HandshakeContext(options.dhPrimeChecker(), options.publicRsaKeyRegister());
                            ctx.pipeline().addAfter(HANDSHAKE_CODEC, HANDSHAKE, new Handshake(id, authData, handshakeCtx));
                        })))
                        .subscribe(loaded -> ctx.executor().execute(() -> {
                            authData.authKey(loaded);
                            configure(ctx);
                        }), ctx::fireExceptionCaught);
            } else {
                configure(ctx);
            }
        }
    }

    private void logStateChange(int newState) {
        if (log.isDebugEnabled()) {
            log.debug("[C:0x{}] Updating state: {}->{}", id, ChannelState.stateName(oldState), ChannelState.stateName(newState));
        }
        oldState = newState;
    }

    @Override
    public Mono<Void> connect() {
        return Mono.create(sink -> {
            var currentState = channelState;
            if (currentState == ChannelState.CLOSED_STATE) {
                sink.error(new MTProtoException("Client has been closed"));
                return;
            } else if (currentState != ChannelState.DISCONNECTED_STATE) {
                sink.error(new MTProtoException("Client already connected"));
                return;
            }

            bootstrap.attr(NOTIFY, sink);
            bootstrap.connect()
                    .addListener(notify -> {
                        Throwable t = notify.cause();
                        if (t != null) {
                            if (t instanceof TransportException te) {
                                log.error("[C:0x" + id + "] Transport exception, code: " + te.getCode());
                            } else if (t instanceof AuthorizationException) {
                                log.error("[C:0x" + id + "] Exception during auth key generation", t);
                            } else if (t instanceof MTProtoException) {
                                log.error("[C:0x" + id + "] Validation exception", t);
                            } else {
                                log.error("[C:0x" + id + "] Unexpected client exception", t);
                            }

                            sink.error(t);
                        }
                    });
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> Mono<R> sendAwait(TlMethod<? extends R> method) {
        return Mono.defer(() -> {
            var currentState = channelState;

            if (currentState == ChannelState.CLOSED_STATE) {
                return Mono.error(new MTProtoException("Client has been closed"));
            } else if (currentState == ChannelState.DISCONNECTED_STATE) {
                if (!isResultAwait(method)) {
                    if (!pendingRequests.offer(new RpcRequest(method))) {
                        return Mono.error(new DiscardedRpcRequestException(method));
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("[C:0x{}] Delaying request: {}", id, schemaTypeName(method));
                    }
                    return Mono.empty();
                }

                RequestMono sink = new RequestMono(false);
                if (!pendingRequests.offer(new RpcQuery(method, sink))) {
                    return Mono.error(new DiscardedRpcRequestException(method));
                }

                if (log.isDebugEnabled()) {
                    log.debug("[C:0x{}] Delaying request: {}", id, schemaTypeName(method));
                }
                return (Mono<R>) sink;
            } else { // CONNECTED
                var ch = channelState.channel;
                if (ch == null) {
                    return Mono.error(new MTProtoException("Illegal client state"));
                }

                if (!isResultAwait(method)) {
                    ch.writeAndFlush(new RpcRequest(method));
                    return Mono.empty();
                }

                RequestMono sink = new RequestMono(false);
                ch.writeAndFlush(new RpcQuery(method, sink));
                return (Mono<R>) sink;
            }
        })
        .transform(mono -> {
            for (ResponseTransformer tr : options.responseTransformers()) {
                mono = tr.transform(mono, method);
            }
            return mono;
        });
    }

    @SuppressWarnings("unchecked")
    private <R> Mono<R> send(ChannelHandlerContext ctx, TlMethod<R> method) {
        if (!isResultAwait(method)) {
            ctx.channel().writeAndFlush(new RpcRequest(method));
            return Mono.empty();
        }

        RequestMono sink = new RequestMono(true);
        ctx.channel().writeAndFlush(new RpcQuery(method, sink));
        return (Mono<R>) sink;
    }

    @Override
    public DataCenter dc() {
        return authData.dc();
    }

    @Override
    public DcId.Type type() {
        return type;
    }

    @Override
    public Stats stats() {
        return stats;
    }

    @Override
    public Mono<Void> close() {
        return Mono.create(sink -> {
            var oldCs = (ChannelState) CHANNEL_STATE.getAndSet(this, ChannelState.CLOSED_STATE);
            if (oldCs == ChannelState.CLOSED_STATE) {
                sink.success();
            } else if (oldCs == ChannelState.DISCONNECTED_STATE) {
                onClose.emitEmpty(FAIL_FAST);
                // TODO cancel pending requests
                sink.success();
            } else {
                EventLoop eventLoop = oldCs.channel.eventLoop();

                eventLoop.execute(() -> {
                    logStateChange(ChannelState.CLOSED);
                    channelState = ChannelState.CLOSED_STATE;

                    cancelRequests(eventLoop);

                    oldCs.channel.close()
                            .addListener(notify -> {
                                Throwable t = notify.cause();
                                if (t != null) {
                                    sink.error(t);
                                } else if (notify.isSuccess()) {
                                    sink.success();
                                }
                            });
                });
            }
        });
    }

    @Override
    public Mono<Void> onClose() {
        return onClose.asMono();
    }

    private void cancelRequests(EventLoop eventLoop) {
        RuntimeException exc = Exceptions.failWithCancel();
        for (var e : requests.entrySet()) {
            var req = e.getValue();
            if (req instanceof RpcQuery q) {
                var resultPublisher = q.sink.isPublishOnEventLoop()
                        ? eventLoop
                        : options.resultPublisher();

                q.sink.emitError(resultPublisher, exc);
            }
        }

        pendingRequests.drain(request -> {
            if (request instanceof RpcQuery q) {
                var resultPublisher = q.sink.isPublishOnEventLoop()
                        ? eventLoop
                        : options.resultPublisher();

                q.sink.emitError(resultPublisher, exc);
            }
        });


    }

    static boolean isResultAwait(TlMethod<?> object) {
        return switch (object.identifier()) {
            case MsgsAck.ID:
            case DestroySession.ID:
                // for this message MsgsStateInfo is response
                // case MsgsStateReq.ID:
                yield false;
            default:
                yield true;
        };
    }

    // request types

    static sealed class RpcRequest implements Request {
        final TlMethod<?> method;

        RpcRequest(TlMethod<?> method) {
            this.method = method;
        }

        @Override
        public String toString() {
            return "RpcRequest{" + schemaTypeName(method) + '}';
        }
    }

    static sealed class RpcQuery extends RpcRequest {
        final RequestMono sink;

        RpcQuery(TlMethod<?> method, RequestMono sink) {
            super(method);
            this.sink = sink;
        }

        @Override
        public String toString() {
            return "RpcQuery{" + schemaTypeName(method) + '}';
        }
    }

    sealed interface Request {}

    static final class ContainerRequest implements Request {
        final long[] msgIds;
        // The counter of messages for which response has not received yet
        short cnt;

        ContainerRequest(long[] msgIds) {
            this.msgIds = msgIds;
            this.cnt = (short) msgIds.length;
        }

        boolean decrementCnt() {
            return --cnt <= 0;
        }

        @Override
        public String toString() {
            return "ContainerRequest{" +
                    "msgIds=" + Arrays.stream(msgIds)
                    .mapToObj(s -> "0x" + Long.toHexString(s))
                    .collect(Collectors.joining(", ", "[", "]")) +
                    '}';
        }
    }

    sealed interface ContainerizedRequest extends Request {

        long containerMsgId();

        TlMethod<?> method();
    }

    static final class RpcContainerRequest extends RpcRequest implements ContainerizedRequest {
        final long containerMsgId;

        RpcContainerRequest(TlMethod<?> request, long containerMsgId) {
            super(request);
            this.containerMsgId = containerMsgId;
        }

        RpcContainerRequest(RpcRequest request, long containerMsgId) {
            this(request.method, containerMsgId);
        }

        @Override
        public String toString() {
            return "RpcContainerRequest{" +
                    "containerMsgId=0x" + Long.toHexString(containerMsgId) +
                    ", method=" + schemaTypeName(method) +
                    '}';
        }

        @Override
        public TlMethod<?> method() {
            return method;
        }

        @Override
        public long containerMsgId() {
            return containerMsgId;
        }
    }

    static final class QueryContainerRequest extends RpcQuery implements ContainerizedRequest {
        final long containerMsgId;

        QueryContainerRequest(RpcQuery query, long containerMsgId) {
            super(query.method, query.sink);
            this.containerMsgId = containerMsgId;
        }

        @Override
        public String toString() {
            return "QueryContainerRequest{" +
                    "containerMsgId=0x" + Long.toHexString(containerMsgId) +
                    ", method=" + schemaTypeName(method) +
                    '}';
        }

        @Override
        public TlMethod<?> method() {
            return method;
        }

        @Override
        public long containerMsgId() {
            return containerMsgId;
        }
    }

    private void emitUnwrappedContainer(ChannelHandlerContext ctx, ContainerRequest container) {
        for (long msgId : container.msgIds) {
            var inner = (RpcRequest) requests.remove(msgId);
            // If inner is null this mean response for mean was received
            if (inner != null) {
                // This method was called from MessageStateInfo handling;
                // Failed to send acks, just give back to queue
                if (inner.method instanceof MsgsAck acks) {
                    acknowledgments.addAll(acks.msgIds());
                    // There is no need to resend this requests,
                    // because it computed on relevant 'requests' map
                } else if (inner.method.identifier() == MsgsStateReq.ID) {
                    continue;
                } else {
                    RpcRequest single = inner instanceof QueryContainerRequest query
                            ? new RpcQuery(query.method, query.sink)
                            : new RpcRequest(inner.method);

                    ctx.channel().writeAndFlush(single);
                }
            }
        }
    }

    private void emitUnwrapped(ChannelHandlerContext ctx, long possibleCntMsgId) {
        Request request = requests.get(possibleCntMsgId);
        if (request instanceof ContainerRequest container) {
            requests.remove(possibleCntMsgId);

            emitUnwrappedContainer(ctx, container);
        } else if (request instanceof ContainerizedRequest cntMessage) {
            var cnt = (ContainerRequest) requests.remove(cntMessage.containerMsgId());
            if (cnt != null) {
                emitUnwrappedContainer(ctx, cnt);
            }
        } else if (request instanceof RpcRequest rpcRequest) {
            requests.remove(possibleCntMsgId);

            ctx.channel().writeAndFlush(rpcRequest);
        }
    }

    static class InnerStats implements Stats {
        static final VarHandle QUERIES_COUNT;

        static {
            try {
                var l = MethodHandles.lookup();
                QUERIES_COUNT = l.findVarHandle(InnerStats.class, "queriesCount", int.class);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        volatile Instant lastQueryTimestamp;
        volatile int queriesCount;

        void incrementQueriesCount() {
            QUERIES_COUNT.getAndAdd(this, 1);
        }

        void decrementQueriesCount() {
            QUERIES_COUNT.getAndAdd(this, -1);
        }

        @Override
        public Optional<Instant> lastQueryTimestamp() {
            return Optional.ofNullable(lastQueryTimestamp);
        }

        @Override
        public int queriesCount() {
            return queriesCount;
        }

        @Override
        public Stats copy() {
            return new ImmutableStats(lastQueryTimestamp, queriesCount);
        }

        @Override
        public String toString() {
            return "Stats{" +
                    "lastQueryTimestamp=" + lastQueryTimestamp +
                    ", queriesCount=" + queriesCount +
                    '}';
        }
    }

    class MTProtoEncryption extends ChannelDuplexHandler {
        private final Transport transport;
        private final ArrayDeque<RpcQuery> delayedUntilAuth = new ArrayDeque<>();

        MTProtoEncryption(Transport transport) {
            this.transport = transport;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException {
            if (!(msg instanceof ByteBuf payload)) {
                throw new IllegalArgumentException("Unexpected type of message to decrypt: " + msg);
            }

            if (payload.readableBytes() == 4) {
                int val = payload.readIntLE();
                payload.release();

                if (!TransportException.isError(val) && transport.supportsQuickAck()) {
                    if (rpcLog.isDebugEnabled()) {
                        rpcLog.debug("[C:0x{}, Q:0x{}] Received quick ack",
                                id, Integer.toHexString(val));
                    }
                    return;
                }

                throw new TransportException(val);
            }

            decryptPayload(ctx, payload);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws IOException {
            if (!(msg instanceof RpcRequest req)) {
                throw new IllegalArgumentException("Unexpected type of message to encrypt: " + msg);
            }

            if (!isPingPacket(req.method)) {
                stats.incrementQueriesCount();
                stats.lastQueryTimestamp = Instant.now();
            }

            if (log.isTraceEnabled() && !requests.isEmpty()) {
                log.trace("[C:0x{}] {}", id, requests.entrySet().stream()
                        .map(e -> "0x" + Long.toHexString(e.getKey()) + ": " + e.getValue())
                        .toList().toString());
            }

            if (authData.unauthorized() && msg instanceof RpcQuery query
                    && query.method != GetState.instance()
                    && !isAuthMethod(query.method)) {

                delayedUntilAuth.addLast(query);
                return;
            }

            var currentAuthKey = authData.authKey();
            if (currentAuthKey == null) {
                throw new MTProtoException("No auth key");
            }

            TlObject method = req.method;
            int size = TlSerializer.sizeOf(req.method);
            if (size >= options.gzipWrappingSizeThreshold()) {
                ByteBuf serialized = ctx.alloc().ioBuffer(size);
                TlSerializer.serialize(serialized, method);
                ByteBuf gzipped = TlSerialUtil.compressGzip(ctx.alloc(), 9, serialized);

                method = ImmutableGzipPacked.of(gzipped);
                gzipped.release();

                size = TlSerializer.sizeOf(method);
            }

            long containerMsgId = -1;
            // server returns -404 transport error when this packet placed in container
            boolean canContainerize = req.method.identifier() != InvokeWithLayer.ID && size < MAX_CONTAINER_LENGTH;

            Request containerOrRequest = req;
            long requestMessageId = authData.nextMessageId();
            int requestSeqNo = authData.nextSeqNo(req.method);

            ByteBuf message;
            int padding;
            var statesIds = new ArrayList<Long>();
            var messages = new ArrayList<ContainerMessage>();
            if (canContainerize) {
                for (var e : requests.entrySet()) {
                    long key = e.getKey();
                    var inf = e.getValue();
                    if (inf instanceof ContainerRequest) {
                        continue;
                    }

                    statesIds.add(key);
                    if (statesIds.size() == MAX_IDS_SIZE) {
                        break;
                    }
                }
            }

            if (canContainerize && !statesIds.isEmpty())
                messages.add(new ContainerMessage(authData.nextMessageId(),
                        authData.nextSeqNo(false), ImmutableMsgsStateReq.of(statesIds)));
            if (canContainerize && !acknowledgments.isEmpty())
                messages.add(new ContainerMessage(authData.nextMessageId(),
                        authData.nextSeqNo(false), collectAcks()));

            boolean containerize = canContainerize && !messages.isEmpty();
            if (containerize) {
                messages.add(new ContainerMessage(requestMessageId, requestSeqNo, size, method));

                containerMsgId = authData.nextMessageId();
                int containerSeqNo = authData.nextSeqNo(false);
                int payloadSize = messages.stream().mapToInt(c -> c.size + 16).sum();
                int messageSize = 40 + payloadSize;
                int unpadded = (messageSize + 12) % 16;
                padding = 12 + (unpadded != 0 ? 16 - unpadded : 0);

                message = ctx.alloc().buffer(messageSize + padding);
                message.writeLongLE(authData.serverSalt());
                message.writeLongLE(authData.sessionId());
                message.writeLongLE(containerMsgId);
                message.writeIntLE(containerSeqNo);
                message.writeIntLE(payloadSize + 8);
                message.writeIntLE(MessageContainer.ID);
                message.writeIntLE(messages.size());

                var rpcInCont = req instanceof RpcQuery r
                        ? new QueryContainerRequest(r, containerMsgId)
                        : new RpcContainerRequest(req, containerMsgId);
                requests.put(requestMessageId, rpcInCont);
                var msgIds = new long[messages.size()];
                for (int i = 0; i < messages.size(); i++) {
                    var c = messages.get(i);
                    msgIds[i] = c.messageId;
                    if (c.messageId != requestMessageId) {
                        requests.put(c.messageId, new RpcContainerRequest((TlMethod<?>) c.method, containerMsgId));
                    }

                    message.writeLongLE(c.messageId);
                    message.writeIntLE(c.seqNo);
                    message.writeIntLE(c.size);
                    TlSerializer.serialize(message, c.method);
                }

                containerOrRequest = new ContainerRequest(msgIds);
                requests.put(containerMsgId, containerOrRequest);
            } else {
                requests.put(requestMessageId, req);
                int messageSize = 32 + size;
                int unpadded = (messageSize + 12) % 16;
                padding = 12 + (unpadded != 0 ? 16 - unpadded : 0);

                message = ctx.alloc().buffer(messageSize + padding)
                        .writeLongLE(authData.serverSalt())
                        .writeLongLE(authData.sessionId())
                        .writeLongLE(requestMessageId)
                        .writeIntLE(requestSeqNo)
                        .writeIntLE(size);
                TlSerializer.serialize(message, method);
            }

            byte[] paddingb = new byte[padding];
            random.nextBytes(paddingb);
            message.writeBytes(paddingb);

            ByteBuf authKey = currentAuthKey.value();
            ByteBuf authKeyId = Unpooled.copyLong(Long.reverseBytes(currentAuthKey.id()));

            ByteBuf messageKeyHash = sha256Digest(authKey.slice(88, 32), message);

            boolean quickAck = false;
            int quickAckToken = -1;
            if (!containerize && AuthData.isContentRelated(req.method) && transport.supportsQuickAck()) {
                quickAckToken = messageKeyHash.getIntLE(0) | QUICK_ACK_MASK;
                quickAck = true;
            }

            ByteBuf messageKey = messageKeyHash.slice(8, 16);
            AES256IGECipher cipher = createAesCipher(messageKey, authKey, false);

            ByteBuf encrypted = cipher.encrypt(message);
            ByteBuf packet = Unpooled.wrappedBuffer(authKeyId, messageKey, encrypted);

            if (rpcLog.isDebugEnabled()) {
                if (containerOrRequest instanceof ContainerRequest) {
                    rpcLog.debug("[C:0x{}, M:0x{}] Sending container: {{}}", id,
                            Long.toHexString(containerMsgId), messages.stream()
                                    .map(m -> "0x" + Long.toHexString(m.messageId) + ": " + schemaTypeName(m.method))
                                    .collect(Collectors.joining(", ")));
                } else {
                    if (quickAck) {
                        rpcLog.debug("[C:0x{}, M:0x{}, Q:0x{}] Sending request: {}", id,
                                Long.toHexString(requestMessageId), Integer.toHexString(quickAckToken),
                                schemaTypeName(req.method));
                    } else {
                        rpcLog.debug("[C:0x{}, M:0x{}] Sending request: {}", id,
                                Long.toHexString(requestMessageId), schemaTypeName(req.method));
                    }
                }
            }

            ctx.channel().attr(TransportCodec.quickAck).set(quickAck);
            ctx.writeAndFlush(packet);
        }

        private void decryptPayload(ChannelHandlerContext ctx, ByteBuf data) throws IOException {
            long authKeyId = data.readLongLE();

            var currentAuthKey = authData.authKey();
            if (currentAuthKey == null) {
                throw new MTProtoException("No auth key");
            }

            if (authKeyId != currentAuthKey.id()) {
                throw new MTProtoException("Incorrect auth key id");
            }

            ByteBuf messageKey = data.readRetainedSlice(16);

            ByteBuf authKey = currentAuthKey.value();
            AES256IGECipher cipher = createAesCipher(messageKey, authKey, true);

            ByteBuf decrypted = cipher.decrypt(data.slice());

            ByteBuf messageKeyHash = sha256Digest(authKey.slice(96, 32), decrypted);
            ByteBuf messageKeyHashSlice = messageKeyHash.slice(8, 16);

            if (!messageKey.equals(messageKeyHashSlice)) {
                messageKey.release();
                throw new MTProtoException("Incorrect message key");
            }
            messageKey.release();

            decrypted.readLongLE();  // server_salt
            long sessionId = decrypted.readLongLE();
            if (authData.sessionId() != sessionId) {
                throw new MTProtoException("Incorrect session identifier");
            }
            long messageId = decrypted.readLongLE();
            var res = authData.isValidInboundMessageId(messageId);
            if (res != null) {
                String reason = switch (res) {
                    case DUPLICATE -> "Duplicate";
                    case INVALID_TIME -> "Too old or too new";
                    case EVEN -> "Even";
                };

                throw new MTProtoException(reason + " message id received: 0x" + Long.toHexString(messageId));
            }

            decrypted.readIntLE(); // seq_no
            int length = decrypted.readIntLE();
            if (length % 4 != 0) {
                throw new MTProtoException("Data isn't aligned by 4 bytes");
            }

            ByteBuf payload = decrypted.readSlice(length);
            if (decrypted.readableBytes() < 12 || decrypted.readableBytes() > 1024) {
                throw new MTProtoException("Invalid padding length");
            }

            TlObject obj;
            try {
                obj = TlDeserializer.deserialize(payload);
            } finally {
                decrypted.release();
            }

            handleServiceMessage(ctx, obj, messageId);
        }

        private Object decompressIfApplicable(Object obj) throws IOException {
            return obj instanceof GzipPacked gzipPacked
                    ? TlSerialUtil.decompressGzip(gzipPacked.packedData())
                    : obj;
        }

        static RpcException createRpcException(RpcError error, RpcRequest request) {
            String format = String.format("%s returned code: %d, message: %s",
                    schemaTypeName(request.method), error.errorCode(),
                    error.errorMessage());

            return new RpcException(format, error, request.method);
        }

        private boolean handleMsgsAck(Object obj, long messageId) {
            if (obj instanceof MsgsAck msgsAck) {
                if (rpcLog.isDebugEnabled()) {
                    rpcLog.debug("[C:0x{}, M:0x{}] Received acknowledge for message(s): [{}]",
                            id, Long.toHexString(messageId), msgsAck.msgIds().stream()
                                    .map(l -> String.format("0x%x", l))
                                    .collect(Collectors.joining(", ")));
                }
                return true;
            }
            return false;
        }

        private void decContainer(RpcRequest req) {
            if (req instanceof ContainerizedRequest aux) {
                var cnt = (ContainerRequest) requests.get(aux.containerMsgId());
                if (cnt != null && cnt.decrementCnt()) {
                    requests.remove(aux.containerMsgId());
                }
            }
        }

        private void handleServiceMessage(ChannelHandlerContext ctx, Object obj, long messageId) throws IOException {
            if (obj instanceof RpcResult rpcResult) {
                messageId = rpcResult.reqMsgId();
                obj = decompressIfApplicable(rpcResult.result());

                var query = (RpcQuery) requests.remove(messageId);
                if (query == null) {
                    return;
                }

                stats.decrementQueriesCount();
                decContainer(query);
                acknowledgments.add(messageId);

                if (obj instanceof RpcError rpcError) {
                    if (rpcError.errorCode() == 401) {
                        authData.unauthorized(true);
                    }

                    if (rpcLog.isDebugEnabled()) {
                        rpcLog.debug("[C:0x{}, M:0x{}] Receiving rpc error, code: {}, message: {}",
                                id, Long.toHexString(messageId), rpcError.errorCode(), rpcError.errorMessage());
                    }

                    RpcException e = createRpcException(rpcError, query);

                    if (query.sink.isPublishOnEventLoop()) {
                        query.sink.emitError(e);
                    } else {
                        query.sink.emitError(options.resultPublisher(), e);
                    }
                } else {
                    if (rpcLog.isDebugEnabled()) {
                        rpcLog.debug("[C:0x{}, M:0x{}] Receiving rpc result", id, Long.toHexString(messageId));
                    }

                    if (query.sink.isPublishOnEventLoop()) {
                        query.sink.emitValue(obj);
                    } else {
                        query.sink.emitValue(options.resultPublisher(), obj);
                    }

                    if (authData.unauthorized() && (obj instanceof Authorization || obj instanceof LoginTokenSuccess)) {
                        authData.unauthorized(false);

                        RpcQuery pendingQuery;
                        // TODO send all in container
                        while ((pendingQuery = delayedUntilAuth.poll()) != null) {
                            ctx.channel().writeAndFlush(pendingQuery);
                        }
                    }
                }

                return;
            }

            if (obj instanceof MessageContainer messageContainer) {
                if (rpcLog.isDebugEnabled()) {
                    rpcLog.debug("[C:0x{}] Handling message container: {}", id, messageContainer.messages().stream()
                            .map(msg -> "0x" + Long.toHexString(msg.msgId()) + ": " + schemaTypeName(msg.body()))
                            .collect(Collectors.joining(", ", "{", "}")));
                }

                for (Message message : messageContainer.messages()) {
                    handleServiceMessage(ctx, message.body(), message.msgId());
                }
                return;
            }

            // Applicable for updates
            obj = decompressIfApplicable(obj);
            if (obj instanceof Updates updates) {
                if (rpcLog.isTraceEnabled()) {
                    rpcLog.trace("[C:0x{}] Receiving updates: {}", id, updates);
                }

                group.updates().publish(updates);
                return;
            }

            if (obj instanceof Pong pong) {
                messageId = pong.msgId();

                if (rpcLog.isDebugEnabled()) {
                    rpcLog.debug("[C:0x{}, M:0x{}] Receiving pong after {}", id, Long.toHexString(messageId),
                            Duration.ofNanos(System.nanoTime() - pong.pingId()));
                }

                var query = (RpcRequest) requests.remove(messageId);
                decContainer(query);
                inflightPing = false;

                if (query instanceof RpcQuery q) {
                    if (q.sink.isPublishOnEventLoop()) {
                        q.sink.emitValue(obj);
                    } else {
                        q.sink.emitValue(options.resultPublisher(), obj);
                    }
                }

                return;
            }

            // TODO: necessary?
            // if (obj instanceof FutureSalts) {
            //     FutureSalts futureSalts = (FutureSalts) obj;
            //     messageId = futureSalts.reqMsgId();
            //     if (rpcLog.isDebugEnabled()) {
            //         rpcLog.debug("[C:0x{}, M:0x{}] Receiving future salts", id, Long.toHexString(messageId));
            //     }
            //
            //     resolve(messageId, obj);
            //     return;
            // }

            if (obj instanceof NewSession newSession) {
                if (rpcLog.isDebugEnabled()) {
                    rpcLog.debug("[C:0x{}] Receiving new session creation, first message id: 0x{}",
                            id, Long.toHexString(newSession.firstMsgId()));
                }

                authData.serverSalt(newSession.serverSalt());
                authData.lastMessageId(newSession.firstMsgId());
                acknowledgments.add(messageId);

                return;
            }

            // from MessageContainer
            if (handleMsgsAck(obj, messageId)) {
                return;
            }

            if (obj instanceof BadMsgNotification badMsgNotification) {
                if (rpcLog.isDebugEnabled()) {
                    if (badMsgNotification instanceof BadServerSalt badServerSalt) {
                        rpcLog.debug("[C:0x{}, M:0x{}] Updating server salt", id,
                                Long.toHexString(badServerSalt.badMsgId()));
                    } else {
                        rpcLog.debug("[C:0x{}, M:0x{}] Receiving notification, code: {}", id,
                                Long.toHexString(badMsgNotification.badMsgId()), badMsgNotification.errorCode());
                    }
                }

                if (badMsgNotification instanceof BadServerSalt badServerSalt) {
                    authData.serverSalt(badServerSalt.newServerSalt());
                }

                authData.updateTimeOffset((int) (messageId >> 32));
                emitUnwrapped(ctx, badMsgNotification.badMsgId());
                return;
            }

            if (obj instanceof MsgsStateInfo inf) {
                var req = (RpcRequest) requests.remove(inf.reqMsgId());
                if (req != null) {
                    MsgsStateReq original = (MsgsStateReq) req.method;
                    ByteBuf c = inf.info();
                    if (original.msgIds().size() != c.readableBytes()) {
                        rpcLog.error("[C:0x{}, M:0x{}] Received not all states. expected: {}, received: {}",
                                id, Long.toHexString(inf.reqMsgId()), original.msgIds().size(),
                                c.readableBytes());
                        return;
                    }

                    if (rpcLog.isDebugEnabled()) {
                        StringJoiner st = new StringJoiner(", ");
                        var msgIds = original.msgIds();
                        for (int i = 0; i < msgIds.size(); i++) {
                            long msgId = msgIds.get(i);
                            st.add("0x" + Long.toHexString(msgId) + "/" + (c.getByte(i) & 7));
                        }

                        rpcLog.debug("[C:0x{}, M:0x{}] Received states: [{}]", id, Long.toHexString(inf.reqMsgId()), st);
                    }

                    decContainer(req);
                    var msgIds = original.msgIds();
                    for (int i = 0; i < msgIds.size(); i++) {
                        long msgId = msgIds.get(i);

                        int state = c.getByte(i) & 7;
                        switch (state) {
                            // not received, resend
                            case 1, 2, 3 -> emitUnwrapped(ctx, msgId);
                            case 4 -> { // acknowledged
                                var sub = (RpcRequest) requests.get(msgId);
                                if (sub == null) {
                                    continue;
                                }
                                if (!isResultAwait(sub.method)) {
                                    requests.remove(msgId);
                                    decContainer(sub);
                                }
                            }
                            default -> rpcLog.debug("[C:0x{}] Unknown message state {}", id, state);
                        }
                    }
                }
                return;
            }

            if (obj instanceof MsgDetailedInfo info) {
                if (info instanceof BaseMsgDetailedInfo base) {
                    if (rpcLog.isDebugEnabled()) {
                        rpcLog.debug("[C:0x{}] Handling message info. msgId: 0x{}, answerId: 0x{}", id,
                                Long.toHexString(base.msgId()), Long.toHexString(base.answerMsgId()));
                    }
                    // acknowledge for base.msgId()
                } else {
                    if (rpcLog.isDebugEnabled()) {
                        rpcLog.debug("[C:0x{}] Handling message info. answerId: 0x{}", id, Long.toHexString(info.answerMsgId()));
                    }
                }

                // TODO
                return;
            }

            if (obj instanceof DestroySessionRes res) {
                // Why DestroySession have concrete type of response, but also have a wrong message_id
                // which can't be used as key of the requests map?

                if (rpcLog.isDebugEnabled()) {
                    rpcLog.debug("[C:0x{}] Session 0x{} destroyed {}",
                            id, Long.toHexString(res.sessionId()),
                            res.identifier() == DestroySessionOk.ID ? "successfully" : "with nothing");
                }

                return;
            }

            log.warn("[C:0x{}] Unhandled payload: {}", id, obj);
        }
    }

    private static boolean isPingPacket(TlMethod<?> method) {
        return switch (method.identifier()) {
            case PingDelayDisconnect.ID, Ping.ID -> true;
            default -> false;
        };
    }

    static boolean isAuthMethod(TlMethod<?> method) {
        return switch (method.identifier()) {
            case ImportLoginToken.ID:
            case ExportLoginToken.ID:
            case GetPassword.ID:
            case CheckPassword.ID:
                yield true;
            default:
                yield false;
        };
    }

    record ContainerMessage(long messageId, int seqNo, int size, TlObject method) {

        ContainerMessage(long messageId, int seqNo, TlObject method) {
            this(messageId, seqNo, TlSerializer.sizeOf(method), method);
        }
    }

    static class RequestMono extends Mono<Object> implements Subscription {
        static final VarHandle ONCE;

        static {
            var lookup = MethodHandles.lookup();
            try {
                ONCE = lookup.findVarHandle(RequestMono.class, "once", boolean.class);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        final boolean publishOnEventLoop;

        volatile boolean once;
        volatile boolean cancelled;

        CoreSubscriber<? super Object> subscriber;

        RequestMono(boolean publishOnEventLoop) {
            this.publishOnEventLoop = publishOnEventLoop;
        }

        public boolean isPublishOnEventLoop() {
            return publishOnEventLoop;
        }

        @Override
        public void subscribe(CoreSubscriber<? super Object> actual) {
            if (!once && ONCE.compareAndSet(this, false, true)) {
                subscriber = actual;
                actual.onSubscribe(this);
            } else {
                Operators.error(actual, new IllegalStateException("RequestMono allows only a single Subscriber"));
            }
        }

        public void emitValue(Object obj) {
            if (cancelled) {
                return;
            }

            subscriber.onNext(obj);
            subscriber.onComplete();
        }

        public void emitValue(ExecutorService resultPublisher, Object obj) {
            if (cancelled) {
                return;
            }

            resultPublisher.execute(() -> {
                subscriber.onNext(obj);
                subscriber.onComplete();
            });
        }

        public void emitError(RuntimeException e) {
            if (cancelled) {
                return;
            }

            subscriber.onError(e);
        }

        public void emitError(ExecutorService resultPublisher, RuntimeException e) {
            if (cancelled) {
                return;
            }

            resultPublisher.execute(() -> subscriber.onError(e));
        }

        @Override
        public void request(long n) {
            Operators.validate(n);
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }

    static class ChannelState {
        public static final int DISCONNECTED = 0;
        public static final int CONNECTED = 1;
        public static final int CLOSED = 2;

        static final ChannelState DISCONNECTED_STATE = new ChannelState(null, DISCONNECTED);
        static final ChannelState CLOSED_STATE = new ChannelState(null, CLOSED);

        final Channel channel;
        final int state;

        ChannelState(@Nullable Channel channel, int state) {
            this.channel = channel;
            this.state = state;
        }

        private static String stateName(int state) {
            return switch (state) {
                case DISCONNECTED -> "DISCONNECTED";
                case CONNECTED -> "CONNECTED";
                case CLOSED -> "CLOSED";
                default -> throw new IllegalStateException();
            };
        }
    }
}
