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
import telegram4j.mtproto.util.TlEntityUtil;
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
import java.util.stream.Collectors;

import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;
import static telegram4j.mtproto.DefaultMTProtoClient.BaseRequest.*;
import static telegram4j.mtproto.transport.Transport.QUICK_ACK_MASK;
import static telegram4j.mtproto.util.CryptoUtil.*;

// TODO: split lead and regular clients
public class DefaultMTProtoClient implements MainMTProtoClient {
    private static final Logger log = Loggers.getLogger(DefaultMTProtoClient.class);
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

    private final DataCenter dataCenter;
    private final TcpClient tcpClient;
    private final Transport transport;

    private final AuthorizationContext authContext = new AuthorizationContext();
    private final Sinks.Many<Updates> updates;
    private final Sinks.Many<Request> outbound;
    private final Sinks.Many<TlMethod<?>> authOutbound;
    private final ResettableInterval pingEmitter;
    private final Sinks.Many<State> state;

    private final String id = Integer.toHexString(hashCode());
    private final MTProtoOptions options;

    private volatile long lastPing; // nanotime of sent Ping
    private volatile long lastPong; // nanotime of received Pong
    private final AtomicInteger missedPong = new AtomicInteger();

    private final long sessionId = random.nextLong();
    private volatile Sinks.Empty<Void> closeHook;
    private volatile AuthorizationKeyHolder authKey;
    private volatile Connection connection;
    private volatile boolean closed;
    private volatile boolean initConnectionIsSent;
    private volatile int timeOffset;
    private volatile long serverSalt;
    private volatile long lastMessageId;
    private final AtomicInteger seqNo = new AtomicInteger();
    private final Queue<Long> acknowledgments = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Long, Request> requests = new ConcurrentHashMap<>();
    private final Cache<Integer, Long> quickAckTokens;

    DefaultMTProtoClient(DataCenter dataCenter, MTProtoOptions options) {
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

    public DefaultMTProtoClient(MTProtoOptions options) {
        this(options.getDatacenter(), options);
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
                                pingEmitter.dispose();
                                closeHook.emitEmpty(FAIL_FAST);

                                log.debug("[C:0x{}] Disconnected from the datacenter {}", id, dataCenter);
                                return Mono.fromRunnable(connection::dispose)
                                        .onErrorResume(t -> Mono.empty());
                            case DISCONNECTED:
                                pingEmitter.dispose();

                                return Mono.fromRunnable(connection::dispose)
                                        .onErrorResume(t -> Mono.empty())
                                        .then(Mono.error(RETRY));
                            case CONFIGURED: // if not reset there is a chance that the ping interval will not work after reconnect
                                lastPong = 0;
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

            Flux<Request> payloadFlux = outbound.asFlux()
                    .filter(DefaultMTProtoClient::isContentRelated)
                    .delayUntil(e -> onConnect)
                    .map(r -> {
                        if (r instanceof ContainerRequest) {
                            return r;
                        } else if (r instanceof RpcRequest) {
                            var request = (RpcRequest) r;
                            if (request.method.identifier() == InvokeWithLayer.ID) {
                                return r;
                            }

                            if (!initConnectionIsSent) {
                                return request.withMethod(
                                        options.getInitConnection().withQuery(
                                                options.getInitConnection().query()
                                                        .withQuery(request.method)));
                            }
                            return r;
                        } else {
                            throw new IllegalStateException();
                        }
                    });

            Flux<Request> rpcFlux = outbound.asFlux()
                    .filter(e -> !isContentRelated(e));

            Flux<ByteBuf> outboundFlux = Flux.merge(rpcFlux, payloadFlux)
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

            // Pings will send with MsgsAck in container
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
                initConnectionIsSent = false;
                state.emitNext(State.CONNECTED, options.getEmissionHandler());
                pingEmitter.start(dataCenter.getType() == DataCenter.Type.MEDIA ? PING_QUERY_PERIOD_MEDIA : PING_QUERY_PERIOD);

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

    static boolean isContentRelated(Request request) {
        if (request instanceof ContainerRequest) {
            var container = (ContainerRequest) request;
            return isContentRelated(container.query().method);
        } else if (request instanceof RpcRequest) {
            var rpcRequest = (RpcRequest) request;
            return isContentRelated(rpcRequest.method);
        } else {
            throw new IllegalStateException();
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

        @Override
        public String toString() {
            return "ContainerMessage{" +
                    "messageId=0x" + Long.toHexString(messageId) +
                    ", seqNo=" + seqNo +
                    ", size=" + size +
                    ", method=" + prettyMethodName(method) +
                    '}';
        }
    }

    private Function<Request, ByteBuf> serializePacket(ByteBufAllocator alloc) {
        return req -> {
            // if (log.isTraceEnabled() && !requests.isEmpty())
            //     log.trace(requests.toString());

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

            if (req instanceof ContainerRequest) {
                var container = (ContainerRequest) req;

                var inners = new ArrayList<ContainerizedRequest>();
                var messages = new ArrayList<ContainerMessage>(container.messages.size());
                for (var msg : container.messages) {
                    var old = requests.remove(msg.msgId());
                    if (old instanceof RpcQuery) {
                        rpcQuery = (RpcQuery) old;
                        requestMessageId = getMessageId();
                    } else if (old instanceof RpcRequest) {
                        long newMsgId = getMessageId();
                        if (msg.method() instanceof MsgsStateReq)
                            requests.put(newMsgId, msg);

                        inners.add(msg);
                        messages.add(new ContainerMessage(newMsgId, updateSeqNo(msg.method()), msg.method()));
                    }
                }

                // we rechecked the container, and it turned out that all messages were processed
                if (inners.isEmpty())
                    return null;
                if (rpcQuery != null) {
                    containerMsgId = getMessageId();
                    requests.put(requestMessageId, new QueryContainerRequest(rpcQuery, containerMsgId, requestMessageId));
                } else {
                    throw new IllegalStateException();
                }

                int containerSeqNo = updateSeqNo(false);
                int payloadSize = messages.stream().mapToInt(c -> c.size + 16).sum();
                message = alloc.buffer(24 + payloadSize);
                message.writeLongLE(containerMsgId);
                message.writeIntLE(containerSeqNo);
                message.writeIntLE(payloadSize + 8);
                message.writeIntLE(MessageContainer.ID);
                message.writeIntLE(messages.size());

                for (var c : messages) {
                    message.writeLongLE(c.messageId);
                    message.writeIntLE(c.seqNo);
                    message.writeIntLE(c.size);
                    TlSerializer.serialize(message, c.method);
                }

                req = new ContainerRequest(inners);
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
                        if ((st & SENT) != 0) {
                            statesIds.add(key);
                            if (statesIds.size() >= MAX_IDS_SIZE) {
                                break;
                            }
                        }
                    }
                }

                List<ContainerMessage> messages = new ArrayList<>();

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

                    for (var c : messages) {
                        message.writeLongLE(c.messageId);
                        message.writeIntLE(c.seqNo);
                        message.writeIntLE(c.size);
                        TlSerializer.serialize(message, c.method);
                    }

                    var query = (RpcQuery) req;
                    var rpcInCont = new QueryContainerRequest(query, containerMsgId, requestMessageId);
                    requests.put(requestMessageId, rpcInCont);
                    var inners = new ArrayList<ContainerizedRequest>(messages.size());
                    for (var msg : messages) {
                        if (msg.messageId == requestMessageId) {
                            inners.add(rpcInCont);
                        } else {
                            var wrap = new RpcContainerRequest(msg.method, msg.messageId, containerMsgId);
                            if (msg.method instanceof MsgsStateReq)
                                requests.put(msg.messageId, wrap);

                            inners.add(wrap);
                        }

                    }

                    req = new ContainerRequest(inners);
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
            if (transport.supportQuickAck() && !canContainerize && isContentRelated(req)) {
                int quickAckToken = messageKeyHash.getIntLE(0) | QUICK_ACK_MASK;
                quickAckTokens.put(quickAckToken, requestMessageId);
                quickAck = true;
            }

            ByteBuf messageKey = messageKeyHash.slice(8, 16);
            AES256IGECipher cipher = createAesCipher(messageKey, authKey, false);

            ByteBuf encrypted = cipher.encrypt(plainData);
            ByteBuf packet = Unpooled.wrappedBuffer(authKeyId.retain(), messageKey, encrypted);

            if (rpcLog.isDebugEnabled()) {
                if (canContainerize) {
                    rpcLog.debug("[C:0x{}, M:0x{}] Sending in container 0x{}: {}", id,
                            Long.toHexString(requestMessageId), Long.toHexString(containerMsgId),
                            prettyMethodName(rpcRequest.method));
                } else if (req instanceof ContainerRequest) {
                    rpcLog.debug("[C:0x{}, M:0x{}] Sending container: {}", id,
                            Long.toHexString(containerMsgId), prettyMethodName(rpcQuery.method));
                } else {
                    rpcLog.debug("[C:0x{}, M:0x{}] Sending request: {}", id,
                            Long.toHexString(requestMessageId), prettyMethodName(rpcRequest.method));
                }
            }

            req.setState(SENT);

            return transport.encode(packet, quickAck);
        };
    }

    @Override
    public Sinks.Many<Updates> updates() {
        return updates;
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
    public MTProtoClient createMediaClient(DataCenter dc) {
        if (dataCenter.getType() != DataCenter.Type.REGULAR)
            throw new IllegalStateException("Not default client can't create media clients: " + dataCenter);
        if (dc.getType() != DataCenter.Type.MEDIA)
            throw new IllegalStateException("Invalid datacenter type: " + dc);

        DefaultMTProtoClient client = new DefaultMTProtoClient(dc, options);

        client.authKey = authKey;
        client.lastMessageId = lastMessageId;
        client.timeOffset = timeOffset;
        client.serverSalt = serverSalt;

        return client;
    }

    @Override
    public Mono<Void> close() {
        return Mono.fromSupplier(() -> connection)
                .switchIfEmpty(Mono.error(new IllegalStateException("MTProto client isn't connected")))
                .doOnNext(con -> state.emitNext(State.CLOSED, options.getEmissionHandler()))
                .then();
    }

    public void updateTimeOffset(int serverTime) {
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

    private boolean handleMsgsAck(Object obj, long messageId) {
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
                if (req instanceof ContainerizedRequest) {
                    var cntMsg = (ContainerizedRequest) req;
                    var cnt = requests.remove(cntMsg.containerMsgId());
                    if (cnt != null) {
                        cnt.markState(s -> s | ACKNOWLEDGED);
                    }
                } else if (req instanceof ContainerRequest) {
                    var container = (ContainerRequest) req;
                    requests.remove(msgId);
                    for (var msg : container.messages) {
                        msg.markState(s -> s | ACKNOWLEDGED);
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

            if (rpcLog.isDebugEnabled()) {
                rpcLog.debug("[C:0x{}, M:0x{}] Receiving rpc result", id, Long.toHexString(messageId));
            }

            obj = ungzip(obj);

            handleMsgsAck(obj, messageId);

            var req = (RpcRequest) requests.get(messageId);
            if (req == null) {
                return Mono.empty();
            }

            if (obj instanceof RpcError) {
                RpcError rpcError = (RpcError) obj;
                obj = createRpcException(rpcError, messageId, req);
            } else {
                if (req.method.identifier() == InvokeWithLayer.ID) {
                    initConnectionIsSent = true;
                }
            }

            resolve(messageId, obj);
            if (req instanceof ContainerizedRequest) {
                var cntMsg = (ContainerizedRequest) req;
                requests.remove(cntMsg.containerMsgId());
            }

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

            this.updates.emitNext(updates, options.getEmissionHandler());
            return Mono.empty();
        }

        if (obj instanceof Pong) {
            Pong pong = (Pong) obj;
            messageId = pong.msgId();

            if (rpcLog.isDebugEnabled()) {
                rpcLog.debug("[C:0x{}, M:0x{}] Receiving pong after {}", id, Long.toHexString(messageId),
                        Duration.ofNanos(System.nanoTime() - lastPing));
            }

            lastPong = System.nanoTime();
            missedPong.set(0);

            var req = requests.get(messageId);
            resolve(messageId, obj);
            if (req instanceof ContainerizedRequest) {
                var cntMsg = (ContainerizedRequest) req;
                requests.remove(cntMsg.containerMsgId());
            }
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
                rpcLog.debug("[C:0x{}] Receiving new session creation, new server salt: 0x{}, first id: 0x{}",
                        id, Long.toHexString(newSession.serverSalt()), Long.toHexString(newSession.firstMsgId()));
            }

            serverSalt = newSession.serverSalt();
            lastMessageId = newSession.firstMsgId();

            requests.forEach((k, v) -> {
                if (k < newSession.firstMsgId()) {
                    requests.remove(k);
                    if (v instanceof ContainerizedRequest) { // resend only ContainerRequest
                        return;
                    }

                    outbound.emitNext(v, options.getEmissionHandler());
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
                if (request instanceof ContainerRequest) {
                    var container = (ContainerRequest) request;
                    for (var msg : container.messages) {
                        requests.remove(msg.msgId());
                    }
                }
                outbound.emitNext(request, options.getEmissionHandler());
            }

            return Mono.empty();
        }

        if (obj instanceof MsgsStateInfo) {
            MsgsStateInfo inf = (MsgsStateInfo) obj;

            var req = (RpcRequest) requests.remove(inf.reqMsgId());
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
                        case 3: // not received, resend
                            sub.setState(PENDING);
                            requests.remove(msgId);
                            outbound.emitNext(sub, options.getEmissionHandler());
                            // resendIds.add(msgId);
                            break;
                        case 4:
                            sub.markState(s -> s | ACKNOWLEDGED | REQUESTED_INFO);
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
    private void resolve(long messageId, Object value) {
        requests.computeIfPresent(messageId, (k, v) -> {
            if (v instanceof RpcQuery) {
                var c = (RpcQuery) v;
                Sinks.One<Object> sink = (Sinks.One<Object>) c.sink;

                if (value instanceof Throwable) {
                    Throwable value0 = (Throwable) value;
                    sink.emitError(value0, FAIL_FAST);
                } else {
                    sink.emitValue(value, FAIL_FAST);
                }
            }
            return null;
        });
    }

    @Override
    public String toString() {
        return id;
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
            case MsgResendReq.ID:
                return false;
            default:
                return true;
        }
    }

    // name in format: 'users.GetFullUser'
    static String prettyMethodName(TlMethod<?> method) {
        return method.getClass().getCanonicalName()
                .replace("telegram4j.tl.", "")
                .replace("request.", "")
                .replace("Immutable", "");
    }

    static RpcException createRpcException(RpcError error, long messageId, RpcRequest request) {
        String orig = error.errorMessage();
        int argIdx = orig.indexOf("_X");
        String message = argIdx != -1 ? orig.substring(0, argIdx) : orig;
        String arg = argIdx != -1 ? orig.substring(argIdx) : null;
        String hexMsgId = Long.toHexString(messageId);
        String methodName = String.format("%s/0x%s", prettyMethodName(request.method), hexMsgId);

        String format = String.format("%s returned code: %d, message: %s%s",
                methodName, error.errorCode(),
                message, arg != null ? ", param: " + arg : "");

        return new RpcException(format, error);
    }

    static class RpcRequest extends BaseRequest {

        final TlMethod<?> method;

        RpcRequest(TlMethod<?> method) {
            this.method = method;
        }

        RpcRequest(TlMethod<?> method, int state) {
            this.method = method;
            this.state = state;
        }

        @Override
        public String toString() {
            return "RpcRequest{" +
                    "method=" + prettyMethodName(method) +
                    ", state=" + state +
                    '}';
        }

        public RpcRequest withMethod(TlMethod<?> method) {
            return new RpcRequest(method, state);
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
                    "method=" + method +
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
        static final int REQUESTED_INFO = 0b0100;

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

        final List<ContainerizedRequest> messages;

        ContainerRequest(List<ContainerizedRequest> messages) {
            this.messages = messages;
        }

        @Override
        public String toString() {
            return "ContainerRequest{" +
                    "messages=" + messages +
                    ", state=" + state +
                    '}';
        }

        public QueryContainerRequest query() {
            return messages.stream()
                    .map(TlEntityUtil.isInstance(QueryContainerRequest.class))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseThrow();
        }
    }

    interface ContainerizedRequest extends Request {

        long msgId();

        long containerMsgId();

        TlMethod<?> method();
    }

    static class RpcContainerRequest extends RpcRequest implements ContainerizedRequest {

        final long msgId;
        final long containerMsgId;

        RpcContainerRequest(TlMethod<?> method, long msgId, long containerMsgId) {
            super(method);
            this.msgId = msgId;
            this.containerMsgId = containerMsgId;
        }

        @Override
        public String toString() {
            return "RpcContainerRequest{" +
                    "msgId=0x" + Long.toHexString(msgId) +
                    ", containerMsgId=0x" + Long.toHexString(containerMsgId) +
                    ", state=" + state +
                    ", method=" + prettyMethodName(method) +
                    '}';
        }

        @Override
        public TlMethod<?> method() {
            return method;
        }

        @Override
        public long msgId() {
            return msgId;
        }

        @Override
        public long containerMsgId() {
            return containerMsgId;
        }
    }

    static class QueryContainerRequest extends RpcQuery implements ContainerizedRequest {
        final long msgId;
        final long containerMsgId;

        QueryContainerRequest(RpcQuery query, long containerMsgId, long msgId) {
            super(query.method, query.sink);
            this.containerMsgId = containerMsgId;
            this.msgId = msgId;
        }

        @Override
        public String toString() {
            return "QueryContainerRequest{" +
                    "msgId=0x" + Long.toHexString(msgId) +
                    ", containerMsgId=0x" + Long.toHexString(containerMsgId) +
                    ", state=" + state +
                    ", method=" + prettyMethodName(method) +
                    '}';
        }

        @Override
        public TlMethod<?> method() {
            return method;
        }

        @Override
        public long msgId() {
            return msgId;
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
