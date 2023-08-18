/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package telegram4j.mtproto.client.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.AttributeKey;
import io.netty.util.NetUtil;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.ScheduledFuture;
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
import telegram4j.mtproto.client.MTProtoClient;
import telegram4j.mtproto.client.MTProtoClientGroup;
import telegram4j.mtproto.client.MTProtoOptions;
import telegram4j.mtproto.internal.Preconditions;
import telegram4j.mtproto.resource.impl.BaseProxyResources;
import telegram4j.mtproto.transport.Transport;
import telegram4j.tl.api.TlMethod;
import telegram4j.tl.mtproto.MsgsAck;
import telegram4j.tl.request.account.GetPassword;
import telegram4j.tl.request.auth.CheckPassword;
import telegram4j.tl.request.auth.ExportLoginToken;
import telegram4j.tl.request.auth.ImportLoginToken;
import telegram4j.tl.request.mtproto.*;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;
import static telegram4j.mtproto.util.TlEntityUtil.schemaTypeName;

public final class MTProtoClientImpl implements MTProtoClient {
    static final String PROXY           = "proxy";

    static final String HANDSHAKE_CODEC = "mtproto.handshake.codec";
    static final String HANDSHAKE       = "mtproto.handshake";
    static final String TRANSPORT       = "mtproto.transport";
    static final String ENCRYPTION      = "mtproto.encryption";
    static final String CORE            = "mtproto.core";

    static final Logger log = Loggers.getLogger("telegram4j.mtproto.MTProtoClient");
    static final Logger rpcLog = Loggers.getLogger("telegram4j.mtproto.rpc");

    static final int PING_TIMEOUT = 60;

    static final AttributeKey<MonoSink<Void>> NOTIFY = AttributeKey.valueOf("$notify");

    static final VarHandle CHANNEL_STATE;
    static {
        var lookup = MethodHandles.lookup();
        try {
            CHANNEL_STATE = lookup.findVarHandle(MTProtoClientImpl.class, "channelState", ChannelState.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    volatile ChannelState channelState = ChannelState.disconnected(null);
    int oldState = ChannelState.DISCONNECTED;
    boolean inflightPing;
    ScheduledFuture<?> pingTrigger;

    final MTProtoClientGroup group;
    final DcId.Type type;
    final AuthData authData;
    final MpscArrayQueue<RpcQuery> pendingRequests = new MpscArrayQueue<>(Queues.SMALL_BUFFER_SIZE);
    final HashMap<Long, Request> requests = new HashMap<>();
    final ArrayDeque<RpcQuery> delayedUntilAuth = new ArrayDeque<>(16);
    final ArrayDeque<RpcRequest> resend = new ArrayDeque<>(32);
    final String id = Integer.toHexString(hashCode());
    final ReconnectionContextImpl reconnectCtx = new ReconnectionContextImpl();
    final ConcurrentStats stats = new ConcurrentStats();
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
                .channelFactory(tcpClientRes.eventLoopResources().getChannelFactory())
                .group(tcpClientRes.eventLoopGroup())
                .remoteAddress(InetSocketAddress.createUnresolved(dc.getAddress(), dc.getPort()))
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        var impl = (BaseProxyResources) tcpClientRes.proxyProvider().orElse(null);
                        if (impl != null) {
                            ch.pipeline().addFirst(PROXY, impl.createProxyHandler(impl.address));
                        }

                        ch.pipeline().addLast(CORE, new MTProtoClientHandler());
                    }
                });
    }

    class MTProtoClientHandler extends ChannelInboundHandlerAdapter {
        private MTProtoEncryption encryption;
        private ScheduledFuture<?> reconnectHandle;

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable t) {
            setException(t);

            ctx.close();
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
            if (pingTrigger != null) {
                pingTrigger.cancel(false);
            }

            long backoff;
            if (reconnectCtx.isResume() && (backoff = nextBackoff(null)) != -1) {
                logStateChange(ChannelState.DISCONNECTED);
                channelState = ChannelState.disconnected(ctx.channel());

                authData.resetSessionId();

                scheduleReconnection(ctx.executor(), backoff);
            } else {
                logStateChange(ChannelState.CLOSED);
                channelState = ChannelState.CLOSED_STATE;

                if (reconnectHandle != null) {
                    reconnectHandle.cancel(true);
                }

                log.info("[C:0x{}] Disconnected from DC {}", id, authData.dc().getId());

                cancelRequests(ctx);

                Throwable cause = reconnectCtx.cause();
                // Reset for correct close()
                reconnectCtx.reset();

                close(ctx.channel(), cause);
            }
        }

        void scheduleReconnection(EventExecutor executor, long backoffMillis) {
            log.info("[C:0x{}] Reconnecting to DC {} after {}",
                    id, authData.dc().getId(), Duration.ofMillis(backoffMillis));
            reconnectHandle = executor.schedule(this::reconnect, backoffMillis, TimeUnit.MILLISECONDS);
        }

        void reconnect() {
            var future = bootstrap.connect();
            future.addListener(notify -> {
                Throwable t = notify.cause();
                if (t != null) {
                    boolean resume = setException(t);

                    long backoff;
                    if (resume && (backoff = nextBackoff(t)) != -1) {
                        scheduleReconnection(future.channel().eventLoop(), backoff);
                    } else {
                        close(future.channel(), t);
                    }
                }
            });
        }

        void close(Channel channel, @Nullable Throwable cause) {
            var sinkAttr = channel.attr(NOTIFY);
            var sink = sinkAttr.getAndSet(null);
            if (sink != null) {
                if (cause != null) {
                    sink.error(cause);
                } else {
                    sink.success();
                }
            }

            if (cause != null) {
                onClose.emitError(cause, FAIL_FAST);
            } else {
                onClose.emitEmpty(FAIL_FAST);
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt instanceof HandshakeCompleteEvent event) {
                authData.authKey(event.authKey());
                authData.serverSalt(event.serverSalt());
                authData.timeOffset(event.serverTimeDiff());

                // TODO: switch thread to publisher
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
            encryption = new MTProtoEncryption(MTProtoClientImpl.this, transportCodec);

            ctx.pipeline().addAfter(TRANSPORT, ENCRYPTION, encryption);

            if (type == DcId.Type.MAIN) {
                // TODO: switch thread to publisher
                mtProtoOptions.storeLayout().updateDataCenter(authData.dc())
                        .subscribe(null, ctx::fireExceptionCaught);
            }

            schedulePing(ctx);

            if (authData.oldSessionId() != 0) {
                send(ctx, ImmutableDestroySession.of(authData.oldSessionId()))
                        .subscribe(null, ctx::fireExceptionCaught);
            }

            send(ctx, options.initConnection())
                    .subscribe(freshConfig -> {
                        reconnectCtx.resetAfterConnect();

                        logStateChange(ChannelState.CONNECTED);
                        channelState = new ChannelState(ctx.channel(), ChannelState.CONNECTED);

                        var sinkAttr = ctx.channel().attr(NOTIFY);
                        var sink = sinkAttr.getAndSet(null);
                        if (sink != null) {
                            sink.success();
                        }

                        sendPendingRequests(ctx);
                    }, ctx::fireExceptionCaught);
        }

        void schedulePing(ChannelHandlerContext ctx) {
            long period = options.pingInterval().toNanos();
            pingTrigger = ctx.executor().scheduleWithFixedDelay(() -> sendPing(ctx),
                    period, period, TimeUnit.NANOSECONDS);

        }

        void sendPing(ChannelHandlerContext ctx) {
            if (inflightPing) {
                log.debug("[C:0x{}] Closing by ping timeout", id);

                ctx.close();
            } else {
                inflightPing = true;
                ctx.writeAndFlush(new RpcRequest(ImmutablePingDelayDisconnect.of(System.nanoTime(), PING_TIMEOUT)), ctx.voidPromise());
            }
        }

        void sendPendingRequests(ChannelHandlerContext ctx) {
            if (pendingRequests.isEmpty()) {
                return;
            }

            if (log.isDebugEnabled()) {
                log.debug("[C:0x{}] Sending pending requests: {}", id, pendingRequests.size());
            }

            pendingRequests.drain(resend::add);
            try {
                encryption.resend();
            } catch (Exception ex) {
                ctx.fireExceptionCaught(ex);
            }
        }

        void initializeChannel(ChannelHandlerContext ctx, Transport tr) {
            log.info("[C:0x{}] Connected to {} DC {}", id,
                    type.name().toLowerCase(Locale.US),
                    authData.dc().getId());

            ctx.pipeline().addFirst(TRANSPORT, new TransportCodec(tr));

            if (authData.authKey() == null) {
                // TODO: switch thread to publisher
                mtProtoOptions.storeLayout().getAuthKey(authData.dc())
                        .switchIfEmpty(Mono.fromRunnable(() -> ctx.executor().execute(() -> {
                            ctx.pipeline().addAfter(TRANSPORT, HANDSHAKE_CODEC, new HandshakeCodec(authData));

                            int expiresIn = 0;//type == DcId.Type.MAIN ? 0 : Math.toIntExact(options.authKeyLifetime().getSeconds());
                            var handshakeCtx = new HandshakeContext(expiresIn,
                                    mtProtoOptions.dhPrimeChecker(), mtProtoOptions.publicRsaKeyRegister());
                            ctx.pipeline().addAfter(HANDSHAKE_CODEC, HANDSHAKE, new Handshake(id, authData, handshakeCtx));
                        })))
                        .subscribe(loadedAuthKey -> ctx.executor().execute(() -> {
                            authData.authKey(loadedAuthKey);
                            configure(ctx);
                        }), ctx::fireExceptionCaught);
            } else {
                configure(ctx);
            }
        }
    }

    boolean setException(Throwable t) {
        boolean resume = false;

        if (t instanceof IOException) {
            log.error("[C:0x" + id + "] Socket exception: " + t);

            resume = true;
        } else if (t instanceof TransportException te) {
            log.error("[C:0x" + id + "] Transport exception, code: " + te.getCode());
        } else if (t instanceof AuthorizationException) {
            log.error("[C:0x" + id + "] Exception during auth key generation", t);
        } else if (t instanceof MTProtoException) {
            log.error("[C:0x" + id + "] Validation exception", t);

            resume = true;
        } else if (t instanceof ReadTimeoutException) {
            log.error("[C:0x" + id + "] Handshake timeout");

            resume = true;
        } else {
            log.error("[C:0x" + id + "] Unexpected client exception", t);
        }

        reconnectCtx.setException(t);
        reconnectCtx.setResume(resume);

        return resume;
    }

    void logStateChange(int newState) {
        int old = oldState;
        if (old == newState) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("[C:0x{}] Updating state: {}->{}", id, ChannelState.stateName(old), ChannelState.stateName(newState));
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
            } else if (currentState.state == ChannelState.CONNECTED) {
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
                boolean reconnect = setException(t);

                long backoff;
                if (reconnect && (backoff = nextBackoff(t)) != -1) {
                    var eventLoop = future.channel().eventLoop();
                    eventLoop.schedule(() -> connect0(sink), backoff, TimeUnit.MILLISECONDS);
                } else {
                    sink.error(t);
                }
            }
        });
    }

    // -1 indicates null
    // Method reduces accuracy to millis
    private long nextBackoff(@Nullable Throwable exception) {
        reconnectCtx.increment();
        reconnectCtx.setException(exception);

        var backoff = options.reconnectionStrategy().computeBackoff(reconnectCtx);
        if (backoff != null) {
            Preconditions.requireArgument(!backoff.isNegative(), () ->
                    options.reconnectionStrategy() + " returned negative backoff");

            reconnectCtx.setLastBackoff(backoff);
        }

        return backoff == null ? -1 : backoff.truncatedTo(ChronoUnit.MILLIS).toMillis();
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
            } else if (currentState.state == ChannelState.DISCONNECTED) {
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

                assert currentState.channel != null;

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
            var current = (ChannelState) CHANNEL_STATE.getAndSet(this, ChannelState.CLOSED_STATE);
            if (current == ChannelState.CLOSED_STATE) {
                sink.success();
                return;
            }

            if (current.channel != null) {
                var notifyAttr = current.channel.attr(NOTIFY);
                notifyAttr.set(sink);
                reconnectCtx.setResume(false);

                current.channel.close();
            } else { // client was not connected
                sink.success();
                onClose.emitEmpty(FAIL_FAST);
            }
        });
    }

    @Override
    public Mono<Void> onClose() {
        return onClose.asMono();
    }

    // must be called on event loop
    void cancelRequests(ChannelHandlerContext ctx) {
        RuntimeException exc = Exceptions.failWithCancel();
        for (var req : requests.values()) {
            if (req instanceof RpcQuery q) {
                var resultPublisher = q.sink.isPublishOnEventLoop()
                        ? ctx.executor()
                        : mtProtoOptions.resultPublisher();

                q.sink.emitError(resultPublisher, exc);
            }
        }

        pendingRequests.drain(rpcQuery -> {
            var resultPublisher = rpcQuery.sink.isPublishOnEventLoop()
                    ? ctx.executor()
                    : mtProtoOptions.resultPublisher();

            rpcQuery.sink.emitError(resultPublisher, exc);
        });

        RpcRequest r;
        while ((r = resend.pollFirst()) != null) {
            if (r instanceof RpcQuery q) {
                var resultPublisher = q.sink.isPublishOnEventLoop()
                        ? ctx.executor()
                        : mtProtoOptions.resultPublisher();

                q.sink.emitError(resultPublisher, exc);
            }
        }

        RpcQuery q;
        while ((q = delayedUntilAuth.pollFirst()) != null) {
            var resultPublisher = q.sink.isPublishOnEventLoop()
                    ? ctx.executor()
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

        // allows to avoid msgsStateReq flood
        long creationTimestamp;

        RpcRequest(TlMethod<?> method) {
            this.method = method;
        }

        ContainerizedRequest wrap(long containerMsgId) {
            return new RpcContainerRequest(this, containerMsgId);
        }

        public void setCreationTimestamp(long timestamp) {
            creationTimestamp = timestamp;
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
        ContainerizedRequest wrap(long containerMsgId) {
            return new QueryContainerRequest(this, containerMsgId);
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

        void setCreationTimestamp(long timestamp);
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

        public void emitValue(Executor resultPublisher, Object obj) {
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

        public void emitError(Executor resultPublisher, RuntimeException e) {
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

    record ChannelState(@Nullable Channel channel, int state) {
        public static final int DISCONNECTED = 0;
        public static final int CONNECTED = 1;
        public static final int CLOSED = 2;

        static final ChannelState CLOSED_STATE = new ChannelState(null, CLOSED);

        static ChannelState disconnected(@Nullable Channel channel) {
            return new ChannelState(channel, DISCONNECTED);
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
