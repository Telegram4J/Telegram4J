package telegram4j.mtproto.crypto;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.PublicRsaKey;
import telegram4j.tl.*;
import telegram4j.tl.mtproto.*;
import telegram4j.tl.request.mtproto.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static telegram4j.mtproto.crypto.CryptoUtil.*;
import static telegram4j.tl.TlSerialUtil.writeString;

public class MTProtoAuthorizationHandler {

    public static long timeDelta;

    private static final Logger log = Loggers.getLogger(MTProtoAuthorizationHandler.class);

    private final MTProtoClient client;
    private final MTProtoAuthorizationContext context;

    public MTProtoAuthorizationHandler(MTProtoClient client, MTProtoAuthorizationContext context) {
        this.client = client;
        this.context = context;
    }

    public Mono<Void> start() {
        return Mono.defer(() -> {
            byte[] nonce = random.generateSeed(16);
            context.setNonce(nonce);

            log.info("start");
            return send(ByteBufAllocator.DEFAULT, ReqPqMulti.builder()
                    .nonce(nonce)
                    .build());
        });
    }

    public Mono<Void> handle(ByteBuf payload) {
        if (context.getServerSalt() != null) { // authorization key has already generated
            return Mono.empty();
        }

        ByteBufAllocator alloc = payload.alloc();

        long authKeyId = payload.readLongLE();
        if (authKeyId != 0) {
            return Mono.error(new IllegalStateException("Auth key id must be zero, but received: " + authKeyId));
        }
        payload.skipBytes(12); // message id (8) + payload length (4)

        if (log.isTraceEnabled()) {
            log.trace("dump: {}", ByteBufUtil.hexDump(payload));
        }
        TlObject obj = TlDeserializer.deserialize(payload);
        if (log.isTraceEnabled()) {
            log.trace("received: {}/{}", obj.getClass().getCanonicalName(), Integer.toHexString(obj.identifier()));
            log.trace(obj.toString());
        }

        // first step. DH exchange initiation
        if (obj instanceof ResPQ) {
            ResPQ resPQ = (ResPQ) obj;
            context.setServerNonce(resPQ.serverNonce());
            byte[] pqB = resPQ.pq();

            List<Long> fingerprints = resPQ.serverPublicKeyFingerprints();
            long fingerprint = fingerprints.get(0);
            PublicRsaKey key = PublicRsaKey.publicKeys.get(fingerprint);

            BigInteger pq = fromByteArray(pqB);
            BigInteger p = BigInteger.valueOf(pqPrimeLeemon(pq.longValueExact()));
            BigInteger q = pq.divide(p);

            context.setNewNonce(random.generateSeed(32));

            assert p.longValueExact() < q.longValueExact();

            ByteBuf pqInnerData = alloc.heapBuffer()
                    .writeIntLE(0x83c95aec)
                    .writeBytes(writeString(alloc, pqB))
                    .writeBytes(writeString(alloc, toByteArray(p)))
                    .writeBytes(writeString(alloc, toByteArray(q)))
                    .writeBytes(context.getNonce())
                    .writeBytes(context.getServerNonce())
                    .writeBytes(context.getNewNonce());

            byte[] pqInnerDataB = toByteArray(pqInnerData);
            byte[] hash = sha1Digest(pqInnerDataB);
            byte[] seed = random.generateSeed(255 - hash.length - pqInnerDataB.length);
            byte[] dataWithHash = concat(hash, pqInnerDataB, seed);
            byte[] encrypted = rsaEncrypt(dataWithHash, key);

            return send(alloc, ReqDHParams.builder()
                    .nonce(context.getNonce())
                    .serverNonce(context.getServerNonce())
                    .encryptedData(encrypted)
                    .p(toByteArray(p))
                    .q(toByteArray(q))
                    .publicKeyFingerprint(fingerprint)
                    .build());
        }

        // Step 2. Presenting proof of work; Server authentication
        if (obj instanceof ServerDHParams) {
            ServerDHParams serverDHParams = (ServerDHParams) obj;
            context.setServerDHParams(serverDHParams); // for DhGenRetry
            return sendSetClientDHParams(alloc, serverDHParams);
        }

        // Step 3. DH key exchange complete
        if (obj instanceof DhGenOk) {
            DhGenOk dhGenOk = (DhGenOk) obj;
            byte[] newNonceHash1 = dhGenOk.newNonceHash1();
            byte[] newNonceHash = substring(sha1Digest(context.getNewNonce(), new byte[]{1}, context.getAuthAuxHash()), 4, 16);

            if (!Arrays.equals(dhGenOk.newNonceHash1(), newNonceHash)) {
                return Mono.error(() -> new IllegalStateException("New nonce hash mismatch, excepted: "
                        + ByteBufUtil.hexDump(newNonceHash) + ", but received: " + ByteBufUtil.hexDump(newNonceHash1)));
            }

            context.setServerSalt(xor(substring(context.getNewNonce(), 0, 8),
                    substring(context.getServerNonce(), 0, 8)));
            return Mono.empty();
        }

        if (obj instanceof DhGenRetry) {
            DhGenRetry dhGenRetry = (DhGenRetry) obj;
            byte[] newNonceHash2 = dhGenRetry.newNonceHash2();
            byte[] newNonceHash = substring(sha1Digest(context.getNewNonce(), new byte[]{2}, context.getAuthAuxHash()), 4, 16);

            if (!Arrays.equals(dhGenRetry.newNonceHash2(), newNonceHash)) {
                return Mono.error(() -> new IllegalStateException("New nonce hash mismatch, excepted: "
                        + ByteBufUtil.hexDump(newNonceHash) + ", but received: " + ByteBufUtil.hexDump(newNonceHash2)));
            }

            ServerDHParams serverDHParams = context.getServerDHParams();
            return sendSetClientDHParams(alloc, serverDHParams);
        }

        if (obj instanceof DhGenFail) {
            DhGenFail dhGenFail = (DhGenFail) obj;
            byte[] newNonceHash3 = dhGenFail.newNonceHash3();
            byte[] newNonceHash = substring(sha1Digest(context.getNewNonce(), new byte[]{3}, context.getAuthAuxHash()), 4, 16);

            if (!Arrays.equals(newNonceHash3, newNonceHash)) {
                return Mono.error(() -> new IllegalStateException("New nonce hash mismatch, excepted: "
                        + ByteBufUtil.hexDump(newNonceHash) + ", but received: " + ByteBufUtil.hexDump(newNonceHash3)));
            }

            return Mono.error(new IllegalStateException("Failed to create an authorization key."));
        }

        return Mono.error(() -> new IllegalStateException("Unexpected type: 0x" + Integer.toHexString(obj.identifier())));
    }

    private Mono<Void> sendSetClientDHParams(ByteBufAllocator alloc, ServerDHParams serverDHParams) {
        byte[] encryptedAnswerB = serverDHParams.encryptedAnswer();

        byte[] tmpAesKey = concat(sha1Digest(context.getNewNonce(), context.getServerNonce()),
                substring(sha1Digest(context.getServerNonce(), context.getNewNonce()), 0, 12));

        byte[] tmpAesIv = concat(concat(substring(
                sha1Digest(context.getServerNonce(), context.getNewNonce()), 12, 8),
                sha1Digest(context.getNewNonce(), context.getNewNonce())),
                substring(context.getNewNonce(), 0, 4));

        AES256IGECipher cipher = new AES256IGECipher(tmpAesKey, tmpAesIv);
        ByteBuf answer = alloc.heapBuffer()
                .writeBytes(cipher.decrypt(encryptedAnswerB))
                .skipBytes(20); // answer hash

        ServerDHInnerData serverDHInnerData = TlDeserializer.deserialize(answer);

        BigInteger b = fromByteArray(random.generateSeed(256));
        BigInteger g = BigInteger.valueOf(serverDHInnerData.g());
        BigInteger dhPrime = fromByteArray(serverDHInnerData.dhPrime());
        BigInteger gb = g.modPow(b, dhPrime);

        BigInteger authKeyVal = fromByteArray(serverDHInnerData.gA()).modPow(b, dhPrime);
        context.setAuthKey(alignKeyZero(toByteArray(authKeyVal), 256));
        context.setAuthAuxHash(substring(sha1Digest(context.getAuthKey()), 0, 8));

        ClientDHInnerData clientDHInnerData = ClientDHInnerData.builder()
                .retryId(context.getRetry().getAndIncrement())
                .nonce(context.getNonce())
                .serverNonce(context.getServerNonce())
                .gB(toByteArray(gb))
                .build();

        byte[] innerData = toByteArray(TlSerializer.serialize(alloc, clientDHInnerData));
        byte[] innerDataWithHash = align(concat(sha1Digest(innerData), innerData), 16);
        byte[] dataWithHashEnc = cipher.encrypt(innerDataWithHash);

        return send(alloc, SetClientDHParams.builder()
                .nonce(context.getNonce())
                .serverNonce(context.getServerNonce())
                .encryptedData(dataWithHashEnc)
                .build());
    }

    public MTProtoAuthorizationContext getContext() {
        return context;
    }

    public MTProtoClient getClient() {
        return client;
    }

    private <T extends TlObject> Mono<Void> send(ByteBufAllocator alloc, TlMethod<? super T> object) {
        return Mono.defer(() -> {
            long messageId = (System.currentTimeMillis() + timeDelta) / 1000 << 32;
            ByteBuf data = TlSerializer.serialize(alloc, object);

            return client.send(alloc.buffer()
                    .writeLongLE(0) // auth key id
                    .writeLongLE(messageId)
                    .writeIntLE(data.readableBytes())
                    .writeBytes(data));
        });
    }
}
