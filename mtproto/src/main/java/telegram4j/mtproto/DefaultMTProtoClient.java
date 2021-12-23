package telegram4j.mtproto;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelException;
import io.netty.util.ReferenceCountUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.netty.Connection;
import reactor.netty.FutureMono;
import reactor.netty.tcp.TcpClient;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.annotation.Nullable;
import reactor.util.concurrent.Queues;
import reactor.util.retry.Retry;
import telegram4j.mtproto.auth.AuthorizationContext;
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
import telegram4j.tl.request.mtproto.GetFutureSalts;
import telegram4j.tl.request.mtproto.ImmutableGetFutureSalts;
import telegram4j.tl.request.mtproto.ImmutablePing;
import telegram4j.tl.request.mtproto.Ping;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static telegram4j.mtproto.util.CryptoUtil.*;
import static telegram4j.tl.TlSerialUtil.readInt128;

public class DefaultMTProtoClient implements MTProtoClient {
    private static final Logger log = Loggers.getLogger(DefaultMTProtoClient.class);
    private static final Logger rpcLog = Loggers.getLogger("telegram4j.mtproto.rpc");

    private static final Duration FUTURE_SALT_QUERY_PERIOD = Duration.ofMinutes(45);
    private static final Duration PING_QUERY_PERIOD = Duration.ofSeconds(10);

    private final DataCenter dataCenter;
    private final TcpClient tcpClient;
    private final Transport transport;
    private final StoreLayout storeLayout;
    private final int acksSendThreshold;
    private final Sinks.EmitFailureHandler emissionHandler;

    private final AuthorizationContext authContext = new AuthorizationContext();
    private final Sinks.Many<MTProtoObject> authReceiver;
    private final Sinks.Many<TlObject> rpcReceiver;
    private final Sinks.Many<Updates> updates;
    private final Sinks.Many<RequestTuple> outbound;
    private final ResettableInterval futureSaltEmitter;
    private final ResettableInterval pingEmitter;
    private final Sinks.Many<State> state;

    private volatile AuthorizationKeyHolder authorizationKey;
    private volatile Connection connection;
    private volatile long sessionId = random.nextLong();
    private volatile int timeOffset;
    private volatile long serverSalt;
    private volatile long lastMessageId;
    private volatile long lastGeneratedMessageId;
    private final AtomicInteger seqNo = new AtomicInteger();
    private final Queue<Long> acknowledgments = new ConcurrentLinkedQueue<>();
    private final ConcurrentMap<Long, RequestTuple> resolvers = new ConcurrentHashMap<>();

    public DefaultMTProtoClient(MTProtoOptions options) {
        this.dataCenter = options.getDatacenter();
        this.tcpClient = initTcpClient(options.getTcpClient());
        this.transport = options.getTransport();
        this.storeLayout = options.getStoreLayout();
        this.acksSendThreshold = options.getAcksSendThreshold();
        this.emissionHandler = options.getEmissionHandler();

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

    private TcpClient initTcpClient(TcpClient tcpClient) {
        return tcpClient
                .remoteAddress(() -> new InetSocketAddress(dataCenter.getAddress(), dataCenter.getPort()))
                .doOnConnected(con -> {
                    log.debug("Connected to datacenter №{} ({}:{})", dataCenter.getId(),
                            dataCenter.getAddress(), dataCenter.getPort());
                    log.debug("Sending transport identifier to the server.");

                    con.channel().writeAndFlush(transport.identifier(con.channel().alloc()));
                })
                .doOnDisconnected(con -> state.emitNext(State.DISCONNECTED, Sinks.EmitFailureHandler.FAIL_FAST));
    }

    @Override
    public Mono<Void> connect() {
        return tcpClient.connect()
        .flatMap(connection -> {
            this.connection = connection;

            Sinks.One<AuthorizationKeyHolder> onAuthSink = Sinks.one();

            ByteBufAllocator alloc = connection.channel().alloc();

            Mono<Void> stateHandler = state.asFlux()
                    .flatMap(state -> {
                        if (state == State.DISCONNECTED) {
                            // Trigger reconnect
                            return Mono.error(new ChannelException());
                        }
                        return Mono.empty();
                    })
                    .then();

            Mono<Void> onConnect = state.asFlux().filter(state -> state == State.CONNECTED).next().then();

            Mono<Void> inboundHandler = connection.inbound()
                    .receive().retain()
                    .bufferUntil(transport::canDecode)
                    .map(bufs -> alloc.compositeBuffer(bufs.size())
                            .addComponents(true, bufs))
                    .map(transport::decode)
                    .flatMap(payload -> {
                        if (payload.readableBytes() == Integer.BYTES) { // The error code writes as negative int32
                            int code = payload.readIntLE() * -1;
                            return Mono.error(() -> TransportException.create(code));
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

                        long longAuthKeyId = readLongLE(authorizationKey.getAuthKeyId());
                        if (authKeyId != longAuthKeyId) {
                            throw new IllegalStateException("Incorrect auth key id. Received: "
                                    + authKeyId + ", but excepted: " + longAuthKeyId);
                        }

                        byte[] messageKey = readInt128(buf);

                        ByteBuf authKeyBuf = alloc.buffer()
                                .writeBytes(authorizationKey.getAuthKey());

                        AES256IGECipher cipher = createAesCipher(messageKey, authKeyBuf, true);

                        byte[] decrypted = cipher.decrypt(toByteArray(buf));
                        byte[] messageKeyCLarge = sha256Digest(concat(toByteArray(authKeyBuf.slice(96, 32)), decrypted));
                        byte[] messageKeyC = Arrays.copyOfRange(messageKeyCLarge, 8, 24);

                        if (!Arrays.equals(messageKey, messageKeyC)) {
                            throw new IllegalStateException("Incorrect message key.");
                        }

                        ByteBuf decryptedBuf = alloc.buffer()
                                .writeBytes(decrypted);

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
                            throw new IllegalStateException("Length of data isn't a multiple of four.");
                        }

                        updateTimeOffset(messageId >> 32);
                        lastMessageId = messageId;

                        TlObject obj = TlDeserializer.deserialize(decryptedBuf.readBytes(length));
                        decryptedBuf.release();

                        rpcReceiver.emitNext(obj, emissionHandler);
                    })
                    .doOnDiscard(ByteBuf.class, ReferenceCountUtil::safeRelease)
                    .then();

            Mono<Void> outboundHandler = outbound.asFlux()
                    .flatMap(tuple -> tuple.method instanceof MTProtoObject
                            ? Mono.just(tuple) : Mono.just(tuple).delayUntil(t -> onConnect))
                    .flatMap(tuple -> {
                        ByteBuf data = TlSerializer.serialize(alloc, tuple.method);
                        if (tuple.method instanceof MTProtoObject) {
                            ByteBuf payload = alloc.buffer()
                                    .writeLongLE(0) // auth key id
                                    .writeLongLE(tuple.messageId)
                                    .writeIntLE(data.readableBytes())
                                    .writeBytes(data);
                            data.release();

                            return FutureMono.from(connection.channel()
                                    .writeAndFlush(transport.encode(payload)));
                        }

                        int seqNo = updateSeqNo(tuple.method);
                        int minPadding = 12;
                        int unpadded = (32 + data.readableBytes() + minPadding) % 16;
                        int padding = minPadding + (unpadded != 0 ? 16 - unpadded : 0);

                        ByteBuf plainData = alloc.buffer()
                                .writeLongLE(serverSalt)
                                .writeLongLE(sessionId)
                                .writeLongLE(tuple.messageId)
                                .writeIntLE(seqNo)
                                .writeIntLE(data.readableBytes())
                                .writeBytes(data)
                                .writeBytes(random.generateSeed(padding));
                        data.release();

                        byte[] authKey = authorizationKey.getAuthKey();
                        byte[] authKeyId = authorizationKey.getAuthKeyId();

                        byte[] plainDataB = toByteArray(plainData);
                        byte[] msgKeyLarge = sha256Digest(concat(Arrays.copyOfRange(authKey, 88, 120), plainDataB));
                        byte[] messageKey = Arrays.copyOfRange(msgKeyLarge, 8, 24);

                        ByteBuf authKeyBuf = alloc.buffer().writeBytes(authKey);
                        AES256IGECipher cipher = createAesCipher(messageKey, authKeyBuf, false);

                        ByteBuf payload = alloc.buffer()
                                .writeBytes(authKeyId)
                                .writeBytes(messageKey)
                                .writeBytes(cipher.encrypt(plainDataB));

                        return FutureMono.from(connection.channel()
                                .writeAndFlush(transport.encode(payload)));
                    })
                    .then();

            AuthorizationHandler authHandler = new AuthorizationHandler(this, authContext, onAuthSink, alloc);

            Mono<Void> authHandlerFuture = authReceiver.asFlux()
                    .checkpoint("Authorization handler.")
                    .flatMap(authHandler::handle)
                    .then();

            Mono<Void> rpcHandler = rpcReceiver.asFlux()
                    .checkpoint("RPC handler.")
                    .flatMap(obj -> handleServiceMessage(obj, lastMessageId))
                    .then();

            Mono<Void> futureSaltSchedule = futureSaltEmitter.ticks()
                    .flatMap(tick -> sendAwait(ImmutableGetFutureSalts.of(1)))
                    .doOnNext(futureSalts -> {
                        FutureSalt salt = futureSalts.salts().get(0);
                        int delaySeconds = salt.validUntil() - futureSalts.now() - 900;
                        serverSalt = salt.salt();

                        log.debug("Delaying future salt for {} seconds.", delaySeconds);

                        futureSaltEmitter.start(Duration.ofSeconds(delaySeconds), FUTURE_SALT_QUERY_PERIOD);
                    })
                    .then();

            Mono<Void> ping = pingEmitter.ticks()
                    .flatMap(tick -> sendAwait(ImmutablePing.of(random.nextInt())))
                    .then();

            Mono<Void> awaitKey = storeLayout.getAuthorizationKey(dataCenter)
                    .doOnNext(key -> onAuthSink.emitValue(key, Sinks.EmitFailureHandler.FAIL_FAST))
                    .switchIfEmpty(authHandler.start()
                            .checkpoint("Authorization key generation.")
                            .then(onAuthSink.asMono())
                            .doOnNext(auth -> serverSalt = authContext.getServerSalt()) // apply temporal salt
                            .flatMap(key -> storeLayout.updateAuthorizationKey(key)
                                    .thenReturn(key)))
                    .doOnNext(key -> this.authorizationKey = key)
                    .then();

            Mono<Void> startSchedule = Mono.defer(() -> {
                futureSaltEmitter.start(FUTURE_SALT_QUERY_PERIOD);
                pingEmitter.start(PING_QUERY_PERIOD);

                state.emitNext(State.CONNECTED, Sinks.EmitFailureHandler.FAIL_FAST);
                log.info("Connected to datacenter.");
                return Mono.when(futureSaltSchedule, ping);
            });

            return Mono.zip(inboundHandler, outboundHandler, stateHandler, authHandlerFuture,
                            rpcHandler, awaitKey.then(startSchedule))
                    .then();
        })
        .retryWhen(Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(10))
                .filter(t -> t instanceof ChannelException)
                .doBeforeRetry(signal -> {
                    state.emitNext(State.RECONNECT, Sinks.EmitFailureHandler.FAIL_FAST);
                    log.debug("Reconnecting to the dc (attempts: {}).", signal.totalRetries());
                }))
        .then(Mono.defer(() -> connection.onDispose()));
    }

    @Override
    public Flux<TlObject> rpcReceiver() {
        return rpcReceiver.asFlux();
    }

    @Override
    public Sinks.Many<Updates> updates() {
        return updates;
    }

    @Override
    public boolean updateTimeOffset(long serverTime) {
        int updated = Math.toIntExact(serverTime - System.currentTimeMillis() / 1000);
        boolean changed = Math.abs(timeOffset - updated) > 10;

        lastGeneratedMessageId = 0;
        timeOffset = updated;

        return changed;
    }

    @Override
    public <R, T extends TlMethod<R>> Mono<R> sendAwait(T method) {
        return Mono.defer(() -> {
            long messageId = getMessageId();

            Sinks.One<R> res = Sinks.one();
            RequestTuple tuple = new RequestTuple(res, method, messageId);
            if (isAwaitResult(method)) {
                resolvers.put(messageId, tuple);
            } else {
                res.emitEmpty(Sinks.EmitFailureHandler.FAIL_FAST);
            }

            outbound.emitNext(tuple, emissionHandler);
            return res.asMono();
        });
    }

    @Override
    public Mono<Void> send(TlMethod<?> method) {
        return Mono.fromRunnable(() -> {
            RequestTuple tuple = new RequestTuple(Sinks.one(), method, getMessageId());
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
    public Mono<Void> close() {
        return Mono.justOrEmpty(connection)
                .switchIfEmpty(Mono.error(new IllegalStateException("MTProto client isn't connected.")))
                .doOnNext(con -> {
                    resetSession();
                    futureSaltEmitter.dispose();
                    pingEmitter.dispose();

                    connection.dispose();
                    log.debug("Disconnected from the datacenter №{} ({}:{})",
                            dataCenter.getId(), dataCenter.getAddress(), dataCenter.getPort());

                    state.emitNext(State.CLOSED, Sinks.EmitFailureHandler.FAIL_FAST);
                })
                .then();
    }

    private long getMessageId() {
        long millis = System.currentTimeMillis();
        long seconds = millis / 1000;
        long mod = millis % 1000;
        long messageId = seconds + timeOffset << 32 | mod << 22 | random.nextInt(524288) << 2;

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
        ByteBufAllocator alloc = connection.channel().alloc();

        if (isAwaitAcknowledge(messageId)) {
            return sendAcknowledgments();
        }

        // For updates
        if (obj instanceof GzipPacked) {
            GzipPacked gzipPacked = (GzipPacked) obj;
            obj = TlSerialUtil.decompressGzip(alloc.buffer()
                    .writeBytes(gzipPacked.packedData()));
        }

        if (obj instanceof RpcResult) {
            RpcResult rpcResult = (RpcResult) obj;
            rpcLog.debug("[{}] Handling RPC result.", rpcResult.reqMsgId());

            messageId = rpcResult.reqMsgId();
            obj = rpcResult.result();

            if (obj instanceof GzipPacked) {
                GzipPacked gzipPacked = (GzipPacked) obj;

                ByteBuf buf = alloc.buffer()
                        .writeBytes(gzipPacked.packedData());
                obj = TlSerialUtil.decompressGzip(buf);
                buf.release();
            }

            if (obj instanceof MsgsAck) {
                MsgsAck msgsAck = (MsgsAck) obj;

                if (rpcLog.isDebugEnabled()) {
                    rpcLog.debug("Handling acknowledge for message(s): {}", msgsAck.msgIds());
                }
                return Mono.empty();
            }

            if (obj instanceof Pong) {
                Pong pong = (Pong) obj;
                messageId = pong.msgId();
            } else if (obj instanceof FutureSalts) {
                FutureSalts futureSalts = (FutureSalts) obj;
                messageId = futureSalts.reqMsgId();
            }

            RequestTuple req = resolvers.get(messageId);
            if (obj instanceof RpcError) {
                RpcError rpcError = (RpcError) obj;
                if (rpcError.errorCode() == 420) { // FLOOD_WAIT_X
                    String arg = rpcError.errorMessage().substring(
                            rpcError.errorMessage().lastIndexOf('_') + 1);
                    Duration delay = Duration.ofSeconds(Integer.parseInt(arg));

                    long messageId0 = messageId;
                    rpcLog.debug("[{}] Delaying for {} seconds.", messageId0, delay);

                    // Need resend with delay.
                    return Mono.fromSupplier(() -> resolvers.get(messageId0))
                            .delayElement(delay)
                            .map(t -> new RequestTuple(t, getMessageId()))
                            .doOnNext(t -> resolvers.put(t.messageId, t))
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
                rpcLog.debug("Handling message container: {}", messageContainer);
            }

            return Flux.fromIterable(messageContainer.messages())
                    .flatMap(message -> handleServiceMessage(
                            message.body(), message.msgId()))
                    .then();
        }

        if (obj instanceof NewSession) {
            NewSession newSession = (NewSession) obj;

            rpcLog.debug("Handling new session salt creation.");

            serverSalt = newSession.serverSalt();

            return acknowledgmentMessage(messageId);
        }

        if (obj instanceof BadMsgNotification) {
            BadMsgNotification badMsgNotification = (BadMsgNotification) obj;
            if (rpcLog.isDebugEnabled()) {
                rpcLog.debug("Handling bad msg notification: {}", badMsgNotification);
            }

            if (obj instanceof BadServerSalt) {
                BadServerSalt badServerSalt = (BadServerSalt) obj;

                serverSalt = badServerSalt.newServerSalt();
            }

            switch (badMsgNotification.errorCode()) {
                case 16: // msg_id too low
                case 17: // msg_id too high
                    if (updateTimeOffset(lastMessageId)) {
                        resetSession();
                    }
                    break;
            }

            return Mono.fromSupplier(() -> resolvers.get(badMsgNotification.badMsgId()))
                    .map(t -> new RequestTuple(t, getMessageId()))
                    .doOnNext(t -> resolvers.put(t.messageId, t))
                    .doOnNext(tuple -> outbound.emitNext(tuple, emissionHandler))
                    .then();
        }

        if (isNotPartialUpdates(obj)) {
            Updates updates = (Updates) obj;
            if (rpcLog.isTraceEnabled()) {
                rpcLog.trace("Receiving updates: {}", updates);
            }

            this.updates.emitNext(updates, emissionHandler);
        }

        return acknowledgmentMessage(messageId);
    }

    private Mono<Void> acknowledgmentMessage(long messageId) {
        acknowledgments.add(messageId);

        return sendAcknowledgments();
    }

    private boolean isAwaitAcknowledge(long messageId) {
        return acknowledgments.contains(messageId) &&
                acknowledgments.size() + 1 > acksSendThreshold;
    }

    private Mono<Void> sendAcknowledgments() {
        return Mono.defer(() -> {
            if (acknowledgments.size() < acksSendThreshold) {
                return Mono.empty();
            }

            if (rpcLog.isDebugEnabled()) {
                rpcLog.debug("Sending acknowledges for message(s): {}", acknowledgments);
            }

            return sendAwait(MsgsAck.builder().msgIds(acknowledgments).build())
                    .and(Mono.fromRunnable(acknowledgments::clear));
        });
    }

    @SuppressWarnings("unchecked")
    private void resolve(long messageId, @Nullable Object value) {
        resolvers.computeIfPresent(messageId, (k, tuple) -> {
            Sinks.One<Object> sink = (Sinks.One<Object>) tuple.sink;
            if (value == null) {
                sink.emitEmpty(Sinks.EmitFailureHandler.FAIL_FAST);
            } else if (value instanceof Throwable) {
                Throwable value0 = (Throwable) value;
                sink.emitError(value0, Sinks.EmitFailureHandler.FAIL_FAST);
            } else {
                sink.emitValue(value, Sinks.EmitFailureHandler.FAIL_FAST);
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
        return !(object instanceof MsgsAck) && !(object instanceof Ping) && !(object instanceof MessageContainer);
    }

    static boolean isAwaitResult(TlMethod<?> method) {
        return !(method instanceof MsgsAck);
    }

    static class RequestTuple {
        final Sinks.One<?> sink;
        final TlMethod<?> method;
        final long messageId;

        RequestTuple(RequestTuple tuple, long messageId) {
            this(tuple.sink, tuple.method, messageId);
        }

        RequestTuple(Sinks.One<?> sink, TlMethod<?> method, long messageId) {
            this.sink = sink;
            this.method = method;
            this.messageId = messageId;
        }
    }
}
