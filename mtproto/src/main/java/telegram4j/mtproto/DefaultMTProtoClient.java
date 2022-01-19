package telegram4j.mtproto;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
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
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.mtproto.transport.Transport;
import telegram4j.mtproto.util.AES256IGECipher;
import telegram4j.tl.*;
import telegram4j.tl.api.MTProtoObject;
import telegram4j.tl.api.TlMethod;
import telegram4j.tl.api.TlObject;
import telegram4j.tl.mtproto.*;
import telegram4j.tl.request.mtproto.ImmutableGetFutureSalts;
import telegram4j.tl.request.mtproto.ImmutablePingDelayDisconnect;
import telegram4j.tl.request.mtproto.Ping;
import telegram4j.tl.request.mtproto.PingDelayDisconnect;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;
import static telegram4j.mtproto.RpcException.prettyMethodName;
import static telegram4j.mtproto.transport.IntermediateTransport.QUICK_ACK_MASK;
import static telegram4j.mtproto.util.CryptoUtil.*;
import static telegram4j.tl.TlSerialUtil.readInt128;

public class DefaultMTProtoClient implements MTProtoClient {
    private static final Logger log = Loggers.getLogger(DefaultMTProtoClient.class);
    private static final Logger rpcLog = Loggers.getLogger("telegram4j.mtproto.rpc");

    private static final Throwable RETRY = new RetryConnectException();
    private static final Duration FUTURE_SALT_QUERY_PERIOD = Duration.ofMinutes(45);
    private static final Duration PING_QUERY_PERIOD = Duration.ofSeconds(10);
    private static final int PING_TIMEOUT = (int) (PING_QUERY_PERIOD.getSeconds() + 10);

    private final DataCenter dataCenter;
    private final TcpClient tcpClient;
    private final Transport transport;
    private final StoreLayout storeLayout;
    private final int acksSendThreshold;
    private final Sinks.EmitFailureHandler emissionHandler;
    private final Type type;

    private final AuthorizationContext authContext = new AuthorizationContext();
    private final Sinks.Many<MTProtoObject> authReceiver;
    private final Sinks.Many<TlObject> rpcReceiver;
    private final Sinks.Many<Updates> updates;
    private final Sinks.Many<RequestEntry> outbound;
    private final ResettableInterval futureSaltEmitter;
    private final ResettableInterval pingEmitter;
    private final Sinks.Many<State> state;

    private final String id = Integer.toHexString(hashCode());
    private final MTProtoOptions options;

    private volatile Sinks.Empty<Void> closeHook;
    private volatile boolean close = false; // needed for detecting close and disconnect states
    private volatile AuthorizationKeyHolder authKey;
    private volatile Connection connection;
    private volatile long sessionId = random.nextLong();
    private volatile long timeOffset;
    private volatile long serverSalt;
    private volatile long lastMessageId;
    private volatile long lastGeneratedMessageId;
    private final AtomicInteger seqNo = new AtomicInteger();
    private final Queue<Long> acknowledgments = new ConcurrentLinkedQueue<>();
    private final ConcurrentMap<Long, RequestEntry> requests = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, Long> quickAckTokens = new ConcurrentHashMap<>();

    DefaultMTProtoClient(Type type, DataCenter dataCenter, MTProtoOptions options) {
        this.type = type;
        this.dataCenter = dataCenter;
        this.tcpClient = initTcpClient(options.getTcpClient());
        this.transport = options.getTransport().get();
        this.storeLayout = options.getStoreLayout();
        this.acksSendThreshold = options.getAcksSendThreshold();
        this.emissionHandler = options.getEmissionHandler();
        this.options = options;

        this.authReceiver = Sinks.many().multicast()
                .onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);
        this.rpcReceiver = Sinks.many().multicast()
                .onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);
        this.updates = Sinks.many().multicast()
                .onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);
        this.outbound = Sinks.many().multicast()
                .onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);
        this.state = Sinks.many().replay()
                .latestOrDefault(State.RECONNECT);
        this.futureSaltEmitter = new ResettableInterval(Schedulers.boundedElastic());
        this.pingEmitter = new ResettableInterval(Schedulers.boundedElastic());
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

                        con.channel().writeAndFlush(transport.identifier(con.channel().alloc()));
                    } else if (!close && (st == ConnectionObserver.State.DISCONNECTING ||
                            st == ConnectionObserver.State.RELEASED)) {
                        state.emitNext(State.DISCONNECTED, emissionHandler);
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

            Mono<Void> stateHandler = state.asFlux()
                    .flatMap(state -> {
                        switch (state) {
                            case AUTHORIZATION_END:
                            case AUTHORIZATION_BEGIN:
                                // auth requests doesn't require acknowledging
                                boolean enable = state == State.AUTHORIZATION_END;
                                transport.setQuickAckState(enable);
                                return Mono.empty();
                            case CLOSED:
                                close = true;
                                resetSession();
                                futureSaltEmitter.dispose();
                                pingEmitter.dispose();
                                closeHook.emitEmpty(FAIL_FAST);
                                this.state.emitComplete(FAIL_FAST);

                                log.debug("[C:0x{}] Disconnected from the datacenter {}", id, dataCenter);
                                return Mono.fromRunnable(connection::dispose)
                                        .onErrorResume(t -> Mono.empty());
                            case DISCONNECTED:
                                futureSaltEmitter.dispose();
                                pingEmitter.dispose();

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
                            if (val != 0 && transport.supportQuickAck()) { // quick acknowledge
                                Long id = quickAckTokens.get(val);
                                if (id == null) {
                                    log.debug("[C:0x{}] Unserialized quick acknowledge", this.id);
                                    return Mono.empty();
                                }

                                if (rpcLog.isDebugEnabled()) {
                                    rpcLog.debug("[C:0x{}, M:0x{}] Handling quick ack", this.id, Long.toHexString(id));
                                }

                                quickAckTokens.remove(val);
                                return Mono.empty();
                            } else { // The error code writes as negative int32
                                TransportException exc = TransportException.create(val * -1);
                                if (authKey == null && val == -404) { // retry authorization
                                    onAuthSink.emitError(new AuthorizationException(exc), FAIL_FAST);
                                    return Mono.empty();
                                }

                                return Mono.error(exc);
                            }
                        }
                        return Mono.just(payload);
                    })
                    .doOnNext(buf -> {
                        long authKeyId = buf.readLongLE();

                        if (authKeyId == 0) { // unencrypted message
                            buf.skipBytes(12); // message id (8) + payload length (4)

                            MTProtoObject obj = TlDeserializer.deserialize(buf);
                            authReceiver.emitNext(obj, emissionHandler);
                            return;
                        }

                        long longAuthKeyId = readLongLE(authKey.getAuthKeyId());
                        if (authKeyId != longAuthKeyId) {
                            throw new IllegalStateException("Incorrect auth key id. Received: "
                                    + authKeyId + ", but excepted: " + longAuthKeyId);
                        }

                        byte[] messageKey = readInt128(buf);

                        // Releasing is unnecessary because it clears byte array
                        ByteBuf authKeyBuf = Unpooled.wrappedBuffer(authKey.getAuthKey());
                        AES256IGECipher cipher = createAesCipher(messageKey, authKeyBuf, true);

                        byte[] decrypted = cipher.decrypt(toByteArray(buf));
                        byte[] messageKeyCLarge = sha256Digest(toByteArray(authKeyBuf.slice(96, 32)), decrypted);
                        byte[] messageKeyC = Arrays.copyOfRange(messageKeyCLarge, 8, 24);

                        if (!Arrays.equals(messageKey, messageKeyC)) {
                            throw new IllegalStateException("Incorrect message key");
                        }

                        ByteBuf decryptedBuf = Unpooled.wrappedBuffer(decrypted);

                        long serverSalt = decryptedBuf.readLongLE();
                        long sessionId = decryptedBuf.readLongLE();
                        if (this.sessionId != sessionId) {
                            throw new IllegalStateException("Incorrect session identifier. Current: "
                                    + this.sessionId + ", received: " + sessionId);
                        }
                        long messageId = decryptedBuf.readLongLE();
                        int seqNo = decryptedBuf.readIntLE();
                        int length = decryptedBuf.readIntLE();

                        if (length % 4 != 0) {
                            throw new IllegalStateException("Length of data isn't a multiple of four");
                        }

                        updateTimeOffset(messageId >> 32);
                        lastMessageId = messageId;

                        ByteBuf part = decryptedBuf.readBytes(length);
                        decryptedBuf.release();
                        TlObject obj = TlDeserializer.deserialize(part);
                        part.release();

                        rpcReceiver.emitNext(obj, emissionHandler);
                    })
                    .then();

            Mono<Void> outboundHandler = outbound.asFlux()
                    .flatMap(tuple -> isMtprotoRequest(tuple.method)
                            ? Mono.just(tuple) : Mono.just(tuple).delayUntil(t -> onConnect))
                    .flatMap(tuple -> {
                        ByteBuf data = TlSerializer.serialize(alloc, tuple.method);
                        if (isMtprotoRequest(tuple.method)) {
                            ByteBuf payload = alloc.buffer(20 + data.readableBytes())
                                    .writeLongLE(0) // auth key id
                                    .writeLongLE(tuple.messageId)
                                    .writeIntLE(data.readableBytes())
                                    .writeBytes(data);
                            data.release();

                            return FutureMono.from(connection.channel()
                                    .writeAndFlush(transport.encode(payload)));
                        }

                        if (options.getGzipPackingPredicate().test(data.readableBytes())) {
                            data = TlSerializer.serialize(alloc, ImmutableGzipPacked.of(
                                    toByteArray(TlSerialUtil.compressGzip(alloc, data))));
                            // TODO: replace byte[] to bytebuf because it copies the buffer
                        }

                        int seqNo = updateSeqNo(tuple.method);
                        int minPadding = 12;
                        int unpadded = (32 + data.readableBytes() + minPadding) % 16;
                        int padding = minPadding + (unpadded != 0 ? 16 - unpadded : 0);

                        ByteBuf plainData = alloc.buffer(32 + data.readableBytes() + padding)
                                .writeLongLE(serverSalt)
                                .writeLongLE(sessionId)
                                .writeLongLE(tuple.messageId)
                                .writeIntLE(seqNo)
                                .writeIntLE(data.readableBytes())
                                .writeBytes(data)
                                .writeBytes(random.generateSeed(padding));
                        data.release();

                        byte[] authKey = this.authKey.getAuthKey();
                        byte[] authKeyId = this.authKey.getAuthKeyId();

                        byte[] plainDataB = toByteArray(plainData);
                        byte[] msgKeyLarge = sha256Digest(Arrays.copyOfRange(authKey, 88, 120), plainDataB);

                        if (transport.supportQuickAck() && tuple.method.identifier() != Ping.ID
                                && tuple.method.identifier() != PingDelayDisconnect.ID) {
                            int quickAck = readIntLE(msgKeyLarge) | QUICK_ACK_MASK;
                            quickAckTokens.put(quickAck, tuple.messageId);
                        }

                        byte[] messageKey = Arrays.copyOfRange(msgKeyLarge, 8, 24);

                        ByteBuf authKeyBuf = Unpooled.wrappedBuffer(authKey);
                        AES256IGECipher cipher = createAesCipher(messageKey, authKeyBuf, false);

                        byte[] enc = cipher.encrypt(plainDataB);
                        ByteBuf payload = alloc.buffer(enc.length + authKeyId.length + messageKey.length)
                                .writeBytes(authKeyId)
                                .writeBytes(messageKey)
                                .writeBytes(enc);

                        if (rpcLog.isDebugEnabled()) {
                            rpcLog.debug("[C:0x{}, M:0x{}] Sending request: {}", id,
                                    Long.toHexString(tuple.messageId), prettyMethodName(tuple.method));
                        }

                        return FutureMono.from(connection.channel()
                                .writeAndFlush(transport.encode(payload)));
                    })
                    .then();

            AuthorizationHandler authHandler = new AuthorizationHandler(this, authContext, onAuthSink, alloc);

            Mono<Void> authHandlerFuture = authReceiver.asFlux()
                    .checkpoint("Authorization handler")
                    .flatMap(authHandler::handle)
                    .then();

            Mono<Void> rpcHandler = rpcReceiver.asFlux()
                    .checkpoint("RPC handler")
                    .flatMap(obj -> handleServiceMessage(obj, lastMessageId))
                    .then();

            Mono<Void> futureSaltSchedule = futureSaltEmitter.ticks()
                    .flatMap(tick -> sendAwait(ImmutableGetFutureSalts.of(1)))
                    .doOnNext(futureSalts -> {
                        FutureSalt salt = futureSalts.salts().get(0);
                        int delaySeconds = salt.validUntil() - futureSalts.now() - 900;
                        serverSalt = salt.salt();

                        log.debug("[C:0x{}] Delaying future salt for {} seconds", id, delaySeconds);

                        futureSaltEmitter.start(Duration.ofSeconds(delaySeconds), FUTURE_SALT_QUERY_PERIOD);
                    })
                    .then();

            Mono<Void> ping = pingEmitter.ticks()
                    .flatMap(tick -> send(ImmutablePingDelayDisconnect.of(random.nextLong(), PING_TIMEOUT)))
                    .then();

            Mono<AuthorizationKeyHolder> startAuth = Mono.fromRunnable(() -> state.emitNext(State.AUTHORIZATION_BEGIN, FAIL_FAST))
                    .then(authHandler.start())
                    .checkpoint("Authorization key generation")
                    .then(onAuthSink.asMono().retryWhen(authRetry()))
                    .doOnNext(auth -> {
                        state.emitNext(State.AUTHORIZATION_END, FAIL_FAST);
                        serverSalt = authContext.getServerSalt(); // apply temporal salt
                    })
                    .flatMap(key -> storeLayout.updateAuthorizationKey(key).thenReturn(key));

            Mono<Void> awaitKey = Mono.justOrEmpty(authKey)
                    .switchIfEmpty(storeLayout.getAuthorizationKey(dataCenter))
                    .doOnNext(key -> onAuthSink.emitValue(key, FAIL_FAST))
                    .switchIfEmpty(startAuth)
                    .doOnNext(key -> this.authKey = key)
                    .then();

            Mono<Void> startSchedule = Mono.defer(() -> {
                futureSaltEmitter.start(FUTURE_SALT_QUERY_PERIOD);
                pingEmitter.start(PING_QUERY_PERIOD);
                state.emitNext(State.CONNECTED, FAIL_FAST);
                log.info("[C:0x{}] Connected to datacenter.", id);
                return Mono.when(futureSaltSchedule, ping);
            });

            return Mono.zip(inboundHandler, outboundHandler, stateHandler, authHandlerFuture,
                            rpcHandler, awaitKey.then(startSchedule))
                    .doOnError(t -> t != RETRY, t -> {
                        if (t instanceof TransportException) {
                            TransportException t0 = (TransportException) t;
                            log.error("[C:0x" + id + "] Transport exception, code: " + t0.getCode(), t0);
                        } else if (!(t instanceof AuthorizationException)) {
                            log.error("[C:0x" + id + "] Unexpected client exception", t);
                        }
                    })
                    .then();
        })
        // FluxReceive (inbound) emits empty signals if channel was DISCONNECTING
        .switchIfEmpty(Mono.defer(() -> {
            if (close) {
                return Mono.empty();
            }

            state.emitNext(State.DISCONNECTED, emissionHandler);
            return Mono.error(RETRY);
        }))
        .retryWhen(options.getRetry()
                .filter(t -> !close && (t == RETRY || t instanceof AbortedException || t instanceof IOException))
                .doAfterRetry(signal -> {
                    state.emitNext(State.RECONNECT, FAIL_FAST);
                    log.debug("[C:0x{}] Reconnecting to the datacenter (attempts: {})", id, signal.totalRetries());
                }))
        .onErrorResume(t -> Mono.empty())
        .then(Mono.defer(() -> closeHook.asMono()));
    }

    private Retry authRetry() {
        return options.getAuthRetry()
                .filter(t -> t instanceof AuthorizationException)
                .onRetryExhaustedThrow((spec, signal) -> {
                    state.emitNext(State.CLOSED, emissionHandler);
                    return new MTProtoException("Failed to generate auth key (" +
                            signal.totalRetries() + "/" + spec.maxAttempts + ")");
                })
                .doAfterRetry(signal -> {
                    log.debug("[C:0x{}] Retrying regernerate auth key (attempts: {})",
                            id, signal.totalRetries());
                    authContext.clear();
                });
    }

    @Override
    public Sinks.Many<Updates> updates() {
        return updates;
    }

    @Override
    public boolean updateTimeOffset(long serverTime) {
        long updated = serverTime - System.currentTimeMillis() / 1000;
        boolean changed = Math.abs(timeOffset - updated) > 3;

        if (changed) {
            lastGeneratedMessageId = 0;
            timeOffset = updated;
        }

        return changed;
    }

    @Override
    public <R, T extends TlMethod<R>> Mono<R> sendAwait(T method) {
        return Mono.defer(() -> {
            long messageId = getMessageId();

            Sinks.One<R> res = Sinks.one();
            RequestEntry tuple = new RequestEntry(res, method, messageId);
            if (method.identifier() != MsgsAck.ID) {
                requests.put(messageId, tuple);
            } else {
                res.emitEmpty(FAIL_FAST);
            }

            outbound.emitNext(tuple, emissionHandler);
            return res.asMono();
        });
    }

    @Override
    public Mono<Void> send(TlMethod<?> method) {
        return Mono.fromRunnable(() -> {
            RequestEntry tuple = new RequestEntry(Sinks.one(), method, getMessageId());
            outbound.emitNext(tuple, emissionHandler);
        });
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
        client.lastGeneratedMessageId = lastGeneratedMessageId;
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
                .doOnNext(con -> state.emitNext(State.CLOSED, emissionHandler))
                .then();
    }

    private long getMessageId() {
        long millis = System.currentTimeMillis();
        long seconds = millis / 1000;
        long mod = millis % 1000;
        long messageId = seconds + timeOffset << 32 | mod << 22 | random.nextInt(0xFFFF) << 2;

        if (lastGeneratedMessageId >= messageId) {
            messageId = lastGeneratedMessageId + 4;
        }

        lastGeneratedMessageId = messageId;
        return messageId;
    }

    private void resetSession() {
        sessionId = random.nextLong();
        timeOffset = 0;
        lastGeneratedMessageId = 0;
        seqNo.set(0);
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
            resolve(messageId, obj);
            return Mono.empty();
        }

        if (obj instanceof RpcResult) {
            RpcResult rpcResult = (RpcResult) obj;
            if (rpcLog.isDebugEnabled()) {
                rpcLog.debug("[C:0x{}, M:0x{}] Handling RPC result", id, Long.toHexString(rpcResult.reqMsgId()));
            }

            messageId = rpcResult.reqMsgId();
            obj = rpcResult.result();

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

            if (obj instanceof FutureSalts) {
                FutureSalts futureSalts = (FutureSalts) obj;
                messageId = futureSalts.reqMsgId();
            }

            RequestEntry req = requests.get(messageId);
            if (obj instanceof RpcError) {
                RpcError rpcError = (RpcError) obj;
                if (rpcError.errorCode() == 420) { // FLOOD_WAIT_X
                    String arg = rpcError.errorMessage().substring(
                            rpcError.errorMessage().lastIndexOf('_') + 1);
                    Duration delay = Duration.ofSeconds(Integer.parseInt(arg));

                    long messageId0 = messageId;
                    if (rpcLog.isDebugEnabled()) {
                        rpcLog.debug("[C:0x{}, M:0x{}] Delaying resend for {}", id, Long.toHexString(messageId0), delay);
                    }

                    // Need resend with delay.
                    return Mono.fromSupplier(() -> requests.get(messageId0))
                            .doOnNext(t -> requests.remove(messageId0))
                            .delayElement(delay)
                            .map(t -> t.withMessageId(getMessageId()))
                            .doOnNext(t -> requests.put(t.messageId, t))
                            .doOnNext(tuple -> outbound.emitNext(tuple, emissionHandler))
                            .then();
                }

                obj = RpcException.create(rpcError, req);
            }

            resolve(messageId, obj);
            return acknowledgmentMessage(messageId);
        }

        if (obj instanceof MessageContainer) {
            MessageContainer messageContainer = (MessageContainer) obj;
            if (rpcLog.isDebugEnabled()) {
                rpcLog.debug("[C:0x{}] Handling message container", id);
            } else if (rpcLog.isTraceEnabled()) {
                rpcLog.debug("[C:0x{}] Handling message container: {}", id, messageContainer);
            }

            return Flux.fromIterable(messageContainer.messages())
                    .flatMap(message -> handleServiceMessage(
                            message.body(), message.msgId()))
                    .then();
        }

        if (obj instanceof NewSession) {
            NewSession newSession = (NewSession) obj;
            rpcLog.debug("[C:0x{}] Handling new session salt creation", id);

            serverSalt = newSession.serverSalt();

            return acknowledgmentMessage(messageId);
        }

        if (obj instanceof BadMsgNotification) {
            BadMsgNotification badMsgNotification = (BadMsgNotification) obj;
            if (rpcLog.isDebugEnabled()) {
                rpcLog.debug("[C:0x{}] Handling notification: {}", id, badMsgNotification);
            }

            switch (badMsgNotification.errorCode()) {
                case 16: // msg_id too low
                case 17: // msg_id too high
                    if (updateTimeOffset(lastMessageId)) {
                        resetSession();
                    }
                    break;
                case 48:
                    BadServerSalt badServerSalt = (BadServerSalt) badMsgNotification;
                    serverSalt = badServerSalt.newServerSalt();
                    break;
            }

            return Mono.fromSupplier(() -> requests.get(badMsgNotification.badMsgId()))
                    .doOnNext(t -> requests.remove(badMsgNotification.badMsgId()))
                    .map(t -> t.withMessageId(getMessageId()))
                    .doOnNext(t -> requests.put(t.messageId, t))
                    .doOnNext(tuple -> outbound.emitNext(tuple, emissionHandler))
                    .then();
        }

        if (isNotPartialUpdates(obj)) {
            Updates updates = (Updates) obj;
            if (rpcLog.isTraceEnabled()) {
                rpcLog.trace("[C:0x{}] Receiving updates: {}", id, updates);
            }

            this.updates.emitNext(updates, emissionHandler);
        }

        return Mono.empty();
    }

    private Mono<Void> acknowledgmentMessage(long messageId) {
        if (transport.supportQuickAck()) {
            return Mono.empty();
        }

        if (!Objects.equals(acknowledgments.peek(), messageId)){
            acknowledgments.add(messageId);
        }

        return Mono.defer(() -> {
            if (acknowledgments.size() < acksSendThreshold) {
                return Mono.empty();
            }

            if (rpcLog.isDebugEnabled()) {
                rpcLog.debug("[C:0x{}] Sending acknowledges for message(s): [{}]", id, acknowledgments.stream()
                        .map(l -> String.format("0x%x", l))
                        .collect(Collectors.joining(", ")));
            }

            return sendAwait(MsgsAck.builder().msgIds(acknowledgments).build())
                    .and(Mono.fromRunnable(acknowledgments::clear));
        });
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

    static boolean isNotPartialUpdates(Object obj) {
        return obj instanceof Updates && !(obj instanceof UpdateShortMessage) &&
                !(obj instanceof UpdateShortSentMessage) &&
                !(obj instanceof UpdateShortChatMessage);
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

    static boolean isMtprotoRequest(TlMethod<?> method) {
        return !(method instanceof Ping) && !(method instanceof PingDelayDisconnect)
                && !(method instanceof MsgsAck) && method instanceof MTProtoObject;
    }

    static class RequestEntry {
        final Sinks.One<?> sink;
        final TlMethod<?> method;
        final long messageId;

        RequestEntry(Sinks.One<?> sink, TlMethod<?> method, long messageId) {
            this.sink = sink;
            this.method = method;
            this.messageId = messageId;
        }

        RequestEntry withMessageId(long messageId) {
            if (messageId == this.messageId) {
                return this;
            }
            return new RequestEntry(sink, method, messageId);
        }

        @Override
        public String toString() {
            return method.toString();
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
