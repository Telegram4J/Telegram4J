package telegram4j.mtproto.crypto;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.FutureMono;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.mtproto.MTProtoSession;
import telegram4j.tl.TlDeserializer;
import telegram4j.tl.TlMethod;
import telegram4j.tl.TlObject;
import telegram4j.tl.TlSerializer;
import telegram4j.tl.mtproto.MsgsAck;
import telegram4j.tl.request.mtproto.Ping;

import java.util.Arrays;

import static telegram4j.mtproto.crypto.CryptoUtil.*;
import static telegram4j.tl.TlSerialUtil.readInt128;

class EncryptedPayloadMapper implements PayloadMapper {

    private static final Logger log = Loggers.getLogger(EncryptedPayloadMapper.class);

    private final MTProtoSession session;

    public EncryptedPayloadMapper(MTProtoSession session) {
        this.session = session;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R, T extends TlMethod<R>> Mono<R> send(T object) {
        return Mono.defer(() -> {

            Channel channel = session.getConnection().channel();
            ByteBufAllocator alloc = channel.alloc();
            ByteBuf data = TlSerializer.serialize(alloc, object);

            long messageId = session.getMessageId();
            int seqNo = session.updateSeqNo(isContentRelated(object));
            long sessionId = session.getSessionId();

            int minPadding = 12;
            int unpadded = (32 + data.readableBytes() + minPadding) % 16;
            int padding = minPadding + (unpadded != 0 ? 16 - unpadded : 0);

            ByteBuf plainData = alloc.buffer()
                    .writeLongLE(session.getServerSalt())
                    .writeLongLE(sessionId)
                    .writeLongLE(messageId)
                    .writeIntLE(seqNo)
                    .writeIntLE(data.readableBytes())
                    .writeBytes(data)
                    .writeBytes(random.generateSeed(padding));

            byte[] authKey = session.getAuthKey();
            byte[] authKeyId = session.getAuthKeyId();

            byte[] plainDataB = toByteArray(plainData);
            byte[] msgKeyLarge = sha256Digest(concat(Arrays.copyOfRange(authKey, 88, 120), plainDataB));
            byte[] messageKey = Arrays.copyOfRange(msgKeyLarge, 8, 24);

            ByteBuf authKeyBuf = alloc.buffer().writeBytes(authKey);
            AES256IGECipher cipher = createAesCipher(messageKey, authKeyBuf, false);

            ByteBuf payload = alloc.buffer()
                    .writeBytes(authKeyId)
                    .writeBytes(messageKey)
                    .writeBytes(cipher.encrypt(plainDataB));

            Sinks.One<R> sink = Sinks.one();
            session.getResolvers().put(messageId, (Sinks.One<Object>) sink);

            return FutureMono.from(channel.writeAndFlush(session.getClient()
                    .getOptions().getResources().getTransport()
                    .encode(payload)))
                    .then(sink.asMono());
        });
    }

    @Override
    public <T extends TlObject> Mono<T> receive(ByteBuf payload) {
        return Mono.fromSupplier(() -> {
            ByteBufAllocator alloc = payload.alloc();
            long authKeyId = payload.readLongLE();
            if (authKeyId == 0) {
//                throw new IllegalStateException("Auth key id must be non zero.");
                return null;
            }

            if (authKeyId != readLongLE(session.getAuthKeyId())) {
                throw new IllegalStateException("Incorrect auth key id.");
            }

            byte[] messageKey = readInt128(payload);

            ByteBuf authKeyBuf = alloc.buffer().writeBytes(session.getAuthKey());
            AES256IGECipher cipher = createAesCipher(messageKey, authKeyBuf, true);

            byte[] decrypted = cipher.decrypt(toByteArray(payload));
            byte[] messageKeyCLarge = sha256Digest(concat(toByteArray(authKeyBuf.slice(96, 32)), decrypted));
            byte[] messageKeyC = Arrays.copyOfRange(messageKeyCLarge, 8, 24);

            if(!Arrays.equals(messageKey, messageKeyC)){
                throw new IllegalStateException("Incorrect message key.");
            }

            ByteBuf decryptedBuf = alloc.buffer().writeBytes(decrypted);

            long serverSalt = decryptedBuf.readLongLE();
            log.trace("serverSalt: {}", serverSalt);
            long sessionId = decryptedBuf.readLongLE();
            log.trace("sessionId: {}", sessionId);
            long messageId = decryptedBuf.readLongLE();
            log.trace("messageId: {}", messageId);
            int seqNo = decryptedBuf.readIntLE();
            log.trace("seqNo: {}", seqNo);
            int length = decryptedBuf.readIntLE();
            log.trace("length: {}", length);

            if (length % 4 != 0) {
                throw new IllegalStateException("Length of data isn't a multiple of four.");
            }

            T obj = TlDeserializer.deserialize(decryptedBuf.readBytes(length));

            session.updateTimeOffset(messageId >> 32);
            session.setLastMessageId(messageId);

            return obj;
        });
    }

    private boolean isContentRelated(TlObject object) {
        return !(object instanceof MsgsAck) && !(object instanceof Ping);
    }
}
