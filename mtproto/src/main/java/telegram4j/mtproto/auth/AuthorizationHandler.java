package telegram4j.mtproto.auth;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.PublicRsaKey;
import telegram4j.mtproto.util.AES256IGECipher;
import telegram4j.tl.TlDeserializer;
import telegram4j.tl.TlSerializer;
import telegram4j.tl.api.MTProtoObject;
import telegram4j.tl.mtproto.*;
import telegram4j.tl.request.mtproto.ImmutableReqPqMulti;
import telegram4j.tl.request.mtproto.ReqDHParams;
import telegram4j.tl.request.mtproto.SetClientDHParams;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static telegram4j.mtproto.util.CryptoUtil.*;

public final class AuthorizationHandler {

    private static final Logger log = Loggers.getLogger(AuthorizationHandler.class);

    private final MTProtoClient client;
    private final AuthorizationContext context;
    private final Sinks.One<AuthorizationKeyHolder> onAuthSink;
    private final ByteBufAllocator alloc;

    public AuthorizationHandler(MTProtoClient client, AuthorizationContext context,
                                Sinks.One<AuthorizationKeyHolder> onAuthSink,
                                ByteBufAllocator alloc) {
        this.client = client;
        this.context = context;
        this.onAuthSink = onAuthSink;
        this.alloc = alloc;
    }

    public Mono<Void> start() {
        return Mono.defer(() -> {
            log.debug("Auth key generation started!");

            byte[] nonce = random.generateSeed(16);
            context.setNonce(nonce);

            return client.send(ImmutableReqPqMulti.of(nonce));
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
            return handleDhGenFail(dhGenFail);
        }

        return Mono.fromRunnable(() -> onAuthSink.emitError(
                new AuthorizationException("Incorrect MTProto object: 0x" + Integer.toHexString(obj.identifier())),
                Sinks.EmitFailureHandler.FAIL_FAST));
    }

    private Mono<Void> handleDhGenFail(DhGenFail dhGenFail) {
        byte[] newNonceHash3 = dhGenFail.newNonceHash3();
        byte[] newNonceHash = substring(sha1Digest(context.getNewNonce(), new byte[]{3},
                context.getAuthAuxHash()), 4, 16);

        if (!Arrays.equals(newNonceHash3, newNonceHash)) {
            return Mono.fromRunnable(() -> onAuthSink.emitError(new AuthorizationException("New nonce hash mismatch, excepted: "
                            + ByteBufUtil.hexDump(newNonceHash) + ", but received: " + ByteBufUtil.hexDump(newNonceHash3)),
                    Sinks.EmitFailureHandler.FAIL_FAST));
        }

        return Mono.fromRunnable(() -> onAuthSink.emitError(
                new AuthorizationException("Failed to create an authorization key."),
                Sinks.EmitFailureHandler.FAIL_FAST));
    }

    private Mono<Void> handleResPQ(ResPQ resPQ) {
        context.setServerNonce(resPQ.serverNonce());
        byte[] pqBytes = resPQ.pq();

        List<Long> fingerprints = resPQ.serverPublicKeyFingerprints();

        long fingerprint = -1;
        PublicRsaKey key = null;
        for (long l : fingerprints) {
            PublicRsaKey k = PublicRsaKey.publicKeys.get(l);
            if (k != null) {
                fingerprint = l;
                key = k;
                break;
            }
        }

        if (fingerprint == -1) {
            return Mono.fromRunnable(() -> onAuthSink.emitError(
                    new AuthorizationException("Unknown server fingerprints: " + fingerprints),
                    Sinks.EmitFailureHandler.FAIL_FAST));
        }

        BigInteger pq = fromByteArray(pqBytes);
        BigInteger p = BigInteger.valueOf(pqPrimeLeemon(pq.longValueExact()));
        byte[] pBytes = toByteArray(p);
        BigInteger q = pq.divide(p);
        byte[] qBytes = toByteArray(q);

        context.setNewNonce(random.generateSeed(32));

        if (p.longValueExact() > q.longValueExact()) {
            return Mono.fromRunnable(() -> onAuthSink.emitError(
                    new AuthorizationException("Incorrect prime factorization. p: "
                    + p + ", q: " + q + ", pq: " + pq),
                    Sinks.EmitFailureHandler.FAIL_FAST));
        }

        PQInnerData pqInnerData = PQInnerDataDc.builder()
                .pq(pqBytes)
                .p(pBytes)
                .q(qBytes)
                .nonce(context.getNonce())
                .serverNonce(context.getServerNonce())
                .newNonce(context.getNewNonce())
                .dc(client.getDatacenter().getId())
                .build();

        ByteBuf pqInnderDataBuf = TlSerializer.serialize(alloc, pqInnerData);
        byte[] pqInnerDataBytes = toByteArray(pqInnderDataBuf);
        byte[] hash = sha1Digest(pqInnerDataBytes);
        byte[] seed = random.generateSeed(255 - hash.length - pqInnerDataBytes.length);
        byte[] dataWithHash = concat(hash, pqInnerDataBytes, seed);
        byte[] encrypted = rsaEncrypt(dataWithHash, key);

        return client.send(ReqDHParams.builder()
                .nonce(context.getNonce())
                .serverNonce(context.getServerNonce())
                .encryptedData(encrypted)
                .p(pBytes)
                .q(qBytes)
                .publicKeyFingerprint(fingerprint)
                .build());
    }

    private Mono<Void> handleDhGenOk(DhGenOk dhGenOk) {
        byte[] newNonceHash1 = dhGenOk.newNonceHash1();
        byte[] newNonceHash = substring(sha1Digest(context.getNewNonce(), new byte[]{1},
                context.getAuthAuxHash()), 4, 16);

        if (!Arrays.equals(newNonceHash1, newNonceHash)) {
            return Mono.fromRunnable(() -> onAuthSink.emitError(
                    new AuthorizationException("New nonce hash mismatch, excepted: "
                    + ByteBufUtil.hexDump(newNonceHash) + ", but received: " + ByteBufUtil.hexDump(newNonceHash1)),
                    Sinks.EmitFailureHandler.FAIL_FAST));
        }

        long serverSalt = readLongLE(xor(substring(context.getNewNonce(), 0, 8),
                substring(context.getServerNonce(), 0, 8)));

        context.setServerSalt(serverSalt);

        log.debug("Auth key generation completed!");
        AuthorizationKeyHolder authKey = new AuthorizationKeyHolder(client.getDatacenter(), context.getAuthKey());
        onAuthSink.emitValue(authKey, Sinks.EmitFailureHandler.FAIL_FAST);
        return Mono.empty();
    }

    private Mono<Void> handleDhGenRetry(DhGenRetry dhGenRetry) {
        byte[] newNonceHash2 = dhGenRetry.newNonceHash2();
        byte[] newNonceHash = substring(sha1Digest(context.getNewNonce(), new byte[]{2},
                context.getAuthAuxHash()), 4, 16);

        if (!Arrays.equals(newNonceHash2, newNonceHash)) {
            return Mono.fromRunnable(() -> onAuthSink.emitError(
                    new AuthorizationException("New nonce hash mismatch, excepted: "
                    + ByteBufUtil.hexDump(newNonceHash) + ", but received: " + ByteBufUtil.hexDump(newNonceHash2)),
                    Sinks.EmitFailureHandler.FAIL_FAST));
        }

        ServerDHParams serverDHParams = context.getServerDHParams();
        log.debug("Retrying dh params extending, attempt: {}.", context.getRetry());
        return handleServerDHParams(serverDHParams);
    }

    private Mono<Void> handleServerDHParams(ServerDHParams serverDHParams) {
        context.setServerDHParams(serverDHParams); // for DhGenRetry
        byte[] encryptedAnswerB = serverDHParams.encryptedAnswer();

        byte[] tmpAesKey = concat(
                sha1Digest(context.getNewNonce(), context.getServerNonce()),
                substring(sha1Digest(context.getServerNonce(), context.getNewNonce()), 0, 12));

        byte[] tmpAesIv = concat(concat(substring(
                sha1Digest(context.getServerNonce(), context.getNewNonce()), 12, 8),
                sha1Digest(context.getNewNonce(), context.getNewNonce())),
                substring(context.getNewNonce(), 0, 4));

        AES256IGECipher decrypter = new AES256IGECipher(false, tmpAesKey, tmpAesIv);
        ByteBuf answer = Unpooled.wrappedBuffer(decrypter.decrypt(encryptedAnswerB))
                .skipBytes(20); // answer hash

        ServerDHInnerData serverDHInnerData = TlDeserializer.deserialize(answer);
        answer.release();

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

        AES256IGECipher encrypter = new AES256IGECipher(true, tmpAesKey, tmpAesIv);
        byte[] dataWithHashEnc = encrypter.encrypt(innerDataWithHash);

        client.updateTimeOffset(serverDHInnerData.serverTime());

        return client.send(SetClientDHParams.builder()
                .nonce(context.getNonce())
                .serverNonce(context.getServerNonce())
                .encryptedData(dataWithHashEnc)
                .build());
    }
}
