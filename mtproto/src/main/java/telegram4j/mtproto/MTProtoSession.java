package telegram4j.mtproto;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.Connection;
import telegram4j.mtproto.auth.AuthorizationKeyHolder;
import telegram4j.mtproto.payload.PayloadMapper;
import telegram4j.mtproto.payload.PayloadMapperStrategy;
import telegram4j.tl.TlObject;
import telegram4j.tl.mtproto.MsgsAck;
import telegram4j.tl.request.mtproto.Ping;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static telegram4j.mtproto.util.CryptoUtil.random;

public final class MTProtoSession {
    private final MTProtoClient client;
    private final Connection connection;
    private final Sinks.Many<ByteBuf> receiver;
    private final DataCenter dataCenter;

    private volatile AuthorizationKeyHolder authorizationKey;
    private volatile long sessionId = random.nextLong();
    private volatile int timeOffset;
    private volatile long serverSalt;
    private volatile long lastMessageId;
    private volatile long lastGeneratedMessageId;
    private final AtomicInteger seqNo = new AtomicInteger();
    private final Queue<Long> acknowledgments = new ConcurrentLinkedQueue<>();
    private final ConcurrentMap<Long, Sinks.One<Object>> resolvers = new ConcurrentHashMap<>();

    MTProtoSession(MTProtoClient client, Connection connection,
                   Sinks.Many<ByteBuf> receiver, DataCenter dataCenter) {
        this.client = client;
        this.connection = connection;
        this.receiver = receiver;
        this.dataCenter = dataCenter;
    }

    public MTProtoClient getClient() {
        return client;
    }

    public Connection getConnection() {
        return connection;
    }

    public Flux<ByteBuf> receiver() {
        return receiver.asFlux()
                .map(ByteBuf::retainedDuplicate)
                .map(client.getOptions().getResources().getTransport()::decode)
                .flatMap(buf -> {
                    if (buf.readableBytes() == Integer.BYTES) { // The error code writes as negative int32
                        int code = buf.readIntLE() * -1;
                        return Mono.error(() -> TransportException.create(code));
                    }
                    return Mono.just(buf);
                })
                .doOnDiscard(ByteBuf.class, ReferenceCountUtil::safeRelease);
    }

    public DataCenter getDataCenter() {
        return dataCenter;
    }

    public AuthorizationKeyHolder getAuthorizationKey() {
        return authorizationKey;
    }

    public void setAuthorizationKey(AuthorizationKeyHolder authorizationKey) {
        this.authorizationKey = Objects.requireNonNull(authorizationKey, "authorizationKey");
    }

    public long getSessionId() {
        return sessionId;
    }

    public int updateSeqNo(TlObject object) {
        boolean content = isContentRelated(object);
        int no = seqNo.get() * 2 + (content ? 1 : 0);
        if (content) {
            seqNo.incrementAndGet();
        }

        return no;
    }

    private static boolean isContentRelated(TlObject object) {
        return !(object instanceof MsgsAck) && !(object instanceof Ping);
    }

    public long getMessageId() {
        long millis = System.currentTimeMillis();
        long seconds = millis / 1000;
        long mod = millis % 1000;
        long messageId = seconds + timeOffset << 32 | mod << 22 | random.nextInt(524288) << 2;

        if (lastGeneratedMessageId >= messageId) {
            messageId = lastMessageId + 4;
        }

        lastGeneratedMessageId = messageId;
        return messageId;
    }

    public boolean updateTimeOffset(long serverTime) {
        int updated = Math.toIntExact(serverTime - System.currentTimeMillis() / 1000);
        boolean changed = Math.abs(timeOffset - updated) > 10;

        lastGeneratedMessageId = 0;
        timeOffset = updated;

        return changed;
    }

    public void setServerSalt(long serverSalt) {
        this.serverSalt = serverSalt;
    }

    public long getServerSalt() {
        return serverSalt;
    }

    public PayloadMapper withPayloadMapper(PayloadMapperStrategy strategy) {
        return strategy.apply(this);
    }

    public Queue<Long> getAcknowledgments() {
        return acknowledgments;
    }

    public ConcurrentMap<Long, Sinks.One<Object>> getResolvers() {
        return resolvers;
    }

    // TODO: find way to write this to the reactive context
    public void setLastMessageId(long lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public long getLastMessageId() {
        return lastMessageId;
    }

    public void reset() {
        sessionId = random.nextLong();
        timeOffset = 0;
        lastGeneratedMessageId = 0;
        seqNo.set(0);
    }
}
