package telegram4j.mtproto;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.Connection;
import telegram4j.mtproto.crypto.PayloadMapper;
import telegram4j.mtproto.crypto.PayloadMapperStrategy;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static telegram4j.mtproto.crypto.CryptoUtil.random;

public final class MTProtoSession {
    private final MTProtoClient client;
    private final Connection connection;
    private final Sinks.Many<ByteBuf> receiver;
    private final DataCenter dataCenter;

    private final AtomicLong sessionId = new AtomicLong(random.nextLong());
    private final AtomicInteger seqNo = new AtomicInteger();
    private final AtomicInteger timeOffset = new AtomicInteger();
    private final AtomicLong serverSalt = new AtomicLong();
    private final AtomicLong lastMessageId = new AtomicLong();
    private final AtomicLong lastGeneratedMessageId = new AtomicLong();
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

    public long getSessionId() {
        return sessionId.get();
    }

    public int updateSeqNo(boolean content) {
        int no = seqNo.get() * 2 + (content ? 1 : 0);
        if (content) {
            seqNo.incrementAndGet();
        }

        return no;
    }

    public long getMessageId() {
        long millis = System.currentTimeMillis();
        long seconds = millis / 1000;
        long mod = millis % 1000;
        long messageId = seconds + timeOffset.get() << 32 | mod << 22 | random.nextInt(524288) << 2;

        if (lastGeneratedMessageId.get() >= messageId) {
            messageId = lastMessageId.get() + 4;
        }

        lastGeneratedMessageId.set(messageId);
        return messageId;
    }

    public boolean updateTimeOffset(long serverTime) {
        int updated = Math.toIntExact(serverTime - System.currentTimeMillis() / 1000);
        boolean changed = Math.abs(timeOffset.get() - updated) > 10;

        lastGeneratedMessageId.set(0);
        timeOffset.set(updated);

        return changed;
    }

    public void setServerSalt(long serverSalt) {
        this.serverSalt.set(serverSalt);
    }

    public long getServerSalt() {
        return serverSalt.get();
    }

    // TODO: must fetch from cache/store.
    public byte[] getAuthKey() {
        return client.getOptions().getAuthorizationContext().getAuthKey();
    }

    public byte[] getAuthKeyId() {
        return client.getOptions().getAuthorizationContext().getAuthKeyId();
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
        this.lastMessageId.set(lastMessageId);
    }

    public long getLastMessageId() {
        return lastMessageId.get();
    }

    public void reset() {
        sessionId.set(random.nextLong());
        timeOffset.set(0);
        lastGeneratedMessageId.set(0);
        seqNo.set(0);
    }
}
