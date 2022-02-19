package telegram4j.mtproto;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
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
import telegram4j.tl.TlDeserializer;
import telegram4j.tl.TlSerialUtil;
import telegram4j.tl.TlSerializer;
import telegram4j.tl.Updates;
import telegram4j.tl.api.MTProtoObject;
import telegram4j.tl.api.TlMethod;
import telegram4j.tl.api.TlObject;
import telegram4j.tl.mtproto.*;
import telegram4j.tl.request.mtproto.ImmutablePingDelayDisconnect;
import telegram4j.tl.request.mtproto.Ping;
import telegram4j.tl.request.mtproto.PingDelayDisconnect;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;
import static telegram4j.mtproto.RpcException.prettyMethodName;
import static telegram4j.mtproto.transport.IntermediateTransport.QUICK_ACK_MASK;
import static telegram4j.mtproto.util.CryptoUtil.*;

public class DefaultMTProtoClient implements MTProtoClient {
    private static final Logger log = Loggers.getLogger(DefaultMTProtoClient.class);
    private static final Logger rpcLog = Loggers.getLogger("telegram4j.mtproto.rpc");

    private static final int maxMissedPong = 3;
    private static final Throwable RETRY = new RetryConnectException();
    private static final Duration PING_QUERY_PERIOD = Duration.ofSeconds(10);
    private static final Duration ACK_QUERY_PERIOD = Duration.ofSeconds(30);
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

    private volatile long sessionId = random.nextLong();
    private volatile Sinks.Empty<Void> closeHook;
    private volatile AuthorizationKeyHolder authKey;
    private volatile Connection connection;
    private volatile boolean close;
    private volatile int timeOffset;
    private volatile long serverSalt;
    private volatile long lastMessageId;
    private final AtomicInteger seqNo = new AtomicInteger();
    private final Queue<Long> acknowledgments = new ConcurrentLinkedQueue<>();
    private final ConcurrentMap<Long, PendingRequest> requests = new ConcurrentHashMap<>();
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

    private TcpClient initTcpClient(TcpClient tcpClient) {
        return tcpClient
                .remoteAddress(() -> new InetSocketAddress(dataCenter.getAddress(), dataCenter.getPort()))
                .observe((con, st) -> {
                    if (st == ConnectionObserver.State.CONFIGURED) {
                        log.debug("[C:0x{}] Connected to datacenter {}", id, dataCenter);
                        log.debug("[C:0x{}] Sending transport identifier to the server", id);

                        con.channel().writeAndFlush(transport.identifier(con.channel().alloc()))
                                .addListener(f -> state.emitNext(State.CONFIGURED, options.getEmissionHandler()));
                    } else if (!close && (st == ConnectionObserver.State.DISCONNECTING ||
                            st == ConnectionObserver.State.RELEASED)) {
                        state.emitNext(State.DISCONNECTED, options.getEmissionHandler());
                    }
                });
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

            Mono<Void> stateHandler = state.asFlux()
                    .flatMap(state -> {
                        log.debug("Updating state: {}", state);

                        switch (state) {
                            case AUTHORIZATION_END:
                            case AUTHORIZATION_BEGIN:
                                // auth requests doesn't require acknowledging
                                boolean enable = state == State.AUTHORIZATION_END;
                                transport.setQuickAckState(enable);
                                return Mono.empty();
                            case CLOSED:
                                close = true;
                                ackEmitter.dispose();
                                pingEmitter.dispose();
                                closeHook.emitEmpty(FAIL_FAST);
                                this.state.emitComplete(FAIL_FAST);

                                log.debug("[C:0x{}] Disconnected from the datacenter {}", id, dataCenter);
                                return Mono.fromRunnable(connection::dispose)
                                        .onErrorResume(t -> Mono.empty());
                            case DISCONNECTED:
                                ackEmitter.dispose();
                                pingEmitter.dispose();
                                lastPong.set(0);
                                lastPing = 0;
                                missedPong.set(0);

                                return Mono.fromRunnable(connection::dispose)
                                        .onErrorResume(t -> Mono.empty())
                                        .then(Mono.error(RETRY));
                            default:
                                return Mono.empty();
                        }
                    })
                    .then();

            Mono<Void> onConnect = state.asFlux().filter(state -> state == State.CONNECTED).next().then();

            Mono<Void> inboundHandler = connection.inbound()
                    .receive().retain()
                    .publishOn(Schedulers.boundedElastic())
                    .bufferUntil(transport::canDecode)
                    .map(bufs -> alloc.compositeBuffer(bufs.size())
                            .addComponents(true, bufs))
                    .map(transport::decode)
                    .flatMap(payload -> {
                        if (payload.readableBytes() == Integer.BYTES) {
                            int val = payload.readIntLE();
                            payload.release();

                            if (!TransportException.isError(val) && transport.supportQuickAck()) { // quick acknowledge
                                Long id = quickAckTokens.getIfPresent(val);
                                if (id == null) {
                                    log.debug("[C:0x{}] Unserialized quick acknowledge", this.id);
                                    return Mono.empty();
                                }

                                if (rpcLog.isDebugEnabled()) {
                                    rpcLog.debug("[C:0x{}, M:0x{}] Handling quick ack", this.id, Long.toHexString(id));
                                }

                                quickAckTokens.invalidate(val);
                                return Mono.empty();
                            } else { // The error code writes as negative int32
                                val *= -1;
                                TransportException exc = TransportException.create(val);
                                if (authKey == null && val == 404) { // retry authorization
                                    onAuthSink.emitError(new AuthorizationException(exc), FAIL_FAST);
                                    return Mono.empty();
                                }

                                return Mono.error(exc);
                            }
                        }
                        return Mono.just(payload);
                    })
                    .flatMap(buf -> {
                        long authKeyId = buf.readLongLE();

                        if (authKeyId == 0) { // unencrypted message
                            buf.skipBytes(12); // message id (8) + payload length (4)

                            MTProtoObject obj = TlDeserializer.deserialize(buf);
                            buf.release();

                            return authHandler.handle(obj);
                        }

                        long longAuthKeyId = authKey.getAuthKeyId().getLongLE(0);
                        if (authKeyId != longAuthKeyId) {
                            return Mono.error(new IllegalStateException("Incorrect auth key id. Received: 0x"
                                    + Long.toHexString(authKeyId) + ", but excepted: 0x"
                                    + Long.toHexString(longAuthKeyId)));
                        }

                        ByteBuf messageKey = buf.readRetainedSlice(Long.BYTES * 2);

                        ByteBuf authKey = this.authKey.getAuthKey();
                        AES256IGECipher cipher = createAesCipher(messageKey, authKey, true);

                        ByteBuf decrypted = Unpooled.wrappedBuffer(cipher.decrypt(toByteArray(buf)));

                        ByteBuf messageKeyHash = sha256Digest(authKey.slice(96, 32), decrypted);
                        ByteBuf messageKeyHashSlice = messageKeyHash.slice(8, 16);

                        if (!messageKey.equals(messageKeyHashSlice)) {
                            return Mono.error(new IllegalStateException("Incorrect message key. Received: "
                                    + ByteBufUtil.hexDump(messageKey) + ", but recomputed: "
                                    + ByteBufUtil.hexDump(messageKeyHashSlice)));
                        }

                        messageKey.release();

                        long serverSalt = decrypted.readLongLE();
                        long sessionId = decrypted.readLongLE();
                        if (this.sessionId != sessionId) {
                            return Mono.error(new IllegalStateException("Incorrect session identifier. Current: "
                                    + this.sessionId + ", received: " + sessionId));
                        }
                        long messageId = decrypted.readLongLE();
                        int seqNo = decrypted.readIntLE();
                        int length = decrypted.readIntLE();
                        if (length % 4 != 0) {
                            return Mono.error(new IllegalStateException("Length of data isn't a multiple of four"));
                        }

                        updateTimeOffset(messageId >> 32);

                        ByteBuf payload = decrypted.readSlice(length);
                        TlObject obj = TlDeserializer.deserialize(payload);
                        payload.release();

                        return handleServiceMessage(obj, messageId);
                    })
                    .then();

            Flux<PendingRequest> payloadFlux = outbound.asFlux()
                    .filter(e -> isContentRelated(e.method))
                    .delayUntil(e -> onConnect);

            // Perhaps it's better to wrap it in a message container via .buffer(Duration), but there are difficulties with packaging
            Flux<PendingRequest> rpcFlux = outbound.asFlux()
                    .filter(e -> !isContentRelated(e.method));

            Flux<ByteBuf> outboundFlux = Flux.merge(rpcFlux, payloadFlux)
                    .map(req -> {
                        boolean needAck = isContentRelated(req.method);
                        long messageId = getMessageId();
                        requests.put(messageId, req);
                        ByteBuf data = TlSerializer.serialize(alloc, req.method);

                        if (options.getGzipPackingPredicate().test(data.readableBytes())) {
                            data = TlSerializer.serialize(alloc, ImmutableGzipPacked.of(
                                    toByteArray(TlSerialUtil.compressGzip(alloc, data))));
                            // TODO: replace byte[] to bytebuf because it copies the buffer
                        }

                        int seqNo = updateSeqNo(req.method);
                        int minPadding = 12;
                        int unpadded = (32 + data.readableBytes() + minPadding) % 16;
                        int padding = minPadding + (unpadded != 0 ? 16 - unpadded : 0);

                        ByteBuf plainData = alloc.buffer(32 + data.readableBytes() + padding)
                                .writeLongLE(serverSalt)
                                .writeLongLE(sessionId)
                                .writeLongLE(messageId)
                                .writeIntLE(seqNo)
                                .writeIntLE(data.readableBytes())
                                .writeBytes(data)
                                .writeBytes(random.generateSeed(padding));
                        data.release();

                        ByteBuf authKey = this.authKey.getAuthKey();
                        ByteBuf authKeyId = this.authKey.getAuthKeyId();

                        ByteBuf messageKeyHash = sha256Digest(authKey.slice(88, 32), plainData);

                        boolean quickAck = false;
                        if (transport.supportQuickAck() && needAck) {
                            int quickAckToken = messageKeyHash.getIntLE(0) | QUICK_ACK_MASK;
                            quickAckTokens.put(quickAckToken, messageId);
                            quickAck = true;
                        }

                        ByteBuf messageKey = messageKeyHash.slice(8, 16);
                        AES256IGECipher cipher = createAesCipher(messageKey, authKey, false);

                        ByteBuf encrypted = Unpooled.wrappedBuffer(cipher.encrypt(toByteArray(plainData)));
                        ByteBuf message = Unpooled.wrappedBuffer(authKeyId.retain(), messageKey, encrypted);

                        if (rpcLog.isDebugEnabled()) {
                            rpcLog.debug("[C:0x{}, M:0x{}] Sending request: {}", id,
                                    Long.toHexString(messageId), prettyMethodName(req.method));

                        }

                        return transport.encode(message, quickAck);
                    });

            Flux<ByteBuf> authFlux = authOutbound.asFlux()
                    .map(method -> {
                        ByteBuf data = TlSerializer.serialize(alloc, method);
                        ByteBuf payload = alloc.buffer(20 + data.readableBytes())
                                .writeLongLE(0) // auth key id
                                .writeLongLE(getMessageId()) // Message id in the auth requests doesn't allow receiving payload
                                .writeIntLE(data.readableBytes())
                                .writeBytes(data);
                        data.release();

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
                            if (missedPong.incrementAndGet() >= maxMissedPong) {
                                sessionId = random.nextLong();
                                seqNo.set(0);
                                lastMessageId = 0;

                                log.debug("[C:0x{}] Session updated due server forgot it", id);

                                return Mono.empty();
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
                    .doOnError(t -> t != RETRY && !(t instanceof AbortedException) && !(t instanceof IOException), t -> {
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
                .filter(t -> !close && (t == RETRY || t instanceof AbortedException || t instanceof IOException))
                .doAfterRetry(signal -> {
                    state.emitNext(State.RECONNECT, options.getEmissionHandler());
                    log.debug("[C:0x{}] Reconnecting to the datacenter (attempts: {})", id, signal.totalRetriesInARow());
                }))
        .then(Mono.defer(() -> closeHook.asMono()));
    }

    private <T> Mono<T> reconnect() {
        return Mono.defer(() -> {
            if (close) {
                return Mono.empty();
            }

            state.emitNext(State.DISCONNECTED, options.getEmissionHandler());
            return Mono.error(RETRY);
        });
    }

    private Retry authRetry(AuthorizationHandler authHandler) {
        return options.getAuthRetry()
                .filter(t -> t instanceof AuthorizationException)
                .doBeforeRetryAsync(v -> authHandler.start())
                .onRetryExhaustedThrow((spec, signal) -> {
                    state.emitNext(State.CLOSED, options.getEmissionHandler());
                    return new MTProtoException("Failed to generate auth key (" +
                            signal.totalRetries() + "/" + spec.maxAttempts + ")");
                })
                .doAfterRetry(signal -> {
                    log.debug("[C:0x{}] Retrying regernerate auth key (attempts: {})",
                            id, signal.totalRetriesInARow());
                    authContext.clear();
                });
    }

    @Override
    public Sinks.Many<Updates> updates() {
        return updates;
    }

    @Override
    public boolean updateTimeOffset(long serverTime) {
        int updated = Math.toIntExact(serverTime - System.currentTimeMillis() / 1000);
        boolean changed = Math.abs(timeOffset - updated) > 3;

        if (changed) {
            lastMessageId = 0;
            timeOffset = updated;
        }

        return changed;
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
            Sinks.One<R> res = Sinks.one();
            PendingRequest request = new PendingRequest(res, method);
            if (method.identifier() == MsgsAck.ID) {
                res.emitEmpty(FAIL_FAST);
            }

            outbound.emitNext(request, options.getEmissionHandler());
            return res.asMono();
        });
    }

    @Override
    public Mono<Void> send(TlMethod<?> method) {
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
    public Type getType() {
        return type;
    }

    @Override
    public Mono<Void> close() {
        return Mono.justOrEmpty(connection)
                .switchIfEmpty(Mono.error(new IllegalStateException("MTProto client isn't connected")))
                .doOnNext(con -> state.emitNext(State.CLOSED, options.getEmissionHandler()))
                .then();
    }

    private long getMessageId() {
        long millis = System.currentTimeMillis();
        long seconds = millis / 1000;
        long mod = millis % 1000;
        long messageId = seconds + timeOffset << 32 | mod << 22 | random.nextInt(0xFFFF) << 2;

        if (lastMessageId >= messageId) {
            messageId = lastMessageId + 4;
        }

        lastMessageId = messageId;
        return messageId;
    }

    private int updateSeqNo(TlObject object) {
        boolean content = isContentRelated(object);
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
            ByteBuf buf = Unpooled.wrappedBuffer(gzipPacked.packedData());
            obj = TlSerialUtil.decompressGzip(buf);
            buf.release();
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

        if (obj instanceof RpcResult) {
            RpcResult rpcResult = (RpcResult) obj;
            messageId = rpcResult.reqMsgId();
            obj = rpcResult.result();

            if (rpcLog.isDebugEnabled()) {
                rpcLog.debug("[C:0x{}, M:0x{}] Receiving rpc result", id, Long.toHexString(messageId));
            }

            if (obj instanceof GzipPacked) {
                GzipPacked gzipPacked = (GzipPacked) obj;

                ByteBuf buf = Unpooled.wrappedBuffer(gzipPacked.packedData());
                obj = TlSerialUtil.decompressGzip(buf);
                buf.release();
            }

            if (obj instanceof MsgsAck) {
                MsgsAck msgsAck = (MsgsAck) obj;

                if (rpcLog.isDebugEnabled()) {
                    rpcLog.debug("[C:0x{}, M:0x{}] Handling acknowledge for message(s): [{}]",
                            id, Long.toHexString(messageId), msgsAck.msgIds().stream()
                                    .map(l -> String.format("0x%x", l))
                                    .collect(Collectors.joining(", ")));
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
                    requests.remove(messageId);
                    return Mono.justOrEmpty(req)
                            .delayElement(delay)
                            .doOnNext(r -> outbound.emitNext(r, options.getEmissionHandler()))
                            .then();
                }

                obj = RpcException.create(rpcError, req);
            }

            resolve(messageId, obj);
            if (transport.supportQuickAck()) { // already ack'ed
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

        if (obj instanceof NewSession) {
            NewSession newSession = (NewSession) obj;
            if (rpcLog.isDebugEnabled()) {
                rpcLog.debug("[C:0x{}] Receiving new session creation, new server salt: 0x{}",
                        id, Long.toHexString(newSession.serverSalt()));
            }

            serverSalt = newSession.serverSalt();

            return acknowledgmentMessage(messageId);
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
                    if (updateTimeOffset(messageId)) {
                        timeOffset = 0;
                        lastMessageId = 0;
                    }
                    break;
                case 48:
                    BadServerSalt badServerSalt = (BadServerSalt) badMsgNotification;
                    serverSalt = badServerSalt.newServerSalt();
                    break;
            }

            return Mono.fromSupplier(() -> requests.remove(badMsgNotification.badMsgId()))
                    .doOnNext(r -> outbound.emitNext(r, options.getEmissionHandler()))
                    .then();
        }

        if (obj instanceof Updates) {
            Updates updates = (Updates) obj;
            if (rpcLog.isTraceEnabled()) {
                rpcLog.trace("[C:0x{}] Receiving updates: {}", id, updates);
            }

            this.updates.emitNext(updates, options.getEmissionHandler());
        }

        return Mono.empty();
    }

    private Mono<Void> acknowledgmentMessage(long messageId) {
        return Mono.defer(() -> {
            if (!Objects.equals(acknowledgments.peek(), messageId)){
                acknowledgments.add(messageId);
            }

            if (acknowledgments.size() < options.getAcksSendThreshold()) {
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

        return sendAwait(MsgsAck.builder().msgIds(acknowledgments).build())
                .and(Mono.fromRunnable(acknowledgments::clear));
    }

    @SuppressWarnings("unchecked")
    private void resolve(long messageId, @Nullable Object value) {
        requests.computeIfPresent(messageId, (k, v) -> {
            Sinks.One<Object> sink = (Sinks.One<Object>) v.sink;
            if (value == null) {
                sink.emitEmpty(FAIL_FAST);
            } else if (value instanceof Throwable) {
                Throwable value0 = (Throwable) value;
                sink.emitError(value0, FAIL_FAST);
            } else {
                sink.emitValue(value, FAIL_FAST);
            }

            return null;
        });
    }

    static boolean isContentRelated(TlObject object) {
        switch (object.identifier()) {
            case MsgsAck.ID:
            case Ping.ID:
            case PingDelayDisconnect.ID:
            case MessageContainer.ID:
                return false;
            default:
                return true;
        }
    }

    static class PendingRequest {
        final Sinks.One<?> sink;
        final TlMethod<?> method;

        PendingRequest(Sinks.One<?> sink, TlMethod<?> method) {
            this.sink = sink;
            this.method = method;
        }
    }

    static class RetryConnectException extends RuntimeException {

        RetryConnectException() {}

        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }
}
