package telegram4j.mtproto;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.util.ReferenceCountUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.Connection;
import reactor.netty.FutureMono;
import reactor.util.annotation.Nullable;
import reactor.util.concurrent.Queues;
import telegram4j.mtproto.auth.AuthorizationContext;
import telegram4j.mtproto.auth.AuthorizationKeyHolder;
import telegram4j.mtproto.util.AES256IGECipher;
import telegram4j.tl.*;
import telegram4j.tl.mtproto.MessageContainer;
import telegram4j.tl.mtproto.MsgsAck;
import telegram4j.tl.request.mtproto.Ping;

import java.util.Arrays;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static telegram4j.mtproto.util.CryptoUtil.*;

public final class MTProtoSession {
    private final Connection connection;
    private final AuthorizationContext authContext;
    private final Sinks.Many<MTProtoObject> authReceiver;
    private final Sinks.Many<TlObject> rpcReceiver;
    private final Sinks.Many<Updates> updates;
    private final DataCenter dataCenter;
    private final SessionResources mtProtoResources;

    private volatile AuthorizationKeyHolder authorizationKey;
    private volatile long sessionId = random.nextLong();
    private volatile int timeOffset;
    private volatile long serverSalt;
    private volatile long lastMessageId;
    private volatile long lastGeneratedMessageId;
    private final AtomicInteger seqNo = new AtomicInteger();
    private final Queue<Long> acknowledgments = new ConcurrentLinkedQueue<>();
    private final ConcurrentMap<Long, Sinks.One<?>> resolvers = new ConcurrentHashMap<>();

    MTProtoSession(Connection connection, Sinks.Many<MTProtoObject> authReceiver,
                   Sinks.Many<TlObject> rpcReceiver, DataCenter dataCenter,
                   SessionResources mtProtoResources) {
        this.connection = connection;
        this.authReceiver = authReceiver;
        this.rpcReceiver = rpcReceiver;
        this.dataCenter = dataCenter;
        this.mtProtoResources = mtProtoResources;

        this.authContext = new AuthorizationContext();
        this.updates = Sinks.many().multicast().onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);
    }

    public Connection getConnection() {
        return connection;
    }

    public SessionResources getMtProtoResources() {
        return mtProtoResources;
    }

    public Flux<TlObject> rpcReceiver() {
        return rpcReceiver.asFlux();
    }

    public Flux<MTProtoObject> authReceiver() {
        return authReceiver.asFlux();
    }

    public Sinks.Many<Updates> updates() {
        return updates;
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

    public <R, T extends TlMethod<R>> Mono<R> sendEncrypted(T object) {
        return Mono.defer(() -> {
            Channel channel = connection.channel();
            ByteBufAllocator alloc = channel.alloc();
            ByteBuf data = TlSerializer.serialize(alloc, object);

            long messageId = getMessageId();
            int seqNo = updateSeqNo(object);

            int minPadding = 12;
            int unpadded = (32 + data.readableBytes() + minPadding) % 16;
            int padding = minPadding + (unpadded != 0 ? 16 - unpadded : 0);

            ByteBuf plainData = alloc.buffer()
                    .writeLongLE(serverSalt)
                    .writeLongLE(sessionId)
                    .writeLongLE(messageId)
                    .writeIntLE(seqNo)
                    .writeIntLE(data.readableBytes())
                    .writeBytes(data)
                    .writeBytes(random.generateSeed(padding));
            ReferenceCountUtil.safeRelease(data);

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

            Sinks.One<R> res = Sinks.one();
            if (object instanceof MsgsAck) {
                res.emitEmpty(Sinks.EmitFailureHandler.FAIL_FAST);
            } else {
                resolvers.put(messageId, res);
            }

            return FutureMono.from(channel.writeAndFlush(
                    mtProtoResources.getTransport().encode(payload)))
                    .then(res.asMono());
        });
    }

    public <R extends MTProtoObject, T extends TlMethod<R> & MTProtoObject> Mono<Void> sendUnencrypted(T object) {
        return Mono.defer(() -> {
            Channel channel = connection.channel();
            ByteBufAllocator alloc = channel.alloc();
            long messageId = getMessageId();
            ByteBuf data = TlSerializer.serialize(alloc, object);

            ByteBuf payload = alloc.buffer()
                    .writeLongLE(0) // auth key id
                    .writeLongLE(messageId)
                    .writeIntLE(data.readableBytes())
                    .writeBytes(data);
            ReferenceCountUtil.safeRelease(data);

            return FutureMono.from(channel.writeAndFlush(
                    mtProtoResources.getTransport().encode(payload)));
        });
    }

    public Queue<Long> getAcknowledgments() {
        return acknowledgments;
    }

    @SuppressWarnings("unchecked")
    public void resolve(long messageId, @Nullable Object value) {
        resolvers.computeIfPresent(messageId, (k, sink) -> {
            Sinks.One<Object> sink0 = (Sinks.One<Object>) sink;
            if (value == null) {
                sink0.emitEmpty(Sinks.EmitFailureHandler.FAIL_FAST);
            } else {
                sink0.emitValue(value, Sinks.EmitFailureHandler.FAIL_FAST);
            }

            return null;
        });

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

    public AuthorizationContext getAuthContext() {
        return authContext;
    }

    private static boolean isContentRelated(TlObject object) {
        return !(object instanceof MsgsAck) && !(object instanceof Ping) && !(object instanceof MessageContainer);
    }
}
