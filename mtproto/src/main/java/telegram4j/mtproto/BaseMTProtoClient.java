package telegram4j.mtproto;

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
import reactor.core.publisher.MonoSink;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.netty.Connection;
import reactor.netty.ConnectionObserver;
import reactor.netty.FutureMono;
import reactor.netty.channel.AbortedException;
import reactor.netty.tcp.TcpClient;
import reactor.util.Logger;
import reactor.util.Loggers;
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
import telegram4j.tl.api.TlMethod;
import telegram4j.tl.api.TlObject;
import telegram4j.tl.mtproto.*;
import telegram4j.tl.request.InvokeWithLayer;
import telegram4j.tl.request.mtproto.*;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;
import static telegram4j.mtproto.transport.Transport.QUICK_ACK_MASK;
import static telegram4j.mtproto.util.CryptoUtil.*;

class BaseMTProtoClient implements MTProtoClient {
    private static final Logger log = Loggers.getLogger("telegram4j.mtproto.MTProtoClient");
    private static final Logger rpcLog = Loggers.getLogger("telegram4j.mtproto.rpc");

    private static final Throwable RETRY = new RetryConnectException();
    private static final Duration PING_QUERY_PERIOD = Duration.ofSeconds(5);
    private static final Duration PING_QUERY_PERIOD_MEDIA = PING_QUERY_PERIOD.multipliedBy(2);
    private static final int PING_TIMEOUT = 60;

    // limit for service container like a MsgsAck, MsgsStateReq
    private static final int MAX_IDS_SIZE = 8192;
    private static final int MAX_CONTAINER_SIZE = 1020; // count of messages
    private static final int MAX_CONTAINER_LENGTH = 1 << 15; // length in bytes

    private final TcpClient tcpClient;
    private final Transport transport;

    private final Sinks.Many<RpcRequest> outbound;
    private final Sinks.Many<TlMethod<?>> authOutbound;
    private final ResettableInterval pingEmitter;
    private final Sinks.Many<State> state;

    private final String id = Integer.toHexString(hashCode());
    private final DataCenter dataCenter;
    private final DcId.Type type;

    protected final MTProtoOptions options;
    protected final InnerStats stats = new InnerStats();
    protected final AtomicBoolean inflightPing = new AtomicBoolean();

    private volatile AuthorizationHandler authHandler;
    private volatile long oldSessionId;
    private volatile long sessionId = random.nextLong();
    private volatile Connection connection;
    private volatile boolean closed;

    protected volatile AuthorizationKeyHolder authKey;
    protected volatile int timeOffset;
    protected volatile long serverSalt;
    protected volatile long lastMessageId;

    private final AtomicInteger seqNo = new AtomicInteger();
    private final Queue<Long> acknowledgments = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Long, Request> requests = new ConcurrentHashMap<>();

    BaseMTProtoClient(DcId.Type type, DataCenter dataCenter, MTProtoOptions options) {
        this.type = type;
        this.dataCenter = dataCenter;
        this.tcpClient = initTcpClient(options.getTcpClient());
        this.transport = options.getTransport().get();
        this.options = options;

        this.outbound = Sinks.unsafe().many().multicast()
                .onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);
        this.authOutbound = Sinks.unsafe().many().multicast()
                .onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);
        this.state = Sinks.many().replay()
                .latestOrDefault(State.RECONNECT);
        this.pingEmitter = new ResettableInterval(Schedulers.parallel());
    }

    private Mono<Void> acquireConnection(MonoSink<Void> readyTrigger) {
        return tcpClient.connect().flatMap(connection -> {
            this.connection = connection;

            Sinks.One<AuthorizationKeyHolder> onAuthSink = Sinks.one();
            ByteBufAllocator alloc = connection.channel().alloc();

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
                                pingEmitter.dispose();

                                if (log.isDebugEnabled()) {
                                    log.debug("[C:0x{}] Disconnected from the datacenter {}", id, dataCenter);
                                }

                                return Mono.fromRunnable(connection::dispose)
                                        .onErrorResume(t -> Mono.empty());
                            case DISCONNECTED:
                                oldSessionId = sessionId;
                                sessionId = random.nextLong();
                                seqNo.set(0);
                                inflightPing.set(false);
                                pingEmitter.dispose();

                                return Mono.fromRunnable(connection::dispose)
                                        .onErrorResume(t -> Mono.empty())
                                        .then(Mono.error(RETRY));
                            case READY:
                                readyTrigger.success();
                                return Mono.empty();
                            case CONNECTED:
                                Mono<Void> deletePrevSession = Mono.defer(() -> {
                                    long oldSessionId = this.oldSessionId;
                                    if (oldSessionId == 0 || oldSessionId == sessionId) {
                                        return Mono.empty();
                                    }
                                    return sendAwait(ImmutableDestroySession.of(oldSessionId));
                                })
                                .then();

                                Mono<Void> initConnection = sendAwait(options.getInitConnection())
                                        .doOnNext(result -> this.state.emitNext(State.READY, options.getEmissionHandler()))
                                        .then();

                                return deletePrevSession
                                        .and(initConnection);
                            default:
                                return Mono.empty();
                        }
                    })
                    .then();

            Mono<Void> onReady = state.asFlux().filter(state -> state == State.READY).next().then();

            Mono<Void> inboundHandler = connection.inbound().receive()
                    .map(ByteBuf::retainedDuplicate)
                    .flatMap(payload -> {
                        if (payload.readableBytes() == 4) {
                            int val = payload.readIntLE();
                            payload.release();

                            if (!TransportException.isError(val) && transport.supportQuickAck()) { // quick acknowledge
                                if (rpcLog.isDebugEnabled()) {
                                    rpcLog.debug("[C:0x{}, Q:0x{}] Received quick ack",
                                            this.id, Integer.toHexString(val));
                                }
                                return Mono.empty();
                            }

                            // The error code writes as negative int32
                            TransportException exc = new TransportException(val);
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
                            buf.readLongLE(); // messageId
                            int payloadLength = buf.readIntLE();
                            ByteBuf payload = buf.readSlice(payloadLength);

                            try {
                                MTProtoObject obj = TlDeserializer.deserialize(payload);
                                return authHandler.handle(obj);
                            } catch (Throwable t) {
                                return Mono.error(Exceptions.propagate(t));
                            } finally {
                                buf.release();
                            }
                        }

                        AuthorizationKeyHolder authKeyHolder = this.authKey;
                        if (authKeyId != authKeyHolder.getAuthKeyId()) {
                            return Mono.error(new MTProtoException("Incorrect auth key id. Received: 0x"
                                    + Long.toHexString(authKeyId) + ", but excepted: 0x"
                                    + Long.toHexString(authKeyHolder.getAuthKeyId())));
                        }

                        // message key recheck

                        ByteBuf messageKey = buf.readRetainedSlice(16);

                        ByteBuf authKey = authKeyHolder.getAuthKey();
                        AES256IGECipher cipher = createAesCipher(messageKey, authKey, true);

                        ByteBuf decrypted = cipher.decrypt(buf.slice());

                        ByteBuf messageKeyHash = sha256Digest(authKey.slice(96, 32), decrypted);
                        ByteBuf messageKeyHashSlice = messageKeyHash.slice(8, 16);

                        if (!messageKey.equals(messageKeyHashSlice)) {
                            return Mono.error(new MTProtoException("Incorrect message key. Received: 0x"
                                    + ByteBufUtil.hexDump(messageKey) + ", but recomputed: 0x"
                                    + ByteBufUtil.hexDump(messageKeyHashSlice)));
                        }

                        messageKey.release();

                        decrypted.readLongLE();
                        long sessionId = decrypted.readLongLE();
                        long currSessionId = this.sessionId;
                        if (currSessionId != sessionId) {
                            return Mono.error(new IllegalStateException("Incorrect session identifier. Current: 0x"
                                    + Long.toHexString(currSessionId) + ", received: 0x"
                                    + Long.toHexString(sessionId)));
                        }
                        long messageId = decrypted.readLongLE();
                        decrypted.readIntLE();
                        int length = decrypted.readIntLE();
                        if (length % 4 != 0) {
                            return Mono.error(new IllegalStateException("Data isn't aligned by 4 bytes"));
                        }

                        updateTimeOffset((int) (messageId >> 32));

                        ByteBuf payload = decrypted.readSlice(length);
                        try {
                            TlObject obj = TlDeserializer.deserialize(payload);
                            // check for end of input?
                            return handleServiceMessage(obj, messageId);
                        } catch (Throwable t) {
                            return Mono.error(Exceptions.propagate(t));
                        } finally {
                            payload.release();
                        }
                    })
                    .then();

            Flux<RpcRequest> startupPayloadFlux = outbound.asFlux()
                    .filter(this::isStartupPayload);

            Flux<RpcRequest> payloadFlux = outbound.asFlux()
                    .filter(Predicate.<RpcRequest>not(this::isStartupPayload)
                            .and(this::isContentRelated))
                    .delayUntil(e -> onReady);

            Flux<RpcRequest> rpcFlux = outbound.asFlux()
                    .filter(Predicate.not(this::isContentRelated));

            Flux<ByteBuf> outboundFlux = Flux.merge(rpcFlux, startupPayloadFlux, payloadFlux)
                    .mapNotNull(serializePacket(alloc));

            Flux<ByteBuf> authFlux = authOutbound.asFlux()
                    .map(method -> {
                        int size = TlSerializer.sizeOf(method);
                        ByteBuf payload = alloc.buffer(20 + size)
                                .writeLongLE(0) // auth key id
                                .writeLongLE(getMessageId()) // Message id in the auth requests doesn't allow receiving payload
                                .writeIntLE(size);
                        TlSerializer.serialize(payload, method);

                        if (rpcLog.isDebugEnabled()) {
                            rpcLog.debug("[C:0x{}] Sending mtproto request: {}", id, prettyMethodName(method));
                        }

                        return transport.encode(payload, false);
                    });

            Mono<Void> outboundHandler = Flux.merge(authFlux, outboundFlux)
                    .flatMap(b -> FutureMono.from(connection.channel().writeAndFlush(b)))
                    .doOnDiscard(ByteBuf.class, ReferenceCountUtil::safeRelease)
                    .then();

            // Pings will send with MsgsAck and MsgsStateReq in container
            Mono<Void> ping = pingEmitter.ticks()
                    .flatMap(tick -> {
                        if (inflightPing.compareAndSet(false, true)) {
                            return sendAwait(ImmutablePingDelayDisconnect.of(System.nanoTime(), PING_TIMEOUT));
                        }
                        log.debug("[C:0x{}] Closing by ping timeout", id);
                        state.emitNext(State.DISCONNECTED, options.getEmissionHandler());
                        return Mono.empty();
                    }, 1)
                    .then();

            Mono<AuthorizationHandler> initializeAuthHandler = options.getStoreLayout().getPublicRsaKeyRegister()
                    .switchIfEmpty(Mono.defer(() -> {
                        var pubRsaKeyReg = options.publicRsaKeyRegister.get();
                        return options.getStoreLayout().updatePublicRsaKeyRegister(pubRsaKeyReg)
                                .thenReturn(pubRsaKeyReg);
                    }))
                    .map(pubRsaKeyReg -> new AuthorizationHandler(this,
                            new AuthorizationContext(pubRsaKeyReg),
                            onAuthSink, alloc))
                    .doOnNext(handler -> this.authHandler = handler);

            Mono<AuthorizationKeyHolder> startAuth = initializeAuthHandler
                    .doOnNext(auth -> state.emitNext(State.AUTHORIZATION_BEGIN, options.getEmissionHandler()))
                    .flatMap(authHandler -> authHandler.start()
                            .checkpoint("Authorization key generation")
                            .then(onAuthSink.asMono().retryWhen(authRetry(authHandler))))
                    .doOnNext(auth -> {
                        serverSalt = authHandler.getContext().getServerSalt(); // apply temporal salt
                        updateTimeOffset(authHandler.getContext().getServerTime());
                        authHandler.getContext().clear();
                        state.emitNext(State.AUTHORIZATION_END, options.getEmissionHandler());
                    })
                    .flatMap(key -> options.getStoreLayout()
                            .updateAuthorizationKey(dataCenter, key)
                            .thenReturn(key));

            Mono<Void> loadAuthKey = Mono.justOrEmpty(authKey)
                    .switchIfEmpty(options.getStoreLayout().getAuthKey(dataCenter))
                    .doOnNext(key -> onAuthSink.emitValue(key, FAIL_FAST))
                    .switchIfEmpty(startAuth)
                    .doOnNext(key -> this.authKey = key)
                    .then();

            Mono<Void> startSchedule = Mono.defer(() -> {
                log.info("[C:0x{}] Connected to datacenter.", id);
                state.emitNext(State.CONNECTED, options.getEmissionHandler());
                Duration period;
                switch (type) {
                    case MAIN:
                    case REGULAR:
                        period = PING_QUERY_PERIOD;
                        break;
                    case UPLOAD:
                    case DOWNLOAD:
                        period = PING_QUERY_PERIOD_MEDIA;
                        break;
                    default: throw new IllegalStateException();
                }
                pingEmitter.start(period);

                return ping;
            });

            Mono<Void> assignDc = this instanceof MainMTProtoClient
                    ? options.getStoreLayout().updateDataCenter(dataCenter)
                    : Mono.empty();

            Mono<Void> initialize = state.asFlux()
                    .filter(s -> s == State.CONFIGURED)
                    .next()
                    .flatMap(s -> loadAuthKey
                            .then(assignDc)
                            .then(startSchedule))
                    .then();

            return Mono.zip(inboundHandler, outboundHandler, stateHandler, initialize)
                    .doOnError(Predicate.not(BaseMTProtoClient::isRetryException), t -> {
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
        .retryWhen(options.getConnectionRetry()
                .filter(t -> !closed && isRetryException(t))
                .doAfterRetry(signal -> {
                    state.emitNext(State.RECONNECT, options.getEmissionHandler());
                    log.debug("[C:0x{}] Reconnecting to the datacenter (attempts: {})", id, signal.totalRetriesInARow());
                }))
        .then();
    }

    @Override
    public Mono<Void> connect() {
        return Mono.create(sink -> sink.onCancel(acquireConnection(sink)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(null, sink::error)));
    }

    @Override
    public Stats getStats() {
        return stats;
    }

    @Override
    public <R, T extends TlMethod<R>> Mono<R> sendAwait(T method) {
        return Mono.defer(() -> {
            if (closed) {
                return Mono.error(new MTProtoException("Client has been closed"));
            }
            if (!isResultAwait(method)) {
                outbound.emitNext(new RpcRequest(method), options.getEmissionHandler());
                return Mono.empty();
            }

            if (method.identifier() != Ping.ID && method.identifier() != PingDelayDisconnect.ID) {
                stats.incrementQueriesCount();
                stats.lastQueryTimestamp = Instant.now();
            }

            Sinks.One<R> res = Sinks.one();
            outbound.emitNext(new RpcQuery(method, res), options.getEmissionHandler());
            return res.asMono();
        })
        .transform(options.getResponseTransformers().stream()
                .map(tr -> tr.transform(method))
                .reduce(Function.identity(), Function::andThen));
    }

    @Override
    public Mono<Void> sendAuth(TlMethod<? extends MTProtoObject> method) {
        return Mono.fromRunnable(() -> {
            if (closed) {
                throw new MTProtoException("Client has been closed");
            }

            authOutbound.emitNext(method, options.getEmissionHandler());
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
        return Mono.fromSupplier(() -> connection)
                .switchIfEmpty(Mono.error(new MTProtoException("MTProto client isn't connected")))
                .doOnNext(con -> state.emitNext(State.CLOSED, options.getEmissionHandler()))
                .then();
    }

    protected void emitUpdates(Updates updates) {
        throw new UnsupportedOperationException("Received update on non-main client 0x" + id);
    }

    private boolean isStartupPayload(Request request) {
        return request.getClass() == RpcQuery.class &&
                ((RpcQuery) request).method == options.getInitConnection();
    }

    private boolean isContentRelated(RpcRequest request) {
        return isContentRelated(request.method);
    }

    private Function<RpcRequest, ByteBuf> serializePacket(ByteBufAllocator alloc) {
        return req -> {
            if (log.isTraceEnabled() && !requests.isEmpty()) {
                log.trace("[C:0x{}] {}", id, requests.entrySet().stream()
                        .map(e -> "0x" + Long.toHexString(e.getKey()) + ": " + e.getValue())
                        .collect(Collectors.toList())
                        .toString());
            }

            TlObject method = req.method;
            int size = TlSerializer.sizeOf(req.method);
            if (size >= options.getGzipWrappingSizeThreshold()) {
                ByteBuf serialized = alloc.ioBuffer(size);
                TlSerializer.serialize(serialized, method);
                ByteBuf gzipped = TlSerialUtil.compressGzip(alloc, 9, serialized);

                method = ImmutableGzipPacked.of(gzipped);
                gzipped.release();

                size = TlSerializer.sizeOf(method);
            }

            long containerMsgId = -1;
            // server returns -404 transport error when this packet placed in container
            boolean canContainerize = req.method.identifier() != InvokeWithLayer.ID && size < MAX_CONTAINER_LENGTH;

            Request containerOrRequest = req;
            long requestMessageId = getMessageId();
            int requestSeqNo = updateSeqNo(req.method);

            ByteBuf message;
            List<Long> statesIds = new ArrayList<>();
            List<ContainerMessage> messages = new ArrayList<>();
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

            if (!statesIds.isEmpty())
                messages.add(new ContainerMessage(getMessageId(), updateSeqNo(false),
                        ImmutableMsgsStateReq.of(statesIds)));
            if (!acknowledgments.isEmpty())
                messages.add(new ContainerMessage(getMessageId(), updateSeqNo(false), collectAcks()));

            canContainerize &= !messages.isEmpty();

            if (canContainerize) {
                messages.add(new ContainerMessage(requestMessageId, requestSeqNo, method, size));

                containerMsgId = getMessageId();
                int containerSeqNo = updateSeqNo(false);
                int payloadSize = messages.stream().mapToInt(c -> c.size + 16).sum();
                message = alloc.buffer(24 + payloadSize);
                message.writeLongLE(containerMsgId);
                message.writeIntLE(containerSeqNo);
                message.writeIntLE(payloadSize + 8);
                message.writeIntLE(MessageContainer.ID);
                message.writeIntLE(messages.size());

                var rpcInCont = req instanceof RpcQuery
                        ? new QueryContainerRequest((RpcQuery) req, containerMsgId)
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
                message = alloc.buffer(16 + size)
                        .writeLongLE(requestMessageId)
                        .writeIntLE(requestSeqNo)
                        .writeIntLE(size);
                TlSerializer.serialize(message, method);
            }

            int minPadding = 12;
            int unpadded = (32 + message.readableBytes() + minPadding) % 16;
            byte[] paddingb = new byte[minPadding + (unpadded != 0 ? 16 - unpadded : 0)];
            random.nextBytes(paddingb);

            ByteBuf plainData = alloc.buffer(32 + message.readableBytes() + paddingb.length)
                    .writeLongLE(serverSalt)
                    .writeLongLE(sessionId)
                    .writeBytes(message)
                    .writeBytes(paddingb);
            message.release();

            var authKeyHolder = this.authKey;
            ByteBuf authKey = authKeyHolder.getAuthKey();
            ByteBuf authKeyId = Unpooled.copyLong(Long.reverseBytes(authKeyHolder.getAuthKeyId()));

            ByteBuf messageKeyHash = sha256Digest(authKey.slice(88, 32), plainData);

            boolean quickAck = false;
            int quickAckToken = -1;
            if (!canContainerize && isContentRelated(req) && transport.supportQuickAck()) {
                quickAckToken = messageKeyHash.getIntLE(0) | QUICK_ACK_MASK;
                quickAck = true;
            }

            ByteBuf messageKey = messageKeyHash.slice(8, 16);
            AES256IGECipher cipher = createAesCipher(messageKey, authKey, false);

            ByteBuf encrypted = cipher.encrypt(plainData);
            ByteBuf packet = Unpooled.wrappedBuffer(authKeyId, messageKey, encrypted);

            if (rpcLog.isDebugEnabled()) {
                if (containerOrRequest instanceof ContainerRequest) {
                    rpcLog.debug("[C:0x{}, M:0x{}] Sending container: {{}}", id,
                            Long.toHexString(containerMsgId), messages.stream()
                                    .map(m -> "0x" + Long.toHexString(m.messageId) + ": " + prettyMethodName(m.method))
                                    .collect(Collectors.joining(", ")));
                } else {
                    if (quickAck) {
                        rpcLog.debug("[C:0x{}, M:0x{}, Q:0x{}] Sending request: {}", id,
                                Long.toHexString(requestMessageId), Integer.toHexString(quickAckToken),
                                prettyMethodName(req.method));
                    } else {
                        rpcLog.debug("[C:0x{}, M:0x{}] Sending request: {}", id,
                                Long.toHexString(requestMessageId), prettyMethodName(req.method));
                    }
                }
            }

            return transport.encode(packet, quickAck);
        };
    }

    private void updateTimeOffset(int serverTime) {
        int now = (int) (System.currentTimeMillis() / 1000);
        int updated = serverTime - now;
        if (Math.abs(timeOffset - updated) > 3) {
            lastMessageId = 0;
            timeOffset = updated;
        }
    }

    private TcpClient initTcpClient(TcpClient tcpClient) {
        return tcpClient
                .remoteAddress(() -> new InetSocketAddress(dataCenter.getAddress(), dataCenter.getPort()))
                .observe((con, st) -> {
                    if (st == ConnectionObserver.State.CONFIGURED) {
                        if (log.isDebugEnabled()) {
                            log.debug("[C:0x{}] Connected to datacenter {}", id, dataCenter);
                            log.debug("[C:0x{}] Sending transport identifier to the server", id);
                        }

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
                    authHandler.getContext().clear();
                });
    }

    private long getMessageId() {
        long millis = System.currentTimeMillis();
        long seconds = millis / 1000;
        long mod = millis % 1000;
        // [ 32 bits to approximate server time in seconds | 12 bits to fractional part of time | 20 bits of random number (divisible by 4) ]
        long messageId = seconds + timeOffset << 32 | mod << 20 | random.nextInt(0x1fffff) << 2;
        long l = lastMessageId;
        if (l >= messageId) {
            messageId = l + 4;
        }

        lastMessageId = messageId;
        return messageId;
    }

    private int updateSeqNo(TlMethod<?> object) {
        return updateSeqNo(isContentRelated(object));
    }

    private int updateSeqNo(boolean content) {
        if (content) {
            return seqNo.getAndIncrement() * 2 + 1;
        }
        return seqNo.get() * 2;
    }

    private boolean handleMsgsAck(Object obj, long messageId) {
        if (obj instanceof MsgsAck) {
            var msgsAck = (MsgsAck) obj;

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

    private Object ungzip(Object obj) {
        if (obj instanceof GzipPacked) {
            var gzipPacked = (GzipPacked) obj;
            obj = TlSerialUtil.decompressGzip(gzipPacked.packedData());
        }
        return obj;
    }

    private Mono<Void> handleServiceMessage(Object obj, long messageId) {
        if (obj instanceof RpcResult) {
            var rpcResult = (RpcResult) obj;
            messageId = rpcResult.reqMsgId();
            obj = rpcResult.result();

            obj = ungzip(obj);

            boolean needAck = !handleMsgsAck(obj, messageId);

            var req = (RpcRequest) requests.get(messageId);
            if (req == null) {
                return Mono.empty();
            }

            if (obj instanceof RpcError) {
                RpcError rpcError = (RpcError) obj;

                if (rpcLog.isDebugEnabled()) {
                    rpcLog.debug("[C:0x{}, M:0x{}] Receiving rpc error, code: {}, message: {}",
                            id, Long.toHexString(messageId), rpcError.errorCode(), rpcError.errorMessage());
                }

                obj = createRpcException(rpcError, req);
            } else {
                if (rpcLog.isDebugEnabled()) {
                    rpcLog.debug("[C:0x{}, M:0x{}] Receiving rpc result", id, Long.toHexString(messageId));
                }
            }

            stats.decrementQueriesCount();
            resolveQuery(messageId, obj);
            decContainer(req);

            if (needAck) {
                acknowledgments.add(messageId);
            }
            return Mono.empty();
        }

        if (obj instanceof MessageContainer) {
            var messageContainer = (MessageContainer) obj;
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

        // Applicable for updates
        obj = ungzip(obj);
        if (obj instanceof Updates) {
            var updates = (Updates) obj;
            if (rpcLog.isTraceEnabled()) {
                rpcLog.trace("[C:0x{}] Receiving updates: {}", id, updates);
            }

            emitUpdates(updates);
            return Mono.empty();
        }

        if (obj instanceof Pong) {
            var pong = (Pong) obj;
            messageId = pong.msgId();

            inflightPing.set(false);
            if (rpcLog.isDebugEnabled()) {
                long nanoTime = System.nanoTime();
                rpcLog.debug("[C:0x{}, M:0x{}] Receiving pong after {}", id, Long.toHexString(messageId),
                        Duration.ofNanos(nanoTime - pong.pingId()));
            }

            var req = (RpcQuery) requests.get(messageId);
            resolveQuery(messageId, pong);
            decContainer(req);
            return Mono.empty();
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
        //     return Mono.empty();
        // }

        if (obj instanceof NewSession) {
            var newSession = (NewSession) obj;
            if (rpcLog.isDebugEnabled()) {
                rpcLog.debug("[C:0x{}] Receiving new session creation, new server salt: 0x{}, first id: 0x{}",
                        id, Long.toHexString(newSession.serverSalt()), Long.toHexString(newSession.firstMsgId()));
            }

            serverSalt = newSession.serverSalt();
            lastMessageId = newSession.firstMsgId();

            acknowledgments.add(messageId);
            return Mono.empty();
        }

        // from MessageContainer
        if (handleMsgsAck(obj, messageId)) {
            return Mono.empty();
        }

        if (obj instanceof BadMsgNotification) {
            var badMsgNotification = (BadMsgNotification) obj;
            if (rpcLog.isDebugEnabled()) {
                if (badMsgNotification instanceof BadServerSalt) {
                    var badServerSalt = (BadServerSalt) badMsgNotification;
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
                    updateTimeOffset((int) (messageId >> 32));
                    break;
                case 48:
                    var badServerSalt = (BadServerSalt) badMsgNotification;
                    serverSalt = badServerSalt.newServerSalt();
                    break;
            }

            emitUnwrapped(badMsgNotification.badMsgId());
            return Mono.empty();
        }

        if (obj instanceof MsgsStateInfo) {
            var inf = (MsgsStateInfo) obj;

            var req = (RpcRequest) requests.remove(inf.reqMsgId());
            if (req != null) {
                MsgsStateReq original = (MsgsStateReq) req.method;
                ByteBuf c = inf.info();
                if (original.msgIds().size() != c.readableBytes()) {
                    rpcLog.error("[C:0x{}, M:0x{}] Received not all states. expected: {}, received: {}",
                            id, Long.toHexString(inf.reqMsgId()), original.msgIds().size(),
                            c.readableBytes());

                    return Mono.empty();
                }

                if (rpcLog.isDebugEnabled()) {
                    StringJoiner st = new StringJoiner(", ");
                    int i = 0;
                    for (long msgId : original.msgIds()) {
                        st.add("0x" + Long.toHexString(msgId) + "/" + (c.getByte(i++) & 7));
                    }

                    rpcLog.debug("[C:0x{}, M:0x{}] Received states: [{}]", id, Long.toHexString(inf.reqMsgId()), st);
                }

                decContainer(req);
                var msgIds = original.msgIds();
                for (int i = 0; i < msgIds.size(); i++) {
                    long msgId = msgIds.get(i);

                    int state = c.getByte(i) & 7;
                    switch (state) {
                        case 1:
                        case 2:
                        case 3: // not received, resend
                            emitUnwrapped(msgId);
                            break;
                        case 4: // acknowledged
                            var sub = (RpcRequest) requests.get(msgId);
                            if (sub == null) {
                                continue;
                            }

                            if (!isResultAwait(sub.method)) {
                                requests.remove(msgId);
                                decContainer(sub);
                            }
                            break;
                        default:
                            log.debug("[C:0x{}] Unknown state {}", id, state);
                    }
                }
            }
            return Mono.empty();
        }

        if (obj instanceof MsgDetailedInfo) {
            var info = (MsgDetailedInfo) obj;

            if (info instanceof BaseMsgDetailedInfo) {
                BaseMsgDetailedInfo base = (BaseMsgDetailedInfo) info;
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
            return Mono.empty();
        }

        if (obj instanceof DestroySessionRes) {
            var res = (DestroySessionRes) obj;

            // Why DestroySession have concrete type of response, but also have a wrong message_id
            // which can't be used as key of the requests map?

            if (rpcLog.isDebugEnabled()) {
                rpcLog.debug("[C:0x{}] Session 0x{} destroyed {}",
                        id, Long.toHexString(res.sessionId()),
                        res.identifier() == DestroySessionOk.ID ? "successfully" : "with nothing");
            }

            return Mono.empty();
        }

        log.warn("[C:0x{}] Unhandled payload: {}", id, obj);
        return Mono.empty();
    }

    private void emitUnwrappedContainer(ContainerRequest container) {
        for (long msgId : container.msgIds) {
            var inner = (RpcRequest) requests.remove(msgId);
            // If inner is null this mean response for mean was received
            if (inner != null) {
                // This method was called from MessageStateInfo handling;
                // Failed to send acks, just give back to queue
                if (inner.method instanceof MsgsAck) {
                    var acks = (MsgsAck) inner.method;
                    acknowledgments.addAll(acks.msgIds());
                // There is no need to resend this requests,
                // because it computed on relevant 'requests' map
                } else if (inner.method.identifier() == MsgsStateReq.ID) {
                    continue;
                } else {
                    RpcRequest single;
                    if (inner instanceof QueryContainerRequest) {
                        var query = (QueryContainerRequest) inner;
                        single = new RpcQuery(query.method, query.sink);
                    } else {
                        single = new RpcRequest(inner.method);
                    }

                    outbound.emitNext(single, options.getEmissionHandler());
                }
            }
        }
    }

    private void emitUnwrapped(long possibleCntMsgId) {
        Request request = requests.remove(possibleCntMsgId);
        if (request instanceof ContainerRequest) {
            var container = (ContainerRequest) request;
            emitUnwrappedContainer(container);
        } else if (request instanceof ContainerizedRequest) {
            var cntMessage = (ContainerizedRequest) request;
            var cnt = (ContainerRequest) requests.remove(cntMessage.containerMsgId());
            if (cnt != null) {
                emitUnwrappedContainer(cnt);
            }
        } else if (request instanceof RpcRequest) {
            outbound.emitNext((RpcRequest) request, options.getEmissionHandler());
        }
    }

    private void decContainer(RpcRequest req) {
        if (req instanceof ContainerizedRequest) {
            var aux = (ContainerizedRequest) req;
            var cnt = (ContainerRequest) requests.get(aux.containerMsgId());
            if (cnt != null && cnt.decrementCnt()) {
                requests.remove(aux.containerMsgId());
            }
        }
    }

    private MsgsAck collectAcks() {
        var acks = MsgsAck.builder()
                .msgIds(List.of());
        int cnt = 0;
        Long id;
        while ((id = acknowledgments.poll()) != null && ++cnt <= MAX_IDS_SIZE)
            acks.addMsgId(id);

        return acks.build();
    }

    @SuppressWarnings("unchecked")
    private void resolveQuery(long messageId, Object value) {
        requests.computeIfPresent(messageId, (k, v) -> {
            var query = (RpcQuery) v;
            var sink = (Sinks.One<Object>) query.sink;
            if (value instanceof Throwable) {
                Throwable value0 = (Throwable) value;
                sink.emitError(value0, FAIL_FAST);
            } else {
                sink.emitValue(value, FAIL_FAST);
            }
            return null;
        });
    }

    static boolean isRetryException(Throwable t) {
        return t == RETRY || t instanceof AbortedException || t instanceof IOException;
    }

    static boolean isContentRelated(TlMethod<?> object) {
        switch (object.identifier()) {
            case MsgsAck.ID:
            case Ping.ID:
            case PingDelayDisconnect.ID:
            case MessageContainer.ID:
            case MsgsStateReq.ID:
            case MsgResendReq.ID:
                return false;
            default:
                return true;
        }
    }

    static boolean isResultAwait(TlMethod<?> object) {
        switch (object.identifier()) {
            case MsgsAck.ID:
            case DestroySession.ID:
            // for this message MsgsStateInfo is response
            // case MsgsStateReq.ID:
                return false;
            default:
                return true;
        }
    }

    // name in format: 'users.getFullUser'
    static String prettyMethodName(TlObject method) {
        String name = method.getClass().getSimpleName();
        if (name.startsWith("Immutable"))
            name = name.substring(9);

        String namespace = method.getClass().getPackageName();
        if (namespace.startsWith("telegram4j.tl."))
            namespace = namespace.substring(14);
        if (namespace.startsWith("request"))
            namespace = namespace.substring(7);
        if (namespace.startsWith("."))
            namespace = namespace.substring(1);
        name = Character.toLowerCase(name.charAt(0)) + name.substring(1);

        if (namespace.isEmpty()) {
            return name;
        }
        return namespace + '.' + name;
    }

    static RpcException createRpcException(RpcError error, RpcRequest request) {
        String format = String.format("%s returned code: %d, message: %s",
                prettyMethodName(request.method), error.errorCode(),
                error.errorMessage());

        return new RpcException(format, error);
    }

    static class RpcRequest implements Request {
        final TlMethod<?> method;

        RpcRequest(TlMethod<?> method) {
            this.method = method;
        }

        @Override
        public String toString() {
            return "RpcRequest{" + prettyMethodName(method) + '}';
        }
    }

    static class RpcQuery extends RpcRequest {
        final Sinks.One<?> sink;

        RpcQuery(TlMethod<?> method, Sinks.One<?> sink) {
            super(method);
            this.sink = sink;
        }

        @Override
        public String toString() {
            return "RpcQuery{" + prettyMethodName(method) + '}';
        }
    }

    interface Request {}

    static class ContainerRequest implements Request {
        static final VarHandle CNT;

        static {
            try {
                var l = MethodHandles.lookup();
                CNT = l.findVarHandle(ContainerRequest.class, "cnt", short.class);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        final long[] msgIds;
        // The counter of messages for which response has not received yet
        volatile short cnt;

        ContainerRequest(long[] msgIds) {
            this.msgIds = msgIds;
            this.cnt = (short) msgIds.length;
        }

        boolean decrementCnt() {
            int cnt = (int)CNT.getAndAdd(this, (short)-1) - 1;
            return cnt <= 0;
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

    interface ContainerizedRequest extends Request {

        long containerMsgId();

        TlMethod<?> method();
    }

    static class RpcContainerRequest extends RpcRequest implements ContainerizedRequest {

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
                    ", method=" + prettyMethodName(method) +
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

    static class QueryContainerRequest extends RpcQuery implements ContainerizedRequest {
        final long containerMsgId;

        QueryContainerRequest(RpcQuery query, long containerMsgId) {
            super(query.method, query.sink);
            this.containerMsgId = containerMsgId;
        }

        @Override
        public String toString() {
            return "QueryContainerRequest{" +
                    "containerMsgId=0x" + Long.toHexString(containerMsgId) +
                    ", method=" + prettyMethodName(method) +
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

    static class RetryConnectException extends RuntimeException {

        RetryConnectException() {
            super(null, null, false, false);
        }
    }

    static class ContainerMessage {
        final long messageId;
        final int seqNo;
        final int size;
        final TlObject method;

        ContainerMessage(long messageId, int seqNo, TlObject method, int size) {
            this.messageId = messageId;
            this.seqNo = seqNo;
            this.method = method;
            this.size = size;
        }

        ContainerMessage(long messageId, int seqNo, TlObject method) {
            this(messageId, seqNo, method, TlSerializer.sizeOf(method));
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
        public Optional<Instant> getLastQueryTimestamp() {
            return Optional.ofNullable(lastQueryTimestamp);
        }

        @Override
        public int getQueriesCount() {
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
}
