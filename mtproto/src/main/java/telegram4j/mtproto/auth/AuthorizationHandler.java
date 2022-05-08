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

import static telegram4j.mtproto.util.CryptoUtil.*;

public final class AuthorizationHandler {

    private static final Logger log = Loggers.getLogger(AuthorizationHandler.class);

    private static final ByteBuf DH_GEN_OK = Unpooled.wrappedBuffer(new byte[]{1});
    private static final ByteBuf DH_GEN_RETRY = Unpooled.wrappedBuffer(new byte[]{2});
    private static final ByteBuf DH_GEN_FAIL = Unpooled.wrappedBuffer(new byte[]{3});

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
            byte[] nonce = random.generateSeed(16);
            context.setNonce(Unpooled.wrappedBuffer(nonce));

            return client.sendAuth(ImmutableReqPqMulti.of(nonce));
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
                    .then(Mono.error(new AuthorizationException("Dh gen retry")));
        }

        if (obj instanceof DhGenFail) {
            DhGenFail dhGenFail = (DhGenFail) obj;
            return handleDhGenFail(dhGenFail);
        }

        return Mono.fromRunnable(() -> onAuthSink.emitError(
                new AuthorizationException("Incorrect MTProto object: 0x" + Integer.toHexString(obj.identifier())),
                Sinks.EmitFailureHandler.FAIL_FAST));
    }

    private Mono<Void> handleResPQ(ResPQ resPQ) {
        byte[] serverNonce = resPQ.serverNonce();
        byte[] nonce = resPQ.nonce();
        byte[] pqBytes = resPQ.pq();

        context.setServerNonce(Unpooled.wrappedBuffer(serverNonce));

        var fingerprints = resPQ.serverPublicKeyFingerprints();
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
        BigInteger q = pq.divide(p);
        byte[] pBytes = toByteArray(p);
        byte[] qBytes = toByteArray(q);

        byte[] newNonce = random.generateSeed(32);
        context.setNewNonce(Unpooled.wrappedBuffer(newNonce));

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
                .nonce(nonce)
                .serverNonce(serverNonce)
                .newNonce(newNonce)
                .dc(client.getDatacenter().getId())
                .build();

        ByteBuf pqInnerDataBuf = TlSerializer.serialize(alloc, pqInnerData);
        ByteBuf hash = sha1Digest(pqInnerDataBuf);
        ByteBuf seed = Unpooled.wrappedBuffer(random.generateSeed(
                255 - hash.readableBytes() - pqInnerDataBuf.readableBytes()));
        ByteBuf dataWithHash = Unpooled.wrappedBuffer(hash, pqInnerDataBuf, seed);
        ByteBuf encrypted = rsaEncrypt(dataWithHash, key);

        return client.sendAuth(ReqDHParams.builder()
                .nonce(nonce)
                .serverNonce(serverNonce)
                .encryptedData(toByteArray(encrypted))
                .p(pBytes)
                .q(qBytes)
                .publicKeyFingerprint(fingerprint)
                .build());
    }

    private Mono<Void> handleServerDHParams(ServerDHParams serverDHParams) {
        context.setServerDHParams(serverDHParams); // for DhGenRetry

        ByteBuf serverNonceAndNewNonceSha1 = sha1Digest(context.getServerNonce(), context.getNewNonce());
        ByteBuf tmpAesKey = Unpooled.wrappedBuffer(
                sha1Digest(context.getNewNonce(), context.getServerNonce()),
                serverNonceAndNewNonceSha1.retainedSlice(0, 12));

        ByteBuf tmpAesIv = Unpooled.wrappedBuffer(
                serverNonceAndNewNonceSha1.slice(12, 8),
                sha1Digest(context.getNewNonce(), context.getNewNonce()),
                context.getNewNonce().retainedSlice(0, 4));

        byte[] aesKey = toByteArray(tmpAesKey);

        AES256IGECipher decrypter = new AES256IGECipher(false, aesKey, tmpAesIv.retain());

        ByteBuf encryptedAnswer = Unpooled.wrappedBuffer(serverDHParams.encryptedAnswer());
        ByteBuf answer = decrypter.decrypt(encryptedAnswer)
                .skipBytes(20); // answer hash

        ServerDHInnerData serverDHInnerData = TlDeserializer.deserialize(answer);
        answer.release();

        BigInteger b = fromByteArray(random.generateSeed(256));
        BigInteger g = BigInteger.valueOf(serverDHInnerData.g());
        BigInteger dhPrime = fromByteArray(serverDHInnerData.dhPrime());
        BigInteger gb = g.modPow(b, dhPrime);

        BigInteger authKey = fromByteArray(serverDHInnerData.gA()).modPow(b, dhPrime);

        context.setAuthKey(alignKeyZero(toByteBuf(authKey), 256));
        context.setAuthAuxHash(sha1Digest(context.getAuthKey()).slice(0, 8));

        byte[] nonce = toByteArray(context.getNonce().retain());
        byte[] serverNonce = toByteArray(context.getServerNonce().retain());

        ClientDHInnerData clientDHInnerData = ClientDHInnerData.builder()
                .retryId(context.getRetry().getAndIncrement())
                .nonce(nonce)
                .serverNonce(serverNonce)
                .gB(toByteArray(gb))
                .build();

        ByteBuf innerData = TlSerializer.serialize(alloc, clientDHInnerData);
        ByteBuf innerDataWithHash = align(Unpooled.wrappedBuffer(sha1Digest(innerData), innerData), 16);

        AES256IGECipher encrypter = new AES256IGECipher(true, aesKey, tmpAesIv);
        byte[] dataWithHashEnc = toByteArray(encrypter.encrypt(innerDataWithHash));

        client.updateTimeOffset(serverDHInnerData.serverTime());

        return client.sendAuth(SetClientDHParams.builder()
                .nonce(nonce)
                .serverNonce(serverNonce)
                .encryptedData(dataWithHashEnc)
                .build());
    }

    private Mono<Void> handleDhGenOk(DhGenOk dhGenOk) {
        ByteBuf newNonceHash1 = Unpooled.wrappedBuffer(dhGenOk.newNonceHash1());
        ByteBuf newNonceHash = sha1Digest(context.getNewNonce(), DH_GEN_OK,
                context.getAuthAuxHash()).slice(4, 16);

        if (!newNonceHash1.equals(newNonceHash)) {
            return Mono.fromRunnable(() -> {
                String newNonceHash1Dump = ByteBufUtil.hexDump(newNonceHash1);
                String newNonceHashDump = ByteBufUtil.hexDump(newNonceHash);
                newNonceHash1.release();
                newNonceHash.release();

                onAuthSink.emitError(new AuthorizationException("New nonce hash mismatch, excepted: "
                                + newNonceHashDump + ", but received: " + newNonceHash1Dump),
                        Sinks.EmitFailureHandler.FAIL_FAST);
            });
        }

        newNonceHash1.release();
        newNonceHash.release();

        ByteBuf xorBuf = xor(context.getNewNonce().slice(0, 8),
                context.getServerNonce().slice(0, 8));
        long serverSalt = xorBuf.readLongLE();
        xorBuf.release();

        context.setServerSalt(serverSalt);

        AuthorizationKeyHolder authKey = new AuthorizationKeyHolder(client.getDatacenter(), context.getAuthKey().retain());
        onAuthSink.emitValue(authKey, Sinks.EmitFailureHandler.FAIL_FAST);
        return Mono.empty();
    }

    private Mono<Void> handleDhGenRetry(DhGenRetry dhGenRetry) {
        ByteBuf newNonceHash2 = Unpooled.wrappedBuffer(dhGenRetry.newNonceHash2());
        ByteBuf newNonceHash = sha1Digest(context.getNewNonce(), DH_GEN_RETRY,
                context.getAuthAuxHash()).slice(4, 16);

        if (!newNonceHash2.equals(newNonceHash)) {
            return Mono.fromRunnable(() -> {
                String newNonceHash2Dump = ByteBufUtil.hexDump(newNonceHash2);
                String newNonceHashDump = ByteBufUtil.hexDump(newNonceHash);
                newNonceHash2.release();
                newNonceHash.release();

                onAuthSink.emitError(new AuthorizationException("New nonce hash mismatch, excepted: "
                                + newNonceHashDump + ", but received: " + newNonceHash2Dump),
                        Sinks.EmitFailureHandler.FAIL_FAST);
            });
        }

        newNonceHash2.release();
        newNonceHash.release();

        ServerDHParams serverDHParams = context.getServerDHParams();
        log.debug("Retrying dh params extending, attempt: {}", context.getRetry());
        return handleServerDHParams(serverDHParams);
    }

    private Mono<Void> handleDhGenFail(DhGenFail dhGenFail) {
        ByteBuf newNonceHash3 = Unpooled.wrappedBuffer(dhGenFail.newNonceHash3());
        ByteBuf newNonceHash = sha1Digest(context.getNewNonce(), DH_GEN_FAIL,
                context.getAuthAuxHash()).slice(4, 16);

        if (!newNonceHash3.equals(newNonceHash)) {
            return Mono.fromRunnable(() -> {
                String newNonceHash3Dump = ByteBufUtil.hexDump(newNonceHash3);
                String newNonceHashDump = ByteBufUtil.hexDump(newNonceHash);
                newNonceHash3.release();
                newNonceHash.release();

                onAuthSink.emitError(new AuthorizationException("New nonce hash mismatch, excepted: "
                                + newNonceHashDump + ", but received: " + newNonceHash3Dump),
                        Sinks.EmitFailureHandler.FAIL_FAST);
            });
        }

        newNonceHash3.release();
        newNonceHash.release();

        return Mono.fromRunnable(() -> onAuthSink.emitError(
                new AuthorizationException("Failed to create an authorization key"),
                Sinks.EmitFailureHandler.FAIL_FAST));
    }
}
