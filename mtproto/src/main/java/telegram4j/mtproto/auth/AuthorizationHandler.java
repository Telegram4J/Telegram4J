package telegram4j.mtproto.auth;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.mtproto.MTProtoSession;
import telegram4j.mtproto.PublicRsaKey;
import telegram4j.mtproto.util.AES256IGECipher;
import telegram4j.mtproto.payload.PayloadMapperStrategy;
import telegram4j.tl.TlDeserializer;
import telegram4j.tl.TlSerializer;
import telegram4j.tl.mtproto.*;
import telegram4j.tl.request.mtproto.ReqDHParams;
import telegram4j.tl.request.mtproto.ReqPqMulti;
import telegram4j.tl.request.mtproto.SetClientDHParams;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static telegram4j.mtproto.util.CryptoUtil.*;
import static telegram4j.tl.TlSerialUtil.writeString;

public final class AuthorizationHandler {

    private static final Logger log = Loggers.getLogger(AuthorizationHandler.class);

    private final MTProtoSession session;
    private final AuthorizationContext context;
    private final Sinks.One<AuthorizationKeyHolder> onAuthSink;

    public AuthorizationHandler(MTProtoSession session, Sinks.One<AuthorizationKeyHolder> onAuthSink) {
        this.session = session;
        this.context = session.getClient().getOptions().getAuthorizationContext();
        this.onAuthSink = onAuthSink;
    }

    public Mono<Void> start() {
        return Mono.defer(() -> {
            log.debug("Auth key generation started!");

            byte[] nonce = random.generateSeed(16);
            context.setNonce(nonce);

            return session.withPayloadMapper(PayloadMapperStrategy.UNENCRYPTED)
                    .send(ReqPqMulti.builder().nonce(nonce).build())
                    .then();
        });
    }

    public Mono<Void> handle(ByteBuf payload) {
        return session.withPayloadMapper(PayloadMapperStrategy.UNENCRYPTED)
                .receive(payload)
                .flatMap(obj -> {
                    // first step. DH exchange initiation
                    if (obj instanceof ResPQ) {
                        ResPQ resPQ = (ResPQ) obj;
                        return handleResPQ(resPQ);
                    }

                    // Step 2. Presenting proof of work; Server authentication
                    if (obj instanceof ServerDHParams) {
                        ServerDHParams serverDHParams = (ServerDHParams) obj;
                        return handleServerDHParams(serverDHParams);
                    }

                    // Step 3. DH key exchange complete
                    if (obj instanceof DhGenOk) {
                        DhGenOk dhGenOk = (DhGenOk) obj;
                        return handleDhGenOk(dhGenOk);
                    }

                    if (obj instanceof DhGenRetry) {
                        DhGenRetry dhGenRetry = (DhGenRetry) obj;
                        return handleDhGenRetry(dhGenRetry);
                    }

                    if (obj instanceof DhGenFail) {
                        DhGenFail dhGenFail = (DhGenFail) obj;
                        return handleDhGenFail(dhGenFail);
                    }

                    return Mono.error(() -> new IllegalStateException("Unexpected type: 0x" + Integer.toHexString(obj.identifier())));
                });
    }

    private Mono<Void> handleDhGenFail(DhGenFail dhGenFail) {
        byte[] newNonceHash3 = dhGenFail.newNonceHash3();
        byte[] newNonceHash = substring(sha1Digest(context.getNewNonce(), new byte[]{3}, context.getAuthAuxHash()), 4, 16);

        if (!Arrays.equals(newNonceHash3, newNonceHash)) {
            return Mono.error(() -> new IllegalStateException("New nonce hash mismatch, excepted: "
                    + ByteBufUtil.hexDump(newNonceHash) + ", but received: " + ByteBufUtil.hexDump(newNonceHash3)));
        }

        return Mono.error(new IllegalStateException("Failed to create an authorization key."));
    }

    private Mono<Void> handleResPQ(ResPQ resPQ) {
        ByteBufAllocator alloc = session.getConnection().channel().alloc();
        context.setServerNonce(resPQ.serverNonce());
        byte[] pqBytes = resPQ.pq();

        List<Long> fingerprints = resPQ.serverPublicKeyFingerprints();
        long fingerprint = fingerprints.get(0);
        PublicRsaKey key = PublicRsaKey.publicKeys.get(fingerprint);

        BigInteger pq = fromByteArray(pqBytes);
        BigInteger p = BigInteger.valueOf(pqPrimeLeemon(pq.longValueExact()));
        byte[] pBytes = toByteArray(p);
        BigInteger q = pq.divide(p);
        byte[] qBytes = toByteArray(q);

        context.setNewNonce(random.generateSeed(32));

        assert p.longValueExact() < q.longValueExact();

        ByteBuf pqInnerData = alloc.buffer()
                .writeIntLE(0x83c95aec)
                .writeBytes(writeString(alloc, pqBytes))
                .writeBytes(writeString(alloc, pBytes))
                .writeBytes(writeString(alloc, qBytes))
                .writeBytes(context.getNonce())
                .writeBytes(context.getServerNonce())
                .writeBytes(context.getNewNonce());

        byte[] pqInnerDataBytes = toByteArray(pqInnerData);
        byte[] hash = sha1Digest(pqInnerDataBytes);
        byte[] seed = random.generateSeed(255 - hash.length - pqInnerDataBytes.length);
        byte[] dataWithHash = concat(hash, pqInnerDataBytes, seed);
        byte[] encrypted = rsaEncrypt(dataWithHash, key);

        return session.withPayloadMapper(PayloadMapperStrategy.UNENCRYPTED)
                .send(ReqDHParams.builder()
                        .nonce(context.getNonce())
                        .serverNonce(context.getServerNonce())
                        .encryptedData(encrypted)
                        .p(pBytes)
                        .q(qBytes)
                        .publicKeyFingerprint(fingerprint)
                        .build())
                .then();
    }

    private Mono<Void> handleDhGenOk(DhGenOk dhGenOk) {
        byte[] newNonceHash1 = dhGenOk.newNonceHash1();
        byte[] newNonceHash = substring(sha1Digest(context.getNewNonce(), new byte[]{1}, context.getAuthAuxHash()), 4, 16);

        if (!Arrays.equals(newNonceHash1, newNonceHash)) {
            return Mono.error(() -> new IllegalStateException("New nonce hash mismatch, excepted: "
                    + ByteBufUtil.hexDump(newNonceHash) + ", but received: " + ByteBufUtil.hexDump(newNonceHash1)));
        }

        long serverSalt = readLongLE(xor(substring(context.getNewNonce(), 0, 8),
                substring(context.getServerNonce(), 0, 8)));

        context.setServerSalt(serverSalt);
        session.setServerSalt(serverSalt);

        log.debug("Auth key generation completed!");
        AuthorizationKeyHolder authKey = new AuthorizationKeyHolder(context.getAuthKey());
        onAuthSink.emitValue(authKey, Sinks.EmitFailureHandler.FAIL_FAST);
        return session.getClient().getOptions()
                .getResources().getStoreLayout()
                .updateAuthorizationKey(authKey);
    }

    private Mono<Void> handleDhGenRetry(DhGenRetry dhGenRetry) {
        byte[] newNonceHash2 = dhGenRetry.newNonceHash2();
        byte[] newNonceHash = substring(sha1Digest(context.getNewNonce(), new byte[]{2}, context.getAuthAuxHash()), 4, 16);

        if (!Arrays.equals(newNonceHash2, newNonceHash)) {
            return Mono.error(() -> new IllegalStateException("New nonce hash mismatch, excepted: "
                    + ByteBufUtil.hexDump(newNonceHash) + ", but received: " + ByteBufUtil.hexDump(newNonceHash2)));
        }

        ServerDHParams serverDHParams = context.getServerDHParams();
        log.debug("Retrying dh params extending, attempt: {}.", context.getRetry());
        return handleServerDHParams(serverDHParams);
    }

    private Mono<Void> handleServerDHParams(ServerDHParams serverDHParams) {
        ByteBufAllocator alloc = session.getConnection().channel().alloc();
        context.setServerDHParams(serverDHParams); // for DhGenRetry
        byte[] encryptedAnswerB = serverDHParams.encryptedAnswer();

        byte[] tmpAesKey = concat(
                sha1Digest(context.getNewNonce(), context.getServerNonce()),
                substring(sha1Digest(context.getServerNonce(), context.getNewNonce()), 0, 12));

        byte[] tmpAesIv = concat(concat(substring(
                sha1Digest(context.getServerNonce(), context.getNewNonce()), 12, 8),
                sha1Digest(context.getNewNonce(), context.getNewNonce())),
                substring(context.getNewNonce(), 0, 4));

        AES256IGECipher cipher = new AES256IGECipher(tmpAesKey, tmpAesIv);
        ByteBuf answer = alloc.buffer()
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

        session.updateTimeOffset(serverDHInnerData.serverTime());

        return session.withPayloadMapper(PayloadMapperStrategy.UNENCRYPTED)
                .send(SetClientDHParams.builder()
                        .nonce(context.getNonce())
                        .serverNonce(context.getServerNonce())
                        .encryptedData(dataWithHashEnc)
                        .build())
                .then();
    }
}
