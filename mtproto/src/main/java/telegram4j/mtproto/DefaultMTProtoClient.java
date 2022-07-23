package telegram4j.mtproto;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.ReferenceCountUtil;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.netty.Connection;
import reactor.netty.ConnectionObserver;
import reactor.netty.FutureMono;
import reactor.netty.channel.AbortedException;
import reactor.netty.tcp.TcpClient;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.annotation.Nullable;
import reactor.util.concurrent.Queues;
import reactor.util.retry.Retry;
import telegram4j.mtproto.auth.AuthorizationContext;
import telegram4j.mtproto.auth.AuthorizationException;
import telegram4j.mtproto.auth.AuthorizationHandler;
import telegram4j.mtproto.auth.AuthorizationKeyHolder;
import telegram4j.mtproto.transport.Transport;
import telegram4j.mtproto.util.AES256IGECipher;
import telegram4j.mtproto.util.ResettableInterval;
import telegram4j.tl.TlDeserializer;
import telegram4j.tl.TlSerialUtil;
import telegram4j.tl.TlSerializer;
import telegram4j.tl.Updates;
import telegram4j.tl.api.MTProtoObject;
import telegram4j.tl.api.RpcMethod;
import telegram4j.tl.api.TlMethod;
import telegram4j.tl.api.TlObject;
import telegram4j.tl.mtproto.*;
import telegram4j.tl.request.InvokeWithLayer;
import telegram4j.tl.request.mtproto.ImmutablePingDelayDisconnect;
import telegram4j.tl.request.mtproto.Ping;
import telegram4j.tl.request.mtproto.PingDelayDisconnect;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;

import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;
import static telegram4j.mtproto.DefaultMTProtoClient.PendingRequest.ACKNOWLEDGED;
import static telegram4j.mtproto.DefaultMTProtoClient.PendingRequest.PENDING;
import static telegram4j.mtproto.RpcException.prettyMethodName;
import static telegram4j.mtproto.transport.Transport.QUICK_ACK_MASK;
import static telegram4j.mtproto.util.CryptoUtil.*;

public class DefaultMTProtoClient implements MTProtoClient {
    private static final Logger log = Loggers.getLogger(DefaultMTProtoClient.class);
    private static final Logger rpcLog = Loggers.getLogger("telegram4j.mtproto.rpc");

    private static final int MAX_MISSED_PONG = 1;
    private static final int ACK_SEND_THRESHOLD = 3;
    private static final Throwable RETRY = new RetryConnectException();
    private static final Duration PING_QUERY_PERIOD = Duration.ofSeconds(5);
    private static final Duration ACK_QUERY_PERIOD = Duration.ofSeconds(15);
    private static final int PING_TIMEOUT = (int) PING_QUERY_PERIOD.multipliedBy(2).getSeconds();

    private final DataCenter dataCenter;
    private final TcpClient tcpClient;
    private final Transport transport;
    private final Type type;

    private final AuthorizationContext authContext = new AuthorizationContext();
    private final Sinks.Many<Updates> updates;
    private final Sinks.Many<PendingRequest> outbound;
    private final Sinks.Many<TlMethod<?>> authOutbound;
    private final ResettableInterval pingEmitter;
    private final ResettableInterval ackEmitter;
    private final Sinks.Many<State> state;

    private final String id = Integer.toHexString(hashCode());
    private final MTProtoOptions options;

    private volatile long lastPing;
    private final AtomicLong lastPong = new AtomicLong();
    private final AtomicInteger missedPong = new AtomicInteger();

    private final long sessionId = random.nextLong();
    private volatile Sinks.Empty<Void> closeHook;
    private volatile AuthorizationKeyHolder authKey;
    private volatile Connection connection;
    private volatile boolean closed;
    private volatile int timeOffset;
    private volatile long serverSalt;
    private volatile long lastMessageId;
    private final AtomicInteger seqNo = new AtomicInteger();
    private final Queue<Long> acknowledgments = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Long, PendingRequest> requests = new ConcurrentHashMap<>();
    private final Cache<Integer, Long> quickAckTokens = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .build();

    DefaultMTProtoClient(Type type, DataCenter dataCenter, MTProtoOptions options) {
        this.type = type;
        this.dataCenter = dataCenter;
        this.tcpClient = initTcpClient(options.getTcpClient());
        this.transport = options.getTransport().get();
        this.options = options;

        this.updates = Sinks.many().multicast()
                .onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);
        this.outbound = Sinks.many().multicast()
                .onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);
        this.authOutbound = Sinks.many().multicast()
                .onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);
        this.state = Sinks.many().replay()
                .latestOrDefault(State.RECONNECT);
        this.ackEmitter = new ResettableInterval(Schedulers.parallel());
        this.pingEmitter = new ResettableInterval(Schedulers.parallel());
    }

    public DefaultMTProtoClient(MTProtoOptions options) {
        this(Type.DEFAULT, options.getDatacenter(), options);
    }

    @Override
    public Mono<Void> connect() {
        return tcpClient.connect()
        .flatMap(connection -> {
            this.closeHook = Sinks.empty();
            this.connection = connection;

            Sinks.One<AuthorizationKeyHolder> onAuthSink = Sinks.one();
            ByteBufAllocator alloc = connection.channel().alloc();

            AuthorizationHandler authHandler = new AuthorizationHandler(this, authContext, onAuthSink, alloc);

            connection.addHandlerFirst(new DecodeChannelHandler());

            Mono<Void> stateHandler = state.asFlux()
                    .flatMap(state -> {
                        log.debug("[C:0x{}] Updating state: {}", id, state);

                        switch (state) {
                            case AUTHORIZATION_END:
                            case AUTHORIZATION_BEGIN:
                                // auth requests doesn't require acknowledging
                                boolean enable = state == State.AUTHORIZATION_END;
                                transport.setQuickAckState(enable);
                                return Mono.empty();
                            case CLOSED:
                                closed = true;
                                ackEmitter.dispose();
                                pingEmitter.dispose();
                                closeHook.emitEmpty(FAIL_FAST);

                                log.debug("[C:0x{}] Disconnected from the datacenter {}", id, dataCenter);
                                return Mono.fromRunnable(connection::dispose)
                                        .onErrorResume(t -> Mono.empty());
                            case DISCONNECTED:
                                ackEmitter.dispose();
                                pingEmitter.dispose();

                                return Mono.fromRunnable(connection::dispose)
                                        .onErrorResume(t -> Mono.empty())
                                        .then(Mono.error(RETRY));
                            case CONFIGURED: // if not reset there is a chance that the ping interval will not work after reconnect
                                lastPong.set(0);
                                lastPing = 0;
                                missedPong.set(0);
                            default:
                                return Mono.empty();
                        }
                    })
                    .then();

            Mono<Void> onConnect = state.asFlux().filter(state -> state == State.CONNECTED).next().then();

            Mono<Void> inboundHandler = connection.inbound().receive()
                    .map(ByteBuf::retainedDuplicate)
                    .flatMap(payload -> {
                        if (payload.readableBytes() == 4) {
                            int val = payload.readIntLE();
                            payload.release();

                            if (!TransportException.isError(val) && transport.supportQuickAck()) { // quick acknowledge
                                Long msgId = quickAckTokens.getIfPresent(val);
                                if (msgId == null) {
                                    log.debug("[C:0x{}] Unserialized quick acknowledge", this.id);
                                    return Mono.empty();
                                }

                                var req = requests.get(msgId);
                                if (req != null) { // just in case
                                    req.markState(s -> s | ACKNOWLEDGED);
                                }

                                if (rpcLog.isDebugEnabled()) {
                                    rpcLog.debug("[C:0x{}, M:0x{}] Handling quick ack",
                                            this.id, Long.toHexString(msgId));
                                }

                                quickAckTokens.invalidate(val);
                                return Mono.empty();
                            }

                            // The error code writes as negative int32
                            TransportException exc = TransportException.create(val);
                            if (val == -404 && authKey == null) { // retry authorization
                                onAuthSink.emitError(new AuthorizationException(exc), FAIL_FAST);
                                return Mono.empty();
                            }

                            return Mono.error(exc);
                        }
                        return Mono.just(payload);
                    })
                    .publishOn(Schedulers.boundedElastic())
                    .flatMap(buf -> {
                        long authKeyId = buf.readLongLE();

                        if (authKeyId == 0) { // unencrypted message
                            buf.skipBytes(12); // message id (8) + payload length (4)

                            try {
                                MTProtoObject obj = TlDeserializer.deserialize(buf);
                                return authHandler.handle(obj);
                            } catch (Throwable t) {
                                return Mono.error(Exceptions.propagate(t));
                            } finally {
                                buf.release();
                            }
                        }

                        AuthorizationKeyHolder authKeyHolder = this.authKey;
                        long authKeyIdAsLong = authKeyHolder.getAuthKeyId().getLongLE(0);
                        if (authKeyId != authKeyIdAsLong) {
                            return Mono.error(new MTProtoException("Incorrect auth key id. Received: 0x"
                                    + Long.toHexString(authKeyId) + ", but excepted: 0x"
                                    + Long.toHexString(authKeyIdAsLong)));
                        }

                        // message key recheck

                        ByteBuf messageKey = buf.readRetainedSlice(16);

                        ByteBuf authKey = authKeyHolder.getAuthKey();
                        AES256IGECipher cipher = createAesCipher(messageKey, authKey, true);

                        ByteBuf decrypted = cipher.decrypt(buf.slice());

                        ByteBuf messageKeyHash = sha256Digest(authKey.slice(96, 32), decrypted);
                        ByteBuf messageKeyHashSlice = messageKeyHash.slice(8, 16);

                        if (!messageKey.equals(messageKeyHashSlice)) {
                            return Mono.error(() -> new MTProtoException("Incorrect message key. Received: 0x"
                                    + ByteBufUtil.hexDump(messageKey) + ", but recomputed: 0x"
                                    + ByteBufUtil.hexDump(messageKeyHashSlice)));
                        }

                        messageKey.release();

                        long serverSalt = decrypted.readLongLE();
                        long sessionId = decrypted.readLongLE();
                        if (this.sessionId != sessionId) {
                            return Mono.error(() -> new IllegalStateException("Incorrect session identifier. Current: 0x"
                                    + Long.toHexString(this.sessionId) + ", received: 0x"
                                    + Long.toHexString(sessionId)));
                        }
                        long messageId = decrypted.readLongLE();
                        int seqNo = decrypted.readIntLE();
                        int length = decrypted.readIntLE();
                        if (length % 4 != 0) {
                            return Mono.error(new IllegalStateException("Data isn't aligned by 4 bytes"));
                        }

                        updateTimeOffset(messageId >> 32);

                        ByteBuf payload = decrypted.readSlice(length);
                        try {
                            TlObject obj = TlDeserializer.deserialize(payload);
                            return handleServiceMessage(obj, messageId);
                        } catch (Throwable t) {
                            return Mono.error(Exceptions.propagate(t));
                        } finally {
                            payload.release();
                        }
                    })
                    .then();

            Flux<PendingRequest> payloadFlux = outbound.asFlux()
                    .filter(e -> isContentRelated(e.method))
                    .delayUntil(e -> onConnect);

            // Perhaps it's better to wrap it in a message container via .buffer(Duration),
            // but there are difficulties with packaging
            Flux<PendingRequest> rpcFlux = outbound.asFlux()
                    .filter(e -> !isContentRelated(e.method));

            Flux<ByteBuf> outboundFlux = Flux.merge(rpcFlux, payloadFlux)
                    .map(req -> {
                        var pending = requests.entrySet().stream()
                                .filter(e -> (e.getValue().state & PENDING) != 1)
                                .map(Map.Entry::getKey)
                                .collect(Collectors.toList());

                        // server returns -404 transport error when this packet placed in container
                        boolean canContainerize = req.method.identifier() != InvokeWithLayer.ID;
                        int cnt = 1;
                        ByteBuf stateRequest = null;
                        if (canContainerize && !pending.isEmpty()) {
                            cnt++;

                            var stpending = new PendingRequest(MsgsStateReq.builder()
                                    .addAllMsgIds(pending)
                                    .build());

                            long messageId = getMessageId();
                            int seqNo = updateSeqNo(false);

                            requests.put(messageId, stpending);

                            ByteBuf data = TlSerializer.serialize(alloc, stpending.method);

                            stateRequest = alloc.buffer(data.readableBytes() + 16)
                                    .writeLongLE(messageId)
                                    .writeIntLE(seqNo)
                                    .writeIntLE(data.readableBytes())
                                    .writeBytes(data);
                            data.release();
                        }

                        ByteBuf acks = null;
                        if (canContainerize && !acknowledgments.isEmpty()) {
                            cnt++;

                            ByteBuf data = TlSerializer.serialize(alloc, MsgsAck.builder()
                                    .msgIds(acknowledgments)
                                    .build());

                            acknowledgments.clear();

                            long messageId = getMessageId();
                            int seqNo = updateSeqNo(false);

                            acks = alloc.buffer(data.readableBytes() + 16)
                                    .writeLongLE(messageId)
                                    .writeIntLE(seqNo)
                                    .writeIntLE(data.readableBytes())
                                    .writeBytes(data);
                            data.release();
                        }

                        canContainerize &= cnt > 1;

                        long messageId = getMessageId();
                        int seqNo = updateSeqNo(req.method);

                        // No response is sent to this request, so it may clog up the map
                        if (req.method.identifier() != MsgsAck.ID) {
                            requests.put(messageId, req);
                        }

                        ByteBuf requestData = TlSerializer.serialize(alloc, req.method);
                        ByteBuf data = alloc.buffer(16 + requestData.readableBytes())
                                .writeLongLE(messageId)
                                .writeIntLE(seqNo)
                                .writeIntLE(requestData.readableBytes())
                                .writeBytes(requestData);
                        requestData.release();

                        ByteBuf message;
                        if (canContainerize) { // TODO: check container size
                            int size = data.readableBytes() +
                                    (stateRequest != null ? stateRequest.readableBytes() : 0) +
                                    (acks != null ? acks.readableBytes() : 0);

                            long contMsgId = getMessageId();
                            int contSeqNo = updateSeqNo(false);

                            message = alloc.buffer(24 + size);
                            message.writeLongLE(contMsgId);
                            message.writeIntLE(contSeqNo);
                            message.writeIntLE(size + 8);
                            message.writeIntLE(MessageContainer.ID);
                            message.writeIntLE(cnt);

                            if (stateRequest != null) {
                                message.writeBytes(stateRequest);
                                stateRequest.release();
                            }

                            if (acks != null) {
                                message.writeBytes(acks);
                                acks.release();
                            }

                            message.writeBytes(data);
                            data.release();
                        } else {
                            message = data;
                        }

                        int minPadding = 12;
                        int unpadded = (32 + data.readableBytes() + minPadding) % 16;
                        byte[] paddingb = new byte[minPadding + (unpadded != 0 ? 16 - unpadded : 0)];
                        random.nextBytes(paddingb);

                        ByteBuf plainData = alloc.buffer(32 + message.readableBytes() + paddingb.length)
                                .writeLongLE(serverSalt)
                                .writeLongLE(sessionId)
                                .writeBytes(message)
                                .writeBytes(paddingb);
                        message.release();

                        ByteBuf authKey = this.authKey.getAuthKey();
                        ByteBuf authKeyId = this.authKey.getAuthKeyId();

                        ByteBuf messageKeyHash = sha256Digest(authKey.slice(88, 32), plainData);

                        boolean quickAck = false;
                        if (transport.supportQuickAck() && !canContainerize && isContentRelated(req.method)) {
                            int quickAckToken = messageKeyHash.getIntLE(0) | QUICK_ACK_MASK;
                            quickAckTokens.put(quickAckToken, messageId);
                            quickAck = true;
                        }

                        ByteBuf messageKey = messageKeyHash.slice(8, 16);
                        AES256IGECipher cipher = createAesCipher(messageKey, authKey, false);

                        ByteBuf encrypted = cipher.encrypt(plainData);
                        ByteBuf packet = Unpooled.wrappedBuffer(authKeyId.retain(), messageKey, encrypted);

                        if (rpcLog.isDebugEnabled()) {
                            rpcLog.debug("[C:0x{}, M:0x{}] Sending {}: {}", id,
                                    Long.toHexString(messageId), !canContainerize ? "request" : "in container",
                                    prettyMethodName(req.method));

                        }

                        req.markState(s -> s & ~PENDING);

                        return transport.encode(packet, quickAck);
                    });

            Flux<ByteBuf> authFlux = authOutbound.asFlux()
                    .map(method -> {
                        ByteBuf data = TlSerializer.serialize(alloc, method);
                        ByteBuf header = alloc.buffer(20 + data.readableBytes())
                                .writeLongLE(0) // auth key id
                                .writeLongLE(getMessageId()) // Message id in the auth requests doesn't allow receiving payload
                                .writeIntLE(data.readableBytes());
                        ByteBuf payload = Unpooled.wrappedBuffer(header, data);

                        if (rpcLog.isDebugEnabled()) {
                            rpcLog.debug("[C:0x{}] Sending mtproto request: {}", id, prettyMethodName(method));
                        }

                        return transport.encode(payload, false);
                    });

            Mono<Void> outboundHandler = Flux.merge(authFlux, outboundFlux)
                    .flatMap(b -> FutureMono.from(connection.channel().writeAndFlush(b)))
                    .doOnDiscard(ByteBuf.class, ReferenceCountUtil::safeRelease)
                    .then();

            Mono<Void> ping = pingEmitter.ticks()
                    .flatMap(tick -> {
                        long now = System.nanoTime();
                        lastPong.compareAndSet(0, now);

                        if (lastPing - lastPong.get() > 0) {
                            int missed = missedPong.incrementAndGet();
                            if (missed >= MAX_MISSED_PONG) {
                                lastMessageId = 0; // to break connection
                                if (missed > MAX_MISSED_PONG) {
                                    return Mono.empty();
                                }
                            }
                        }

                        lastPing = now;
                        return sendAwait(ImmutablePingDelayDisconnect.of(random.nextLong(), PING_TIMEOUT));
                    })
                    .then();

            Mono<Void> ack = ackEmitter.ticks()
                    .flatMap(tick -> sendAcks())
                    .then();

            Mono<AuthorizationKeyHolder> startAuth = Mono.fromRunnable(() ->
                    state.emitNext(State.AUTHORIZATION_BEGIN, options.getEmissionHandler()))
                    .then(authHandler.start())
                    .checkpoint("Authorization key generation")
                    .then(onAuthSink.asMono().retryWhen(authRetry(authHandler)))
                    .doOnNext(auth -> {
                        serverSalt = authContext.getServerSalt(); // apply temporal salt
                        authContext.clear();
                        state.emitNext(State.AUTHORIZATION_END, options.getEmissionHandler());
                    })
                    .flatMap(key -> options.getStoreLayout()
                            .updateAuthorizationKey(key).thenReturn(key));

            Mono<Void> awaitKey = Mono.justOrEmpty(authKey)
                    .switchIfEmpty(options.getStoreLayout().getAuthorizationKey(dataCenter))
                    .doOnNext(key -> onAuthSink.emitValue(key, FAIL_FAST))
                    .switchIfEmpty(startAuth)
                    .doOnNext(key -> this.authKey = key)
                    .then();

            Mono<Void> startSchedule = Mono.defer(() -> {
                log.info("[C:0x{}] Connected to datacenter.", id);
                state.emitNext(State.CONNECTED, options.getEmissionHandler());
                pingEmitter.start(PING_QUERY_PERIOD);
                ackEmitter.start(ACK_QUERY_PERIOD);

                return Mono.when(ping, ack);
            });

            Mono<Void> initialize = state.asFlux()
                    .filter(s -> s == State.CONFIGURED)
                    .next()
                    .flatMap(s -> awaitKey.then(startSchedule))
                    .then();

            return Mono.zip(inboundHandler, outboundHandler, stateHandler, initialize)
                    .doOnError(t -> !isRetryException(t), t -> {
                        if (t instanceof TransportException) {
                            TransportException t0 = (TransportException) t;
                            log.error("[C:0x" + id + "] Transport exception, code: " + t0.getCode(), t0);
                        } else if (!(t instanceof AuthorizationException)) {
                            log.error("[C:0x" + id + "] Unexpected client exception", t);
                        }
                    })
                    .onErrorResume(t -> Mono.empty())
                    .then();
        })
        // FluxReceive (inbound) emits empty signals if channel was DISCONNECTING
        .switchIfEmpty(reconnect())
        .retryWhen(options.getRetry()
                .filter(t -> !closed && isRetryException(t))
                .doAfterRetry(signal -> {
                    state.emitNext(State.RECONNECT, options.getEmissionHandler());
                    log.debug("[C:0x{}] Reconnecting to the datacenter (attempts: {})", id, signal.totalRetriesInARow());
                }))
        .then(Mono.defer(() -> closeHook.asMono()));
    }

    @Override
    public Sinks.Many<Updates> updates() {
        return updates;
    }

    @Override
    public void updateTimeOffset(long serverTime) {
        int updated = (int) (serverTime - System.currentTimeMillis() / 1000);
        boolean changed = Math.abs(timeOffset - updated) > 3;
        if (changed) {
            lastMessageId = 0;
            timeOffset = updated;
        }
    }

    @Override
    public int getTimeOffset() {
        return timeOffset;
    }

    @Override
    public long getSessionId() {
        return sessionId;
    }

    @Override
    public int getSeqNo() {
        return seqNo.get();
    }

    @Override
    public long getServerSalt() {
        return serverSalt;
    }

    @Override
    public <R, T extends TlMethod<R>> Mono<R> sendAwait(T method) {
        return Mono.defer(() -> {
            if (method instanceof RpcMethod) {
                outbound.emitNext(new PendingRequest(method), options.getEmissionHandler());
                return Mono.empty();
            }

            Sinks.One<R> res = Sinks.one();
            outbound.emitNext(new RpcRequest(method, res), options.getEmissionHandler());

            return res.asMono()
                    .transform(options.getResponseTransformers().stream()
                            .map(tr -> tr.transform(method)
                                    .andThen(m -> m.checkpoint("Apply " + tr + " to " + method + " [DefaultMTProtoClient]")))
                            .reduce(Function.identity(), Function::andThen))
                    .retryWhen(serverErrorRetry());
        });
    }

    @Override
    public Mono<Void> sendAuth(TlMethod<? extends MTProtoObject> method) {
        return Mono.fromRunnable(() -> authOutbound.emitNext(method, options.getEmissionHandler()));
    }

    @Override
    public Flux<State> state() {
        return state.asFlux();
    }

    @Override
    public DataCenter getDatacenter() {
        return dataCenter;
    }

    @Override
    public MTProtoClient createMediaClient(DataCenter dc) {
        if (type != Type.DEFAULT) {
            throw new IllegalStateException("Not default client can't create media clients");
        }

        DefaultMTProtoClient client = new DefaultMTProtoClient(Type.MEDIA, dc, options);

        client.authKey = authKey;
        client.lastMessageId = lastMessageId;
        client.timeOffset = timeOffset;
        client.serverSalt = serverSalt;

        return client;
    }

    @Override
    public MTProtoClient createChildClient() {
        DefaultMTProtoClient client = new DefaultMTProtoClient(type, dataCenter, options);

        client.authKey = authKey;
        client.lastMessageId = lastMessageId;
        client.timeOffset = timeOffset;
        client.serverSalt = serverSalt;

        return client;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Mono<Void> close() {
        return Mono.fromSupplier(() -> connection)
                .switchIfEmpty(Mono.error(new IllegalStateException("MTProto client isn't connected")))
                .doOnNext(con -> state.emitNext(State.CLOSED, options.getEmissionHandler()))
                .then();
    }

    private TcpClient initTcpClient(TcpClient tcpClient) {
        return tcpClient
                .remoteAddress(() -> new InetSocketAddress(dataCenter.getAddress(), dataCenter.getPort()))
                .observe((con, st) -> {
                    if (st == ConnectionObserver.State.CONFIGURED) {
                        log.debug("[C:0x{}] Connected to datacenter {}", id, dataCenter);
                        log.debug("[C:0x{}] Sending transport identifier to the server", id);

                        con.channel().writeAndFlush(transport.identifier(con.channel().alloc()))
                                .addListener(f -> state.emitNext(State.CONFIGURED, options.getEmissionHandler()));
                    } else if (!closed && (st == ConnectionObserver.State.DISCONNECTING ||
                            st == ConnectionObserver.State.RELEASED)) {
                        state.emitNext(State.DISCONNECTED, options.getEmissionHandler());
                    }
                });
    }

    private <T> Mono<T> reconnect() {
        return Mono.defer(() -> {
            if (closed) {
                return Mono.empty();
            }

            state.emitNext(State.DISCONNECTED, options.getEmissionHandler());
            return Mono.error(RETRY);
        });
    }

    private Retry authRetry(AuthorizationHandler authHandler) {
        return options.getAuthRetry()
                .filter(t -> t instanceof AuthorizationException)
                .doAfterRetryAsync(v -> authHandler.start())
                .onRetryExhaustedThrow((spec, signal) -> {
                    state.emitNext(State.CLOSED, options.getEmissionHandler());
                    return new MTProtoException("Failed to generate auth key (" +
                            signal.totalRetries() + "/" + spec.maxAttempts + ")");
                })
                .doAfterRetry(signal -> {
                    log.debug("[C:0x{}] Retrying regenerate auth key (attempts: {})",
                            id, signal.totalRetriesInARow());
                    authContext.clear();
                });
    }

    private Retry serverErrorRetry() {
        return Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(2))
                .maxBackoff(Duration.ofSeconds(15))
                .filter(RpcException.isErrorCode(500))
                .doAfterRetry(signal -> {
                    if (log.isDebugEnabled()) {
                        log.debug("[C:0x{}] Retrying request due to {} auth key for {} (attempts: {})",
                                id, signal.failure().toString(), signal.totalRetriesInARow());
                    }
                });
    }

    private long getMessageId() {
        long millis = System.currentTimeMillis();
        long seconds = millis / 1000;
        long mod = millis % 1000;
        long messageId = seconds + timeOffset << 32 | mod << 22 | random.nextInt(0xFFFF) << 2;

        long l = lastMessageId;
        if (l >= messageId) {
            messageId = l + 4;
        }

        lastMessageId = messageId;
        return messageId;
    }

    private int updateSeqNo(TlObject object) {
        return updateSeqNo(isContentRelated(object));
    }

    private int updateSeqNo(boolean content) {
        int no = seqNo.get() * 2 + (content ? 1 : 0);
        if (content) {
            seqNo.incrementAndGet();
        }

        return no;
    }

    private Mono<Void> handleServiceMessage(Object obj, long messageId) {
        // For updates
        if (obj instanceof GzipPacked) {
            GzipPacked gzipPacked = (GzipPacked) obj;
            obj = TlSerialUtil.decompressGzip(gzipPacked.packedData());
        }

        if (obj instanceof RpcResult) {
            RpcResult rpcResult = (RpcResult) obj;
            messageId = rpcResult.reqMsgId();
            obj = rpcResult.result();

            if (rpcLog.isDebugEnabled()) {
                rpcLog.debug("[C:0x{}, M:0x{}] Receiving rpc result", id, Long.toHexString(messageId));
            }

            if (obj instanceof GzipPacked) {
                GzipPacked gzipPacked = (GzipPacked) obj;
                obj = TlSerialUtil.decompressGzip(gzipPacked.packedData());
            }

            if (obj instanceof MsgsAck) {
                MsgsAck msgsAck = (MsgsAck) obj;

                if (rpcLog.isDebugEnabled()) {
                    rpcLog.debug("[C:0x{}, M:0x{}] Handling acknowledge for message(s): [{}]",
                            id, Long.toHexString(messageId), msgsAck.msgIds().stream()
                                    .map(l -> String.format("0x%x", l))
                                    .collect(Collectors.joining(", ")));
                }

                for (long msgId : msgsAck.msgIds()) {
                    var req = requests.get(msgId);
                    if (req != null) {
                        req.markState(s -> s | ACKNOWLEDGED);
                    }
                }
            }

            PendingRequest req = requests.get(messageId);

            if (obj instanceof RpcError) {
                RpcError rpcError = (RpcError) obj;

                if (rpcError.errorCode() == 420) { // FLOOD_WAIT_X
                    String arg = rpcError.errorMessage().substring(
                            rpcError.errorMessage().lastIndexOf('_') + 1);
                    Duration delay = Duration.ofSeconds(Integer.parseInt(arg));

                    if (rpcLog.isDebugEnabled()) {
                        rpcLog.debug("[C:0x{}, M:0x{}] Delaying resend for {}", id, Long.toHexString(messageId), delay);
                    }

                    // Need resend with delay.
                    return Mono.justOrEmpty(requests.remove(messageId))
                            .delayElement(delay)
                            .doOnNext(r -> {
                                r.state = PENDING;
                                outbound.emitNext(r, options.getEmissionHandler());
                            })
                            .then();
                }

                if (req != null) {
                    obj = RpcException.create(rpcError, messageId, req);
                }
            }

            resolve(messageId, obj);
            if (req != null && (req.state & ACKNOWLEDGED) != 0) { // already ack'ed
                return Mono.empty();
            }

            return acknowledgmentMessage(messageId);
        }

        if (obj instanceof MessageContainer) {
            MessageContainer messageContainer = (MessageContainer) obj;
            if (rpcLog.isTraceEnabled()) {
                rpcLog.trace("[C:0x{}] Handling message container: {}", id, messageContainer);
            } else if (rpcLog.isDebugEnabled()) {
                rpcLog.debug("[C:0x{}] Handling message container", id);
            }

            return Flux.fromIterable(messageContainer.messages())
                    .flatMap(message -> handleServiceMessage(
                            message.body(), message.msgId()))
                    .then();
        }

        if (obj instanceof Pong) {
            Pong pong = (Pong) obj;
            messageId = pong.msgId();
            if (rpcLog.isDebugEnabled()) {
                rpcLog.debug("[C:0x{}, M:0x{}] Receiving pong", id, Long.toHexString(messageId));
            }

            lastPong.set(System.nanoTime());
            missedPong.set(0);

            resolve(messageId, obj);
            return Mono.empty();
        }

        if (obj instanceof FutureSalts) {
            FutureSalts futureSalts = (FutureSalts) obj;
            messageId = futureSalts.reqMsgId();
            if (rpcLog.isDebugEnabled()) {
                rpcLog.debug("[C:0x{}, M:0x{}] Receiving future salts", id, Long.toHexString(messageId));
            }

            resolve(messageId, obj);
            return Mono.empty();
        }

        if (obj instanceof NewSession) {
            NewSession newSession = (NewSession) obj;
            if (rpcLog.isDebugEnabled()) {
                rpcLog.debug("[C:0x{}] Receiving new session creation, new server salt: 0x{}",
                        id, Long.toHexString(newSession.serverSalt()));
            }

            serverSalt = newSession.serverSalt();
            lastMessageId = newSession.firstMsgId();

            requests.forEach((k, v) -> {
                if (k < newSession.firstMsgId() && (v.state & PENDING) != 0) {
                    requests.remove(k);
                    outbound.emitNext(v, options.getEmissionHandler());
                }
            });

            return acknowledgmentMessage(messageId);
        }

        // ? w h y
        if (obj instanceof MsgsAck) {
            MsgsAck msgsAck = (MsgsAck) obj;

            if (rpcLog.isDebugEnabled()) {
                rpcLog.debug("[C:0x{}, M:0x{}] Handling acknowledge for message(s): [{}]",
                        id, Long.toHexString(messageId), msgsAck.msgIds().stream()
                                .map(l -> String.format("0x%x", l))
                                .collect(Collectors.joining(", ")));
            }

            for (long msgId : msgsAck.msgIds()) {
                var req = requests.get(msgId);
                if (req != null) {
                    req.markState(s -> s | ACKNOWLEDGED);
                }
            }
        }

        if (obj instanceof BadMsgNotification) {
            BadMsgNotification badMsgNotification = (BadMsgNotification) obj;
            if (rpcLog.isDebugEnabled()) {
                if (badMsgNotification.errorCode() == 48) {
                    BadServerSalt badServerSalt = (BadServerSalt) badMsgNotification;
                    rpcLog.debug("[C:0x{}, M:0x{}] Updating server salt, seqno: {}, new server salt: 0x{}", id,
                            Long.toHexString(badServerSalt.badMsgId()), badServerSalt.badMsgSeqno(),
                            Long.toHexString(badServerSalt.newServerSalt()));
                } else {
                    rpcLog.debug("[C:0x{}, M:0x{}] Receiving notification, code: {}, seqno: {}", id,
                            Long.toHexString(badMsgNotification.badMsgId()), badMsgNotification.errorCode(),
                            badMsgNotification.badMsgSeqno());
                }
            }

            switch (badMsgNotification.errorCode()) {
                case 16: // msg_id too low
                case 17: // msg_id too high
                    updateTimeOffset(messageId);
                    break;
                case 48:
                    BadServerSalt badServerSalt = (BadServerSalt) badMsgNotification;
                    serverSalt = badServerSalt.newServerSalt();
                    break;
            }

            return Mono.justOrEmpty(requests.remove(badMsgNotification.badMsgId()))
                    .doOnNext(r -> {
                        r.state = PENDING;
                        outbound.emitNext(r, options.getEmissionHandler());
                    })
                    .then();
        }

        if (obj instanceof Updates) {
            Updates updates = (Updates) obj;
            if (rpcLog.isTraceEnabled()) {
                rpcLog.trace("[C:0x{}] Receiving updates: {}", id, updates);
            }

            this.updates.emitNext(updates, options.getEmissionHandler());
        }

        if (obj instanceof MsgsStateInfo) {
            MsgsStateInfo inf = (MsgsStateInfo) obj;

            var req = requests.remove(inf.reqMsgId());
            if (req != null) {
                MsgsStateReq stater = (MsgsStateReq) req.method;
                ByteBuf c = inf.info();
                if (stater.msgIds().size() != c.readableBytes()) {
                    rpcLog.error("[C:0x{}, M:0x{}] Received not all states. expected: {}, received: {}",
                            id, Long.toHexString(inf.reqMsgId()), stater.msgIds().size(),
                            c.readableBytes());

                    return Mono.empty();
                }

                if (rpcLog.isDebugEnabled()) {
                    StringJoiner st = new StringJoiner(", ");
                    int i = 0;
                    for (long msgId : stater.msgIds()) {
                        st.add("0x" + Long.toHexString(msgId) + "/" + (c.getByte(i++) & 7));
                    }

                    rpcLog.debug("[C:0x{}, M:0x{}] Received states: [{}]", id, Long.toHexString(inf.reqMsgId()), st);
                }

                int i = 0;
                for (long msgId : stater.msgIds()) {
                    var sub = requests.get(msgId);
                    if (sub == null) {
                        i++;
                        continue;
                    }

                    int state = c.getByte(i++) & 7;
                    switch (state) {
                        case 1:
                        case 2:
                        case 3:
                            requests.remove(msgId);
                            sub.state = PENDING;
                            outbound.emitNext(sub, options.getEmissionHandler());
                            break;
                        case 0:
                        case 4:
                            sub.markState(s -> s | ACKNOWLEDGED);
                            break;
                    }
                }
            }
        }

        return Mono.empty();
    }

    private Mono<Void> acknowledgmentMessage(long messageId) {
        return Mono.defer(() -> {
            acknowledgments.add(messageId);

            if (acknowledgments.size() < ACK_SEND_THRESHOLD) {
                return Mono.empty();
            }

            return sendAcks();
        });
    }

    private Mono<Void> sendAcks() {
        if (acknowledgments.isEmpty()) {
            return Mono.empty();
        }

        if (rpcLog.isDebugEnabled()) {
            rpcLog.debug("[C:0x{}] Sending acknowledges for message(s): [{}]", id, acknowledgments.stream()
                    .map(l -> String.format("0x%x", l))
                    .collect(Collectors.joining(", ")));
        }

        var acks = MsgsAck.builder().msgIds(acknowledgments).build();
        acknowledgments.clear();

        return sendAwait(acks);
    }

    @SuppressWarnings("unchecked")
    private void resolve(long messageId, @Nullable Object value) {
        requests.computeIfPresent(messageId, (k, v) -> {
            if (v.getClass() == RpcRequest.class) {
                RpcRequest c = (RpcRequest) v;
                Sinks.One<Object> sink = (Sinks.One<Object>) c.sink;

                if (value == null) {
                    sink.emitEmpty(FAIL_FAST);
                } else if (value instanceof Throwable) {
                    Throwable value0 = (Throwable) value;
                    sink.emitError(value0, FAIL_FAST);
                } else {
                    sink.emitValue(value, FAIL_FAST);
                }
            }
            return null;
        });
    }

    static boolean isRetryException(Throwable t) {
        return t == RETRY || t instanceof AbortedException || t instanceof IOException;
    }

    static boolean isContentRelated(TlObject object) {
        switch (object.identifier()) {
            case MsgsAck.ID:
            case Ping.ID:
            case PingDelayDisconnect.ID:
            case MessageContainer.ID:
            case MsgsStateReq.ID:
                return false;
            default:
                return true;
        }
    }

    static class PendingRequest {
        static final VarHandle STATE;

        static {
            try {
                var l = MethodHandles.lookup();
                STATE = l.findVarHandle(PendingRequest.class, "state", int.class);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        static final int PENDING = 1;
        static final int ACKNOWLEDGED = 1 << 1;

        volatile int state = PENDING;

        final TlMethod<?> method;

        PendingRequest(TlMethod<?> method) {
            this.method = method;
        }

        void markState(IntUnaryOperator updateFunction) {
            int prev = state, next = 0;
            for (boolean haveNext = false;;) {
                if (!haveNext) {
                    next = updateFunction.applyAsInt(prev);
                }

                if (STATE.weakCompareAndSet(this, prev, next)) {
                    return;
                }
                haveNext = prev == (prev = state);
            }
        }
    }

    static class RpcRequest extends PendingRequest {
        final Sinks.One<?> sink;

        RpcRequest(TlMethod<?> method, Sinks.One<?> sink) {
            super(method);
            this.sink = sink;
        }
    }

    static class RetryConnectException extends RuntimeException {

        RetryConnectException() {}

        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }

    class DecodeChannelHandler extends ByteToMessageDecoder {
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf payload, List<Object> out) {
            ByteBuf buf = transport.decode(payload);
            if (buf != null) {
                out.add(buf);
            }
        }
    }
}
