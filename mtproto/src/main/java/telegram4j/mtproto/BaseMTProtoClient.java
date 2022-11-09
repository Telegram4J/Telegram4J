package telegram4j.mtproto;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;
import static telegram4j.mtproto.BaseMTProtoClient.BaseRequest.*;
import static telegram4j.mtproto.transport.Transport.QUICK_ACK_MASK;
import static telegram4j.mtproto.util.CryptoUtil.*;

class BaseMTProtoClient implements MTProtoClient {
    private static final Logger log = Loggers.getLogger("telegram4j.mtproto.MTProtoClient");
    private static final Logger rpcLog = Loggers.getLogger("telegram4j.mtproto.rpc");

    private static final int MAX_MISSED_PONG = 1;
    private static final Throwable RETRY = new RetryConnectException();
    private static final Duration PING_QUERY_PERIOD = Duration.ofSeconds(5);
    private static final Duration PING_QUERY_PERIOD_MEDIA = PING_QUERY_PERIOD.multipliedBy(2);
    private static final int PING_TIMEOUT = (int) PING_QUERY_PERIOD.multipliedBy(2).getSeconds();

    // limit for service container like a MsgsAck, MsgsStateReq
    private static final int MAX_IDS_SIZE = 8192;
    private static final int MAX_CONTAINER_SIZE = 1020; // count of messages
    private static final int MAX_CONTAINER_LENGTH = 1 << 15; // length in bytes

    private final TcpClient tcpClient;
    private final Transport transport;

    private final AuthorizationContext authContext = new AuthorizationContext();
    private final Sinks.Many<Request> outbound;
    private final Sinks.Many<TlMethod<?>> authOutbound;
    private final ResettableInterval pingEmitter;
    private final Sinks.Many<State> state;

    private final String id = Integer.toHexString(hashCode());
    private final DataCenter dataCenter;

    protected final MTProtoOptions options;

    private volatile long lastPing; // nanotime of sent Ping
    private volatile long lastPong; // nanotime of received Pong
    private final AtomicInteger missedPong = new AtomicInteger();

    private final long sessionId = random.nextLong();
    private volatile Sinks.Empty<Void> closeHook;
    private volatile Connection connection;
    private volatile boolean closed;

    protected volatile AuthorizationKeyHolder authKey;
    protected volatile int timeOffset;
    protected volatile long serverSalt;
    protected volatile long lastMessageId;

    private final AtomicInteger seqNo = new AtomicInteger();
    private final Queue<Long> acknowledgments = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Long, Request> requests = new ConcurrentHashMap<>();
    private final Cache<Integer, Long> quickAckTokens;

    public BaseMTProtoClient(MTProtoOptions options) {
        this(options.getDatacenter(), options);
    }

    BaseMTProtoClient(DataCenter dataCenter, MTProtoOptions options) {
        this.dataCenter = dataCenter;
        this.tcpClient = initTcpClient(options.getTcpClient());
        this.transport = options.getTransport().get();
        this.options = options;

        this.outbound = Sinks.many().multicast()
                .onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);
        this.authOutbound = Sinks.many().multicast()
                .onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);
        this.state = Sinks.many().replay()
                .latestOrDefault(State.RECONNECT);
        this.pingEmitter = new ResettableInterval(Schedulers.parallel());
        var cacheBuilder = Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.MINUTES);
        // And yes, it will be triggered if the message was acked via MsgsAck.
        if (rpcLog.isDebugEnabled()) {
            cacheBuilder.<Integer, Long>evictionListener((key, value, cause) -> {
                if (cause == RemovalCause.EXPIRED) {
                    Objects.requireNonNull(value);
                    rpcLog.debug("[C:0x{}] Evicted quick acknowledge for 0x{}", this.id, Long.toHexString(value));
                }
            });
        }

        this.quickAckTokens = cacheBuilder.build();
    }

    @Override
    public Mono<Void> connect() {
        return tcpClient
                .remoteAddress(() -> new InetSocketAddress(dataCenter.getAddress(), dataCenter.getPort()))
                .connect()
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
                                        pingEmitter.dispose();
                                        closeHook.emitEmpty(FAIL_FAST);

                                        if (log.isDebugEnabled()) {
                                            log.debug("[C:0x{}] Disconnected from the datacenter {}", id, dataCenter);
                                        }

                                        return Mono.fromRunnable(connection::dispose)
                                                .onErrorResume(t -> Mono.empty());
                                    case DISCONNECTED:
                                        pingEmitter.dispose();

                                        return Mono.fromRunnable(connection::dispose)
                                                .onErrorResume(t -> Mono.empty())
                                                .then(Mono.error(RETRY));
                                    case CONNECTED:
                                        return sendAwait(options.getInitConnection())
                                                .doOnNext(result -> this.state.emitNext(State.READY, options.getEmissionHandler()));
                                    case CONFIGURED: // if not reset there is a chance that the ping interval will not work after reconnect
                                        lastPong = 0;
                                        lastPing = 0;
                                        missedPong.set(0);
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
                                        Long msgId = quickAckTokens.getIfPresent(val);
                                        if (msgId == null) {
                                            rpcLog.debug("[C:0x{}] Unserialized quick acknowledge", this.id);
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
                                    return Mono.error(new MTProtoException("Incorrect message key. Received: 0x"
                                            + ByteBufUtil.hexDump(messageKey) + ", but recomputed: 0x"
                                            + ByteBufUtil.hexDump(messageKeyHashSlice)));
                                }

                                messageKey.release();

                                decrypted.readLongLE();
                                long sessionId = decrypted.readLongLE();
                                if (this.sessionId != sessionId) {
                                    return Mono.error(new IllegalStateException("Incorrect session identifier. Current: 0x"
                                            + Long.toHexString(this.sessionId) + ", received: 0x"
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

                    Flux<Request> startupPayloadFlux = outbound.asFlux()
                            .filter(this::isStartupPayload);

                    Flux<Request> payloadFlux = outbound.asFlux()
                            .filter(this::isContentRelated)
                            .filter(Predicate.not(this::isStartupPayload))
                            .delayUntil(e -> onReady);

                    Flux<Request> rpcFlux = outbound.asFlux()
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

                    // Pings will send with MsgsAck in and MsgsStateReq container
                    Mono<Void> ping = pingEmitter.ticks()
                            .flatMap(tick -> {
                                if (lastPing - lastPong > 0) {
                                    int missed = missedPong.incrementAndGet();
                                    if (missed >= MAX_MISSED_PONG) {
                                        lastMessageId = 0; // to break connection
                                        if (missed > MAX_MISSED_PONG) {
                                            return Mono.empty();
                                        }
                                    }
                                }

                                lastPing = System.nanoTime();
                                return sendAwait(ImmutablePingDelayDisconnect.of(random.nextLong(), PING_TIMEOUT));
                            })
                            .then();

                    Mono<AuthorizationKeyHolder> startAuth = Mono.fromRunnable(() ->
                                    state.emitNext(State.AUTHORIZATION_BEGIN, options.getEmissionHandler()))
                            .then(authHandler.start())
                            .checkpoint("Authorization key generation")
                            .then(onAuthSink.asMono().retryWhen(authRetry(authHandler)))
                            .doOnNext(auth -> {
                                serverSalt = authContext.getServerSalt(); // apply temporal salt
                                updateTimeOffset(authContext.getTimeOffset());
                                authContext.clear();
                                state.emitNext(State.AUTHORIZATION_END, options.getEmissionHandler());
                            })
                            .flatMap(key -> options.getStoreLayout()
                                    .updateAuthorizationKey(dataCenter, key).thenReturn(key));

                    Mono<Void> awaitKey = Mono.justOrEmpty(authKey)
                            .switchIfEmpty(options.getStoreLayout().getAuthorizationKey(dataCenter))
                            .doOnNext(key -> onAuthSink.emitValue(key, FAIL_FAST))
                            .switchIfEmpty(startAuth)
                            .doOnNext(key -> this.authKey = key)
                            .then();

                    Mono<Void> updateStoreDataCenter = options.storeLayout.getDataCenter()
                            .switchIfEmpty(options.storeLayout.updateDataCenter(dataCenter).then(Mono.empty()))
                            .then();

                    Mono<Void> startSchedule = Mono.defer(() -> {
                        log.info("[C:0x{}] Connected to datacenter.", id);
                        state.emitNext(State.CONNECTED, options.getEmissionHandler());
                        pingEmitter.start(dataCenter.getType() == DataCenter.Type.MEDIA
                                ? PING_QUERY_PERIOD_MEDIA : PING_QUERY_PERIOD);

                        return ping;
                    });

                    Mono<Void> initialize = state.asFlux()
                            .filter(s -> s == State.CONFIGURED)
                            .next()
                            .flatMap(s -> updateStoreDataCenter
                                    .then(awaitKey)
                                    .then(startSchedule))
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
                .retryWhen(options.getConnectionRetry()
                        .filter(t -> !closed && isRetryException(t))
                        .doAfterRetry(signal -> {
                            state.emitNext(State.RECONNECT, options.getEmissionHandler());
                            log.debug("[C:0x{}] Reconnecting to the datacenter (attempts: {})", id, signal.totalRetriesInARow());
                        }))
                .then(Mono.defer(() -> closeHook.asMono()));
    }

    @Override
    public SessionInfo getSessionInfo() {
        return new SessionInfo(timeOffset, sessionId, seqNo.get(), serverSalt, lastMessageId);
    }

    @Override
    public <R, T extends TlMethod<R>> Mono<R> sendAwait(T method) {
        return Mono.defer(() -> {
            if (method instanceof RpcMethod) {
                outbound.emitNext(new RpcRequest(method), options.getEmissionHandler());
                return Mono.empty();
            }

            Sinks.One<R> res = Sinks.one();
            outbound.emitNext(new RpcQuery(method, res), options.getEmissionHandler());

            return res.asMono()
                    .transform(options.getResponseTransformers().stream()
                            .map(tr -> tr.transform(method))
                            .reduce(Function.identity(), Function::andThen));
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
    public Mono<Void> close() {
        return Mono.fromSupplier(() -> connection)
                .switchIfEmpty(Mono.error(new IllegalStateException("MTProto client isn't connected")))
                .doOnNext(con -> state.emitNext(State.CLOSED, options.getEmissionHandler()))
                .then();
    }

    protected void emitUpdates(Updates updates) {
        throw new UnsupportedOperationException();
    }

    private boolean isStartupPayload(Request request) {
        return request.getClass() == RpcQuery.class &&
                ((RpcQuery) request).method == options.getInitConnection();
    }

    private boolean isContentRelated(Request request) {
        if (request instanceof ContainerRequest) {
            var container = (ContainerRequest) request;
            return container.hasContentRelated;
        } else if (request instanceof RpcRequest) {
            var rpcRequest = (RpcRequest) request;
            return isContentRelated(rpcRequest.method);
        } else {
            throw new IllegalStateException();
        }
    }

    private Function<Request, ByteBuf> serializePacket(ByteBufAllocator alloc) {
        return req -> {
            if (log.isTraceEnabled() && !requests.isEmpty()) {
                log.trace(requests.entrySet().stream()
                        .map(e -> "0x" + Long.toHexString(e.getKey()) + ": " + e.getValue())
                        .collect(Collectors.toList())
                        .toString());
            }

            long containerMsgId = -1;
            int requestSize = -1;
            long requestMessageId = -1;
            // server returns -404 transport error when this packet placed in container
            RpcRequest rpcRequest = null;
            RpcQuery rpcQuery = null;
            boolean canContainerize = req instanceof RpcRequest &&
                    (rpcRequest = (RpcRequest) req).method.identifier() != InvokeWithLayer.ID &&
                    (requestSize = TlSerializer.sizeOf(rpcRequest.method)) < MAX_CONTAINER_LENGTH;
            ByteBuf message;
            var messages = List.<ContainerMessage>of();

            // TODO: destroy container and resend query
            if (req instanceof ContainerRequest) {
                var container = (ContainerRequest) req;

                messages = new ArrayList<>(container.msgIds.length);
                for (var msgId : container.msgIds) {
                    var old = requests.remove(msgId);
                    if (old instanceof RpcQuery) {
                        rpcQuery = (RpcQuery) old;
                        requestMessageId = getMessageId();
                    } else if (old instanceof RpcRequest) {
                        var request = (RpcRequest) old;
                        messages.add(new ContainerMessage(getMessageId(), updateSeqNo(request.method), request.method));
                    }
                }

                // we rechecked the container, and it turned out that all messages were processed
                if (messages.isEmpty()) {
                    return null;
                }
                Objects.requireNonNull(rpcQuery);
                containerMsgId = getMessageId();
                requests.put(requestMessageId, new QueryContainerRequest(rpcQuery, containerMsgId));

                long rmsgId0 = requestMessageId;
                messages.sort(Comparator.<ContainerMessage, Boolean>comparing(c -> c.messageId == rmsgId0)
                        .thenComparing(c -> isContentRelated(c.method))
                        .reversed());

                int containerSeqNo = updateSeqNo(false);
                int payloadSize = messages.stream().mapToInt(c -> c.size + 16).sum();
                message = alloc.buffer(24 + payloadSize);
                message.writeLongLE(containerMsgId);
                message.writeIntLE(containerSeqNo);
                message.writeIntLE(payloadSize + 8);
                message.writeIntLE(MessageContainer.ID);
                message.writeIntLE(messages.size());

                var msgIds = new long[messages.size()];
                for (int i = 0; i < messages.size(); i++) {
                    var c = messages.get(i);
                    msgIds[i] = c.messageId;
                    if (c.messageId != requestMessageId) {
                        requests.put(c.messageId, new RpcContainerRequest(c.method, containerMsgId));
                    }

                    message.writeLongLE(c.messageId);
                    message.writeIntLE(c.seqNo);
                    message.writeIntLE(c.size);
                    TlSerializer.serialize(message, c.method);
                }

                req = new ContainerRequest(msgIds, isContentRelated(req));
                requests.put(containerMsgId, req);
            } else if (req instanceof RpcRequest) {
                requestMessageId = getMessageId();
                int requestSeqNo = updateSeqNo(rpcRequest.method);

                List<Long> statesIds = new ArrayList<>();
                if (canContainerize) {
                    for (var e : requests.entrySet()) {
                        long key = e.getKey();
                        var inf = e.getValue();
                        if (inf instanceof ContainerRequest)
                            continue;

                        int st = inf.state();
                        if ((st & SENT) == 1) {
                            statesIds.add(key);
                            if (statesIds.size() >= MAX_IDS_SIZE) {
                                break;
                            }
                        }
                    }
                }

                messages = new ArrayList<>();

                if (!statesIds.isEmpty())
                    messages.add(new ContainerMessage(getMessageId(), updateSeqNo(false),
                            ImmutableMsgsStateReq.of(statesIds)));
                if (!acknowledgments.isEmpty())
                    messages.add(new ContainerMessage(getMessageId(), updateSeqNo(false), collectAcks()));

                canContainerize &= !messages.isEmpty();

                if (canContainerize) {
                    messages.add(new ContainerMessage(requestMessageId, requestSeqNo, rpcRequest.method, requestSize));
                    containerMsgId = getMessageId();

                    int containerSeqNo = updateSeqNo(false);

                    int payloadSize = messages.stream().mapToInt(c -> c.size + 16).sum();
                    message = alloc.buffer(24 + payloadSize);
                    message.writeLongLE(containerMsgId);
                    message.writeIntLE(containerSeqNo);
                    message.writeIntLE(payloadSize + 8);
                    message.writeIntLE(MessageContainer.ID);
                    message.writeIntLE(messages.size());

                    long rmsgId0 = requestMessageId;
                    messages.sort(Comparator.<ContainerMessage, Boolean>comparing(c -> c.messageId == rmsgId0)
                            .thenComparing(c -> isContentRelated(c.method))
                            .reversed());

                    var rpcInCont = req instanceof RpcQuery
                            ? new QueryContainerRequest((RpcQuery) req, containerMsgId)
                            : new RpcContainerRequest((RpcRequest) req, containerMsgId);
                    requests.put(requestMessageId, rpcInCont);
                    var msgIds = new long[messages.size()];
                    for (int i = 0; i < messages.size(); i++) {
                        var c = messages.get(i);
                        msgIds[i] = c.messageId;
                        if (c.messageId != requestMessageId) {
                            requests.put(c.messageId, new RpcContainerRequest(c.method, containerMsgId));
                        }

                        message.writeLongLE(c.messageId);
                        message.writeIntLE(c.seqNo);
                        message.writeIntLE(c.size);
                        TlSerializer.serialize(message, c.method);
                    }

                    req = new ContainerRequest(msgIds, isContentRelated(req));
                    requests.put(containerMsgId, req);
                } else {
                    requests.put(requestMessageId, req);
                    if (requestSize == -1)
                        requestSize = TlSerializer.sizeOf(rpcRequest.method);
                    message = alloc.buffer(16 + requestSize)
                            .writeLongLE(requestMessageId)
                            .writeIntLE(requestSeqNo)
                            .writeIntLE(requestSize);
                    TlSerializer.serialize(message, rpcRequest.method);
                }
            } else {
                throw new IllegalStateException();
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
            ByteBuf authKeyId = authKeyHolder.getAuthKeyId();

            ByteBuf messageKeyHash = sha256Digest(authKey.slice(88, 32), plainData);

            boolean quickAck = false;
            if (!canContainerize && isContentRelated(req) && transport.supportQuickAck()) {
                int quickAckToken = messageKeyHash.getIntLE(0) | QUICK_ACK_MASK;
                quickAckTokens.put(quickAckToken, requestMessageId);
                quickAck = true;
            }

            ByteBuf messageKey = messageKeyHash.slice(8, 16);
            AES256IGECipher cipher = createAesCipher(messageKey, authKey, false);

            ByteBuf encrypted = cipher.encrypt(plainData);
            ByteBuf packet = Unpooled.wrappedBuffer(authKeyId.retain(), messageKey, encrypted);

            if (rpcLog.isDebugEnabled()) {
                if (req instanceof ContainerRequest) {
                    rpcLog.debug("[C:0x{}, M:0x{}] Sending container: {{}}", id,
                            Long.toHexString(containerMsgId), messages.stream()
                                    .map(m -> "0x" + Long.toHexString(m.messageId) + ": " + prettyMethodName(m.method))
                                    .collect(Collectors.joining(", ")));
                } else {
                    rpcLog.debug("[C:0x{}, M:0x{}] Sending request: {}", id,
                            Long.toHexString(requestMessageId), prettyMethodName(rpcRequest.method));
                }
            }

            req.setState(SENT);

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
                    authContext.clear();
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
        int no = seqNo.get() * 2 + (content ? 1 : 0);
        if (content) {
            seqNo.incrementAndGet();
        }

        return no;
    }

    private boolean handleMsgsAck(Object obj, long messageId) {
        if (obj instanceof MsgsAck) {
            MsgsAck msgsAck = (MsgsAck) obj;

            if (rpcLog.isDebugEnabled()) {
                rpcLog.debug("[C:0x{}, M:0x{}] Received acknowledge for message(s): [{}]",
                        id, Long.toHexString(messageId), msgsAck.msgIds().stream()
                                .map(l -> String.format("0x%x", l))
                                .collect(Collectors.joining(", ")));
            }

            for (long msgId : msgsAck.msgIds()) {
                var req = requests.get(msgId);
                if (req instanceof ContainerRequest) {
                    var container = (ContainerRequest) req;
                    // I think containers will not receive notifications if it's acknowledged
                    requests.remove(msgId);
                    for (var cmsgId : container.msgIds) {
                        var msg = requests.get(cmsgId);
                        if (msg != null) {
                            msg.markState(s -> s | ACKNOWLEDGED);
                        }
                    }
                } else if (req != null) {
                    req.markState(s -> s | ACKNOWLEDGED);
                }
            }
            return true;
        }
        return false;
    }

    private Object ungzip(Object obj) {
        if (obj instanceof GzipPacked) {
            GzipPacked gzipPacked = (GzipPacked) obj;
            obj = TlSerialUtil.decompressGzip(gzipPacked.packedData());
        }
        return obj;
    }

    private Mono<Void> handleServiceMessage(Object obj, long messageId) {
        if (obj instanceof RpcResult) {
            RpcResult rpcResult = (RpcResult) obj;
            messageId = rpcResult.reqMsgId();
            obj = rpcResult.result();

            obj = ungzip(obj);

            handleMsgsAck(obj, messageId);

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

            resolveQuery(messageId, obj);
            decContainer(req);

            if ((req.state & ACKNOWLEDGED) == 0) {
                acknowledgments.add(messageId);
            }
            return Mono.empty();
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

        // Applicable for updates
        obj = ungzip(obj);
        if (obj instanceof Updates) {
            Updates updates = (Updates) obj;
            if (rpcLog.isTraceEnabled()) {
                rpcLog.trace("[C:0x{}] Receiving updates: {}", id, updates);
            }

            emitUpdates(updates);
            return Mono.empty();
        }

        if (obj instanceof Pong) {
            Pong pong = (Pong) obj;
            messageId = pong.msgId();

            long nanoTime = System.nanoTime();
            lastPong = nanoTime;
            missedPong.set(0);

            if (rpcLog.isDebugEnabled()) {
                rpcLog.debug("[C:0x{}, M:0x{}] Receiving pong after {}", id, Long.toHexString(messageId),
                        Duration.ofNanos(nanoTime - lastPing));
            }

            decContainer((RpcRequest) requests.remove(messageId));
            return Mono.empty();
        }

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

            requests.forEach((k, v) -> {
                if (k > newSession.firstMsgId()) {
                    // resend will process on msgsStateReq request
                    v.setState(SENT);
                }
            });

            // updates.emitNext(UpdatesTooLong.instance(), options.getEmissionHandler());

            acknowledgments.add(messageId);
            return Mono.empty();
        }

        // from MessageContainer
        if (handleMsgsAck(obj, messageId)) {
            return Mono.empty();
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
                    updateTimeOffset((int) (messageId >> 32));
                    break;
                case 48:
                    var badServerSalt = (BadServerSalt) badMsgNotification;
                    serverSalt = badServerSalt.newServerSalt();
                    break;
            }

            var request = requests.remove(badMsgNotification.badMsgId());
            if (request != null) {
                request.setState(PENDING);
                outbound.emitNext(request, options.getEmissionHandler());
            }

            return Mono.empty();
        }

        if (obj instanceof MsgsStateInfo) {
            MsgsStateInfo inf = (MsgsStateInfo) obj;

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
                    var sub = (RpcRequest) requests.get(msgId);
                    if (sub == null) {
                        continue;
                    }

                    int state = c.getByte(i) & 7;
                    switch (state) {
                        case 1:
                        case 2:
                        case 3: // not received, resend
                            requests.remove(msgId);
                            decContainer(sub);

                            // necessary to reset state and unwrap container messages
                            Request copy;
                            if (sub instanceof RpcQuery) {
                                var query = (RpcQuery) sub;
                                copy = new RpcQuery(query.method, query.sink);
                            } else {
                                copy = new RpcRequest(sub.method);
                            }

                            outbound.emitNext(copy, options.getEmissionHandler());
                            break;
                        case 4:
                            sub.markState(s -> s | ACKNOWLEDGED);
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
            MsgDetailedInfo info = (MsgDetailedInfo) obj;

            if (info instanceof BaseMsgDetailedInfo) {
                BaseMsgDetailedInfo base = (BaseMsgDetailedInfo) info;
                if (rpcLog.isDebugEnabled()) {
                    rpcLog.debug("[C:0x{}] Handling message info. msgId: 0x{}, answerId: 0x{}", id,
                            Long.toHexString(base.msgId()), Long.toHexString(base.answerMsgId()));
                }

                var req = requests.get(base.msgId());
                if (req != null) {
                    req.markState(s -> s | ACKNOWLEDGED);
                }
            } else {
                if (rpcLog.isDebugEnabled()) {
                    rpcLog.debug("[C:0x{}] Handling message info. answerId: 0x{}", id, Long.toHexString(info.answerMsgId()));
                }
            }

            // TODO
            return Mono.empty();
        }

        log.warn("[C:0x{}] Unhandled payload: {}", id, obj);
        return Mono.empty();
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
                // case Ping.ID:
                // case PingDelayDisconnect.ID:
            case MessageContainer.ID:
                // case MsgsStateReq.ID:
                // case MsgResendReq.ID:
                return false;
            default:
                return true;
        }
    }

    // name in format: 'users.getFullUser'
    static String prettyMethodName(TlMethod<?> method) {
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

    static class RpcRequest extends BaseRequest {
        final TlMethod<?> method;

        RpcRequest(TlMethod<?> method) {
            this.method = method;
        }

        @Override
        public String toString() {
            return "RpcRequest{" +
                    "method=" + prettyMethodName(method) +
                    ", state=" + state +
                    '}';
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
            return "RpcQuery{" +
                    "method=" + prettyMethodName(method) +
                    ", state=" + state +
                    '}';
        }
    }

    interface Request {

        int state();

        void markState(IntUnaryOperator updateFunction);

        void setState(int state);
    }

    static class BaseRequest implements Request {
        static final VarHandle STATE;

        static {
            try {
                var l = MethodHandles.lookup();
                STATE = l.findVarHandle(BaseRequest.class, "state", int.class);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        static final int PENDING        = 0b0000;
        static final int SENT           = 0b0001;
        static final int ACKNOWLEDGED   = 0b0010;

        volatile int state = PENDING;

        @Override
        public String toString() {
            return "Request{" +
                    "state=" + state +
                    '}';
        }

        @Override
        public void markState(IntUnaryOperator updateFunction) {
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

        @Override
        public int state() {
            return state;
        }

        @Override
        public void setState(int state) {
            this.state = state;
        }
    }

    static class ContainerRequest extends BaseRequest {
        static final VarHandle CNT;

        static {
            try {
                var l = MethodHandles.lookup();
                CNT = l.findVarHandle(ContainerRequest.class, "cnt", short.class);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        // array of message's messageId;
        // first value always is the query message
        // sorted so that first there are those messages
        // that need to be answered, and then there are other service messages
        final long[] msgIds;
        final boolean hasContentRelated;
        // The counter of messages for which response has not received yet
        volatile short cnt;

        ContainerRequest(long[] msgIds, boolean hasContentRelated) {
            this.msgIds = msgIds;
            this.cnt = (short) msgIds.length;
            this.hasContentRelated = hasContentRelated;
        }

        boolean decrementCnt() {
            int cnt = (short)CNT.getAndAdd(this, (short)-1) - 1;
            return cnt == 0;
        }

        @Override
        public String toString() {
            return "ContainerRequest{" +
                    "msgIds=" + Arrays.stream(msgIds)
                    .mapToObj(s -> "0x" + Long.toHexString(s))
                    .collect(Collectors.joining(", ", "[", "]")) +
                    ", state=" + state +
                    ", cnt=" + cnt +
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
            this.state = SENT;
        }

        RpcContainerRequest(RpcRequest request, long containerMsgId) {
            this(request.method, containerMsgId);
        }

        @Override
        public String toString() {
            return "RpcContainerRequest{" +
                    "containerMsgId=0x" + Long.toHexString(containerMsgId) +
                    ", state=" + state +
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
            this.state = SENT;
        }

        @Override
        public String toString() {
            return "QueryContainerRequest{" +
                    "containerMsgId=0x" + Long.toHexString(containerMsgId) +
                    ", state=" + state +
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
        final TlMethod<?> method;

        ContainerMessage(long messageId, int seqNo, TlMethod<?> method, int size) {
            this.messageId = messageId;
            this.seqNo = seqNo;
            this.method = method;
            this.size = size;
        }

        ContainerMessage(long messageId, int seqNo, TlMethod<?> method) {
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
}
