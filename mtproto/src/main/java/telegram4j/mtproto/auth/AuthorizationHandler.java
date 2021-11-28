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
import telegram4j.tl.MTProtoObject;
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

public final class AuthorizationHandler {

    private static final Logger log = Loggers.getLogger(AuthorizationHandler.class);

    private final MTProtoSession session;
    private final Sinks.One<AuthorizationKeyHolder> onAuthSink;

    public AuthorizationHandler(MTProtoSession session, Sinks.One<AuthorizationKeyHolder> onAuthSink) {
        this.session = session;
        this.onAuthSink = onAuthSink;
    }

    public Mono<Void> start() {
        return Mono.defer(() -> {
            log.debug("Auth key generation started!");

            byte[] nonce = random.generateSeed(16);
            session.getAuthContext().setNonce(nonce);

            return session.sendUnencrypted(ReqPqMulti.builder().nonce(nonce).build());
        });
    }

    public Mono<Void> handle(MTProtoObject obj) {
        if (obj instanceof ResPQ) {
            ResPQ resPQ = (ResPQ) obj;

            return handleResPQ(resPQ);
        }

        if (obj instanceof ServerDHParams) {
            ServerDHParams serverDHParams = (ServerDHParams) obj;

            return handleServerDHParams(serverDHParams);
        }

        if (obj instanceof DhGenOk) {
            DhGenOk dhGenOk = (DhGenOk) obj;
            return handleDhGenOk(dhGenOk);
        }

        if (obj instanceof DhGenRetry) {
            DhGenRetry dhGenRetry = (DhGenRetry) obj;
            return handleDhGenRetry(dhGenRetry)
                    .then(Mono.error(new IllegalArgumentException()));
        }

        if (obj instanceof DhGenFail) {
            DhGenFail dhGenFail = (DhGenFail) obj;
            return handleDhGenFail(dhGenFail).then(Mono.empty());
        }

        return Mono.error(new IllegalStateException("Incorrect MTProto object: 0x" + Integer.toHexString(obj.identifier())));
    }

    private Mono<Void> handleDhGenFail(DhGenFail dhGenFail) {
        byte[] newNonceHash3 = dhGenFail.newNonceHash3();
        byte[] newNonceHash = substring(sha1Digest(session.getAuthContext().getNewNonce(), new byte[]{3},
                session.getAuthContext().getAuthAuxHash()), 4, 16);

        if (!Arrays.equals(newNonceHash3, newNonceHash)) {
            return Mono.error(() -> new IllegalStateException("New nonce hash mismatch, excepted: "
                    + ByteBufUtil.hexDump(newNonceHash) + ", but received: " + ByteBufUtil.hexDump(newNonceHash3)));
        }

        return Mono.error(new IllegalStateException("Failed to create an authorization key."));
    }

    private Mono<Void> handleResPQ(ResPQ resPQ) {
        ByteBufAllocator alloc = session.getConnection().channel().alloc();
        session.getAuthContext().setServerNonce(resPQ.serverNonce());
        byte[] pqBytes = resPQ.pq();

        List<Long> fingerprints = resPQ.serverPublicKeyFingerprints();
        long fingerprint = fingerprints.get(0);
        PublicRsaKey key = PublicRsaKey.publicKeys.get(fingerprint);

        BigInteger pq = fromByteArray(pqBytes);
        BigInteger p = BigInteger.valueOf(pqPrimeLeemon(pq.longValueExact()));
        byte[] pBytes = toByteArray(p);
        BigInteger q = pq.divide(p);
        byte[] qBytes = toByteArray(q);

        session.getAuthContext().setNewNonce(random.generateSeed(32));

        if (p.longValueExact() > q.longValueExact()) {
            throw new IllegalStateException("Incorrect prime factorization. p: " + p + ", q: " + q + ", pq: " + pq);
        }

        PQInnerData pqInnerData = PQInnerDataDc.builder()
                .pq(pqBytes)
                .p(pBytes)
                .q(qBytes)
                .nonce(session.getAuthContext().getNonce())
                .serverNonce(session.getAuthContext().getServerNonce())
                .newNonce(session.getAuthContext().getNewNonce())
                .dc(session.getDataCenter().getId())
                .build();

        ByteBuf pqInnderDataBuf = TlSerializer.serialize(alloc, pqInnerData);
        byte[] pqInnerDataBytes = toByteArray(pqInnderDataBuf);
        byte[] hash = sha1Digest(pqInnerDataBytes);
        byte[] seed = random.generateSeed(255 - hash.length - pqInnerDataBytes.length);
        byte[] dataWithHash = concat(hash, pqInnerDataBytes, seed);
        byte[] encrypted = rsaEncrypt(dataWithHash, key);

        return session.sendUnencrypted(ReqDHParams.builder()
                .nonce(session.getAuthContext().getNonce())
                .serverNonce(session.getAuthContext().getServerNonce())
                .encryptedData(encrypted)
                .p(pBytes)
                .q(qBytes)
                .publicKeyFingerprint(fingerprint)
                .build());
    }

    private Mono<Void> handleDhGenOk(DhGenOk dhGenOk) {
        byte[] newNonceHash1 = dhGenOk.newNonceHash1();
        byte[] newNonceHash = substring(sha1Digest(session.getAuthContext().getNewNonce(), new byte[]{1},
                session.getAuthContext().getAuthAuxHash()), 4, 16);

        if (!Arrays.equals(newNonceHash1, newNonceHash)) {
            return Mono.error(() -> new IllegalStateException("New nonce hash mismatch, excepted: "
                    + ByteBufUtil.hexDump(newNonceHash) + ", but received: " + ByteBufUtil.hexDump(newNonceHash1)));
        }

        long serverSalt = readLongLE(xor(substring(session.getAuthContext().getNewNonce(), 0, 8),
                substring(session.getAuthContext().getServerNonce(), 0, 8)));

        session.getAuthContext().setServerSalt(serverSalt);
        session.setServerSalt(serverSalt);

        log.debug("Auth key generation completed!");
        AuthorizationKeyHolder authKey = new AuthorizationKeyHolder(session.getDataCenter(), session.getAuthContext().getAuthKey());
        onAuthSink.emitValue(authKey, Sinks.EmitFailureHandler.FAIL_FAST);

        return session.getClient().getOptions()
                .getResources().getStoreLayout()
                .updateAuthorizationKey(authKey);
    }

    private Mono<Void> handleDhGenRetry(DhGenRetry dhGenRetry) {
        byte[] newNonceHash2 = dhGenRetry.newNonceHash2();
        byte[] newNonceHash = substring(sha1Digest(session.getAuthContext().getNewNonce(), new byte[]{2},
                session.getAuthContext().getAuthAuxHash()), 4, 16);

        if (!Arrays.equals(newNonceHash2, newNonceHash)) {
            return Mono.error(() -> new IllegalStateException("New nonce hash mismatch, excepted: "
                    + ByteBufUtil.hexDump(newNonceHash) + ", but received: " + ByteBufUtil.hexDump(newNonceHash2)));
        }

        ServerDHParams serverDHParams = session.getAuthContext().getServerDHParams();
        log.debug("Retrying dh params extending, attempt: {}.", session.getAuthContext().getRetry());
        return handleServerDHParams(serverDHParams);
    }

    private Mono<Void> handleServerDHParams(ServerDHParams serverDHParams) {
        ByteBufAllocator alloc = session.getConnection().channel().alloc();
        session.getAuthContext().setServerDHParams(serverDHParams); // for DhGenRetry
        byte[] encryptedAnswerB = serverDHParams.encryptedAnswer();

        byte[] tmpAesKey = concat(
                sha1Digest(session.getAuthContext().getNewNonce(), session.getAuthContext().getServerNonce()),
                substring(sha1Digest(session.getAuthContext().getServerNonce(), session.getAuthContext().getNewNonce()), 0, 12));

        byte[] tmpAesIv = concat(concat(substring(
                sha1Digest(session.getAuthContext().getServerNonce(), session.getAuthContext().getNewNonce()), 12, 8),
                sha1Digest(session.getAuthContext().getNewNonce(), session.getAuthContext().getNewNonce())),
                substring(session.getAuthContext().getNewNonce(), 0, 4));

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
        session.getAuthContext().setAuthKey(alignKeyZero(toByteArray(authKeyVal), 256));
        session.getAuthContext().setAuthAuxHash(substring(sha1Digest(session.getAuthContext().getAuthKey()), 0, 8));

        ClientDHInnerData clientDHInnerData = ClientDHInnerData.builder()
                .retryId(session.getAuthContext().getRetry().getAndIncrement())
                .nonce(session.getAuthContext().getNonce())
                .serverNonce(session.getAuthContext().getServerNonce())
                .gB(toByteArray(gb))
                .build();

        byte[] innerData = toByteArray(TlSerializer.serialize(alloc, clientDHInnerData));
        byte[] innerDataWithHash = align(concat(sha1Digest(innerData), innerData), 16);
        byte[] dataWithHashEnc = cipher.encrypt(innerDataWithHash);

        session.updateTimeOffset(serverDHInnerData.serverTime());

        return session.sendUnencrypted(SetClientDHParams.builder()
                .nonce(session.getAuthContext().getNonce())
                .serverNonce(session.getAuthContext().getServerNonce())
                .encryptedData(dataWithHashEnc)
                .build());
    }
}
