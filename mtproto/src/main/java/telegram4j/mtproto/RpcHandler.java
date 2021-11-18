package telegram4j.mtproto;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.mtproto.crypto.AES256IGECipher;
import telegram4j.mtproto.crypto.MTProtoAuthorizationContext;
import telegram4j.tl.TlDeserializer;
import telegram4j.tl.TlObject;
import telegram4j.tl.mtproto.RpcError;

import java.util.Arrays;

import static telegram4j.mtproto.crypto.CryptoUtil.*;
import static telegram4j.tl.TlSerialUtil.*;

public class RpcHandler {

    private static final Logger log = Loggers.getLogger(RpcHandler.class);

    private final MTProtoClient client;

    public RpcHandler(MTProtoClient client) {
        this.client = client;
    }

    public Mono<Void> handle(ByteBuf payload) {
        MTProtoAuthorizationContext ctx = client.getContext();
        ByteBufAllocator alloc = payload.alloc();
        if (ctx.getServerSalt() == null || ctx.getAuthKey() == null) {
            return Mono.empty();
        }

        long authKeyId = payload.readLongLE();
        if (authKeyId == 0) {
            return Mono.error(new IllegalStateException("Auth key id must be non zero."));
        }

        // if (authKeyId != readLongLE(authKeyId)) {
        //    return Mono.error(new IllegalStateException("Incorrect auth key id."));
        // }

        byte[] messageKey = readInt128(payload);

        ByteBuf authKeyBuf = alloc.buffer().writeBytes(ctx.getAuthKey());
        AES256IGECipher cipher = createAesCipher(messageKey, authKeyBuf, true);

        byte[] decrypted = cipher.decrypt(toByteArray(payload));
        byte[] messageKeyCLarge = sha256Digest(concat(toByteArray(authKeyBuf.slice(96, 32)), decrypted));
        byte[] messageKeyC = Arrays.copyOfRange(messageKeyCLarge, 8, 24);

        if(!Arrays.equals(messageKey, messageKeyC)){
            return Mono.error(new IllegalStateException("Incorrect message key."));
        }

        ByteBuf decryptedBuf = alloc.buffer().writeBytes(decrypted);

        long salt = decryptedBuf.readLongLE();
        log.info("salt: {}", salt);
        long sessionId = decryptedBuf.readLongLE();
        log.info("sessionId: {}", sessionId);
        long messageId = decryptedBuf.readLongLE();
        log.info("messageId: {}", messageId);
        int seqNo = decryptedBuf.readIntLE();
        log.info("seqNo: {}", seqNo);
        int length = decryptedBuf.readIntLE();
        log.info("length: {}", length);

        if (length % 4 != 0) {
            return Mono.error(new IllegalStateException("Length of data isn't a multiple of four."));
        }

        TlObject obj = TlDeserializer.deserialize(decryptedBuf.readBytes(length));
        log.info("received object: {}", obj);

        if (obj instanceof RpcError) {
            RpcError rpcError = (RpcError) obj;
            return Mono.error(() -> RpcException.create(rpcError));
        }

        return Mono.empty();
    }

    public MTProtoClient getClient() {
        return client;
    }
}
