package telegram4j.mtproto.client.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.AttributeKey;
import io.netty.util.NetUtil;
import io.netty.util.concurrent.EventExecutor;
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
import telegram4j.tl.TlSerializer;
import telegram4j.tl.api.TlMethod;
import telegram4j.tl.api.TlObject;
import telegram4j.tl.mtproto.MsgsAck;
import telegram4j.tl.request.account.GetPassword;
import telegram4j.tl.request.auth.CheckPassword;
import telegram4j.tl.request.auth.ExportLoginToken;
import telegram4j.tl.request.auth.ImportLoginToken;
import telegram4j.tl.request.mtproto.*;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.netty.channel.ChannelHandler.Sharable;
import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;
import static telegram4j.mtproto.util.TlEntityUtil.schemaTypeName;

public class MTProtoClientImpl implements MTProtoClient {
    static final String HANDSHAKE_CODEC = "mtproto.handshake.codec";
    static final String HANDSHAKE       = "mtproto.handshake";
    static final String TRANSPORT       = "mtproto.transport";
    static final String ENCRYPTION      = "mtproto.encryption";
    static final String CORE            = "mtproto.core";

    static final Logger log = Loggers.getLogger("telegram4j.mtproto.MTProtoClient");
    static final Logger rpcLog = Loggers.getLogger("telegram4j.mtproto.rpc");

    static final int PING_TIMEOUT = 60;

    static final AttributeKey<MonoSink<Void>> NOTIFY = AttributeKey.valueOf("notify");

    static final VarHandle CHANNEL_STATE;
    static {
        var lookup = MethodHandles.lookup();
        try {
            CHANNEL_STATE = lookup.findVarHandle(MTProtoClientImpl.class, "channelState", ChannelState.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    int oldState = ChannelState.DISCONNECTED;
    volatile ChannelState channelState = ChannelState.DISCONNECTED_STATE;
    boolean inflightPing;
    Trigger pingEmitter;

    final MTProtoClientGroup group;
    final DcId.Type type;
    final AuthData authData;
    final MpscArrayQueue<RpcQuery> pendingRequests = new MpscArrayQueue<>(Queues.SMALL_BUFFER_SIZE);
    final HashMap<Long, Request> requests = new HashMap<>();
    final ArrayDeque<RpcQuery> delayedUntilAuth = new ArrayDeque<>(16);
    final String id = Integer.toHexString(hashCode());
    final InnerStats stats = new InnerStats();
    final Sinks.Empty<Void> onClose = Sinks.empty();

    final MTProtoOptions mtProtoOptions;
    final Options options;
    final Bootstrap bootstrap;

    public MTProtoClientImpl(MTProtoClientGroup group, DcId.Type type,
                             DataCenter dc, MTProtoOptions mtProtoOptions,
                             Options options) {
        this.group = group;
        this.type = type;
        this.authData = new AuthData(dc);
        this.mtProtoOptions = mtProtoOptions;
        this.options = options;

        var tcpClientRes = mtProtoOptions.tcpClientResources();
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

    @Sharable
    class MTProtoClientHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable t) {
            if (t instanceof IOException) {
                log.error("[C:0x" + id + "] Internal exception: " + t);

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
                log.debug("[C:0x{}] Sending transport identifier to DC {} ({})",
                        id, authData.dc().getId(), NetUtil.toSocketAddressString(authData.dc().getAddress(), authData.dc().getPort()));
            }

            Transport tr = options.transportFactory().create(authData.dc());
            ctx.writeAndFlush(tr.identifier(ctx.alloc()))
                    .addListener(notify -> {
                        Throwable cause = notify.cause();
                        if (cause != null) {
                            ctx.fireExceptionCaught(cause);
                        } else if (notify.isSuccess()) {
                            initializeChannel(ctx, tr);
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
                closeClient(ctx.executor());
            } else {
                channelState = ChannelState.DISCONNECTED_STATE;

                log.info("[C:0x{}] Reconnecting to DC {}", id, authData.dc().getId());

                authData.resetSessionId();

                ctx.executor().schedule(() -> reconnect(ctx), options.reconnectionInterval().toNanos(), TimeUnit.NANOSECONDS);
            }
        }

        void reconnect(ChannelHandlerContext oldCtx) {
            var currentState = channelState;
            if (currentState == ChannelState.CLOSED_STATE) {
                closeClient(oldCtx.executor());
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
                    } else if (t instanceof ConnectException) {
                        if (t instanceof ConnectTimeoutException) {
                            log.error("[C:0x" + id + "] Connection timed out");
                        } else {
                            log.error("[C:0x" + id + "] Connect exception", t);
                        }

                        oldCtx.executor().schedule(() -> reconnect(oldCtx),
                                options.reconnectionInterval().toNanos(), TimeUnit.NANOSECONDS);
                        return;
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

        void closeClient(EventExecutor executor) {
            log.info("[C:0x{}] Disconnected from DC {}", id, authData.dc().getId());

            cancelRequests(executor);
            onClose.emitEmpty(FAIL_FAST);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt instanceof HandshakeCompleteEvent event) {
                authData.authKey(event.authKey());
                authData.serverSalt(event.serverSalt());
                authData.timeOffset(event.serverTimeDiff());

                mtProtoOptions.storeLayout().updateAuthKey(authData.dc(), event.authKey())
                        .subscribe(null, ctx::fireExceptionCaught);

                ctx.pipeline().remove(HANDSHAKE);
                ctx.pipeline().remove(HANDSHAKE_CODEC);

                configure(ctx);
            } else {
                ctx.fireUserEventTriggered(evt);
            }
        }

        void configure(ChannelHandlerContext ctx) {
            var transportCodec = ctx.pipeline().get(TransportCodec.class);
            ctx.pipeline().addAfter(TRANSPORT, ENCRYPTION, new MTProtoEncryption(MTProtoClientImpl.this, transportCodec));

            if (type == DcId.Type.MAIN) {
                mtProtoOptions.storeLayout().updateDataCenter(authData.dc())
                        .subscribe(null, ctx::fireExceptionCaught);
            }

            pingEmitter = Trigger.create(() -> {
                if (!inflightPing) {
                    inflightPing = true;
                    ctx.writeAndFlush(new RpcRequest(ImmutablePingDelayDisconnect.of(System.nanoTime(), PING_TIMEOUT)), ctx.voidPromise());
                    return;
                }

                log.debug("[C:0x{}] Closing by ping timeout", id);

                logStateChange(ChannelState.DISCONNECTED);
                channelState = ChannelState.DISCONNECTED_STATE;
                ctx.close();
            }, ctx.executor(), options.pingInterval());

            if (authData.oldSessionId() != 0) {
                send(ctx, ImmutableDestroySession.of(authData.oldSessionId()))
                        .subscribe(null, ctx::fireExceptionCaught);
            }

            send(ctx, options.initConnection())
                    .subscribe(freshConfig -> {
                        logStateChange(ChannelState.CONNECTED);
                        channelState = new ChannelState(ctx.channel(), ChannelState.CONNECTED);

                        var sinkAttr = ctx.channel().attr(NOTIFY);
                        var sink = sinkAttr.get();

                        // Send all pending requests on next tick
                        ctx.executor().execute(() -> {
                            if (pendingRequests.isEmpty()) {
                                return;
                            }

                            if (log.isDebugEnabled()) {
                                log.debug("[C:0x{}] Sending pending requests: {}", id, pendingRequests.size());
                            }

                            pendingRequests.drain(ctx.channel()::write);
                            ctx.channel().flush();
                        });

                        if (sink != null) {
                            sink.success();
                            sinkAttr.set(null);
                        }
                    }, ctx::fireExceptionCaught);
        }

        void initializeChannel(ChannelHandlerContext ctx, Transport tr) {
            if (type == DcId.Type.MAIN) {
                log.info("[C:0x{}] Connected to main DC {}", id, authData.dc().getId());
            } else {
                log.info("[C:0x{}] Connected to {} DC {}", id,
                        authData.dc().getType().name().toLowerCase(Locale.US),
                        authData.dc().getId());
            }

            ctx.pipeline().addFirst(TRANSPORT, new TransportCodec(tr));

            if (authData.authKey() == null) {
                mtProtoOptions.storeLayout().getAuthKey(authData.dc())
                        .switchIfEmpty(Mono.fromRunnable(() -> ctx.executor().execute(() -> {
                            ctx.pipeline().addAfter(TRANSPORT, HANDSHAKE_CODEC, new HandshakeCodec(authData));

                            int expiresIn = 0;//type == DcId.Type.MAIN ? 0 : Math.toIntExact(options.authKeyLifetime().getSeconds());
                            var handshakeCtx = new HandshakeContext(expiresIn,
                                    mtProtoOptions.dhPrimeChecker(), mtProtoOptions.publicRsaKeyRegister());
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

    void logStateChange(int newState) {
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
            connect0(sink);
        });
    }

    private void connect0(MonoSink<Void> sink) {
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
                } else if (t instanceof ConnectException) {
                    if (t instanceof ConnectTimeoutException) {
                        log.error("[C:0x" + id + "] Connection timed out");
                    } else {
                        log.error("[C:0x" + id + "] Connect exception", t);
                    }

                    future.channel().eventLoop().schedule(() -> connect0(sink),
                            options.reconnectionInterval().toNanos(), TimeUnit.NANOSECONDS);
                    return;
                } else {
                    log.error("[C:0x" + id + "] Unexpected client exception", t);
                }

                sink.error(t);
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> Mono<R> send(TlMethod<? extends R> method) {
        return Mono.defer(() -> {
            if (!isResultAwait(method)) {
                return Mono.error(new MTProtoException("Illegal method was sent: " + method));
            }

            var currentState = channelState;

            if (currentState == ChannelState.CLOSED_STATE) {
                return Mono.error(new MTProtoException("Client has been closed"));
            } else if (currentState == ChannelState.DISCONNECTED_STATE) {
                RequestMono sink = new RequestMono(false);
                if (!pendingRequests.offer(new RpcQuery(method, sink))) {
                    return Mono.error(new DiscardedRpcRequestException(method));
                }

                if (log.isDebugEnabled()) {
                    log.debug("[C:0x{}] Delaying request: {}", id, schemaTypeName(method));
                }
                return (Mono<R>) sink;
            } else { // CONNECTED
                RequestMono sink = new RequestMono(false);
                currentState.channel.writeAndFlush(new RpcQuery(method, sink), currentState.channel.voidPromise());
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
    <R> Mono<R> send(ChannelHandlerContext ctx, TlMethod<R> method) {
        if (!isResultAwait(method)) {
            ctx.channel().writeAndFlush(new RpcRequest(method), ctx.channel().voidPromise());
            return Mono.empty();
        }

        RequestMono sink = new RequestMono(true);
        ctx.channel().writeAndFlush(new RpcQuery(method, sink), ctx.channel().voidPromise());
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
                cancelRequests(mtProtoOptions.resultPublisher());
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

    void cancelRequests(ExecutorService eventLoop) {
        RuntimeException exc = Exceptions.failWithCancel();
        for (var req : requests.values()) {
            if (req instanceof RpcQuery q) {
                var resultPublisher = q.sink.isPublishOnEventLoop()
                        ? eventLoop
                        : mtProtoOptions.resultPublisher();

                q.sink.emitError(resultPublisher, exc);
            }
        }

        pendingRequests.drain(rpcQuery -> {
            var resultPublisher = rpcQuery.sink.isPublishOnEventLoop()
                    ? eventLoop
                    : mtProtoOptions.resultPublisher();

            rpcQuery.sink.emitError(resultPublisher, exc);
        });

        RpcQuery q;
        while ((q = delayedUntilAuth.poll()) != null) {
            var resultPublisher = q.sink.isPublishOnEventLoop()
                    ? eventLoop
                    : mtProtoOptions.resultPublisher();

            q.sink.emitError(resultPublisher, exc);
        }
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

    static boolean isPingPacket(TlMethod<?> method) {
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

        static String stateName(int state) {
            return switch (state) {
                case DISCONNECTED -> "DISCONNECTED";
                case CONNECTED -> "CONNECTED";
                case CLOSED -> "CLOSED";
                default -> throw new IllegalStateException();
            };
        }
    }
}
