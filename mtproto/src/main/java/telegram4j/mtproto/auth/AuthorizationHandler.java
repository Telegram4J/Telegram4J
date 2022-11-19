package telegram4j.mtproto.auth;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.util.AES256IGECipher;
import telegram4j.tl.TlDeserializer;
import telegram4j.tl.TlSerializer;
import telegram4j.tl.api.MTProtoObject;
import telegram4j.tl.mtproto.*;
import telegram4j.tl.request.mtproto.ImmutableReqPqMulti;
import telegram4j.tl.request.mtproto.ReqDHParams;
import telegram4j.tl.request.mtproto.SetClientDHParams;

import java.math.BigInteger;
import java.util.Objects;
import java.util.stream.Collectors;

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
        this.client = Objects.requireNonNull(client);
        this.context = Objects.requireNonNull(context);
        this.onAuthSink = Objects.requireNonNull(onAuthSink);
        this.alloc = Objects.requireNonNull(alloc);
    }

    public MTProtoClient getClient() {
        return client;
    }

    public AuthorizationContext getContext() {
        return context;
    }

    public Mono<Void> start() {
        return Mono.defer(() -> {
            byte[] nonceb = new byte[16];
            random.nextBytes(nonceb);

            ByteBuf nonce = Unpooled.wrappedBuffer(nonceb);
            context.setNonce(nonce);

            return client.sendAuth(ImmutableReqPqMulti.of(nonce));
        });
    }

    public Mono<Void> handle(MTProtoObject obj) {
        switch (obj.identifier()) {
            case ResPQ.ID:
                ResPQ resPQ = (ResPQ) obj;

                return handleResPQ(resPQ);
            case ServerDHParams.ID:
                ServerDHParams serverDHParams = (ServerDHParams) obj;

                return handleServerDHParams(serverDHParams);
            case DhGenOk.ID:
                DhGenOk dhGenOk = (DhGenOk) obj;

                return handleDhGenOk(dhGenOk);
            case DhGenRetry.ID:
                DhGenRetry dhGenRetry = (DhGenRetry) obj;

                return handleDhGenRetry(dhGenRetry);
            case DhGenFail.ID:
                DhGenFail dhGenFail = (DhGenFail) obj;

                return handleDhGenFail(dhGenFail);
            default:
                return emitError("Incorrect MTProto object: " + obj);
        }
    }

    // handling
    // ====================

    private Mono<Void> handleResPQ(ResPQ resPQ) {
        ByteBuf nonce = resPQ.nonce();

        if (!nonce.equals(context.getNonce())) return emitError("Nonce mismatch");

        var fingerprints = resPQ.serverPublicKeyFingerprints();
        var keyTuple = context.getPublicRsaKeyRegister().findAny(fingerprints)
                .orElse(null);

        if (keyTuple == null) {
            return emitError("Unknown server fingerprints: " + fingerprints.stream()
                    .map(Long::toHexString)
                    .collect(Collectors.joining(", ", "[", "]")));
        }

        BigInteger pq = fromByteBuf(resPQ.pq());
        BigInteger p = BigInteger.valueOf(pqFactorize(pq.longValueExact()));
        BigInteger q = pq.divide(p);

        if (p.longValueExact() > q.longValueExact()) {
            return emitError("Invalid factorization result. p: " + p + ", q: " + q + ", pq: " + pq);
        }

        ByteBuf pb = toByteBuf(p);
        ByteBuf qb = toByteBuf(q);

        byte[] newNonceb = new byte[32];
        random.nextBytes(newNonceb);

        ByteBuf newNonce = Unpooled.wrappedBuffer(newNonceb);

        context.setNewNonce(newNonce);
        context.setServerNonce(resPQ.serverNonce());

        PQInnerData pqInnerData = PQInnerDataDc.builder()
                .pq(resPQ.pq())
                .p(pb)
                .q(qb)
                .nonce(nonce)
                .serverNonce(resPQ.serverNonce())
                .newNonce(newNonce)
                .dc(client.getDatacenter().getInternalId())
                .build();

        ByteBuf pqInnerDataBuf = TlSerializer.serialize(alloc, pqInnerData);
        ByteBuf hash = sha1Digest(pqInnerDataBuf);
        byte[] seedb = new byte[255 - hash.readableBytes() - pqInnerDataBuf.readableBytes()];
        random.nextBytes(seedb);
        ByteBuf seed = Unpooled.wrappedBuffer(seedb);
        ByteBuf dataWithHash = Unpooled.wrappedBuffer(hash, pqInnerDataBuf, seed);
        ByteBuf encrypted = rsaEncrypt(dataWithHash, keyTuple.getT2());

        return client.sendAuth(ReqDHParams.builder()
                .nonce(nonce)
                .serverNonce(resPQ.serverNonce())
                .encryptedData(encrypted)
                .p(pb)
                .q(qb)
                .publicKeyFingerprint(keyTuple.getT1())
                .build());
    }

    private Mono<Void> handleServerDHParams(ServerDHParams serverDHParams) {

        if (!serverDHParams.nonce().equals(context.getNonce())) return emitError("Nonce mismatch");
        if (!serverDHParams.serverNonce().equals(context.getServerNonce())) return emitError("Nonce mismatch");

        ByteBuf encryptedAnswer = serverDHParams.encryptedAnswer();
        if (encryptedAnswer.readableBytes() % 16 != 0) {
            return emitError("Encrypted answer size mismatch");
        }

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

        ByteBuf decrypted = decrypter.decrypt(encryptedAnswer)
                .skipBytes(20); // TODO: answer hash
        ServerDHInnerData serverDHInnerData = TlDeserializer.deserialize(decrypted);

        if (decrypted.readableBytes() >= 16) {
            decrypted.release();
            return emitError("Invalid padding");
        }

        decrypted.release();

        if (!serverDHInnerData.nonce().equals(context.getNonce())) return emitError("Nonce mismatch");
        if (!serverDHInnerData.serverNonce().equals(context.getServerNonce())) return emitError("Nonce mismatch");

        BigInteger dhPrime = fromByteBuf(serverDHInnerData.dhPrime());
        // region dh checks
        if (dhPrime.bitLength() != 2048) {
            return emitError("Invalid 2048 dhPrime");
        }

        // TODO: other checks

        byte[] bs = new byte[256];
        random.nextBytes(bs);

        BigInteger b = fromByteArray(bs);
        BigInteger g = BigInteger.valueOf(serverDHInnerData.g());
        BigInteger gb = g.modPow(b, dhPrime);
        BigInteger ga = fromByteBuf(serverDHInnerData.gA());

        BigInteger ubound = dhPrime.subtract(BigInteger.ONE);
        if (g.compareTo(BigInteger.ONE) < 1 || g.compareTo(ubound) >= 0) return emitError("Invalid g");
        if (ga.compareTo(BigInteger.ONE) < 1 || ga.compareTo(ubound) >= 0) return emitError("Invalid gA");
        if (gb.compareTo(BigInteger.ONE) < 1 || gb.compareTo(ubound) >= 0) return emitError("Invalid gB");

        BigInteger safetyRange = BigInteger.TWO.pow(2048 - 64);
        BigInteger usafeBound = dhPrime.subtract(safetyRange);
        if (ga.compareTo(safetyRange) <= 0 || ga.compareTo(usafeBound) > 0) return emitError("Invalid gA range");
        if (gb.compareTo(safetyRange) <= 0 || gb.compareTo(usafeBound) > 0) return emitError("Invalid gB range");

        // endregion

        BigInteger authKey = ga.modPow(b, dhPrime);

        context.setServerTime(serverDHInnerData.serverTime());
        context.setAuthKey(alignKeyZero(toByteBuf(authKey), 256));
        context.setAuthAuxHash(sha1Digest(context.getAuthKey()).slice(0, 8));

        context.setServerSalt(Long.reverseBytes(context.getNewNonce().getLong(0) ^ context.getServerNonce().getLong(1)));

        ByteBuf nonce = context.getNonce();
        ByteBuf serverNonce = context.getServerNonce();

        ClientDHInnerData clientDHInnerData = ClientDHInnerData.builder()
                .retryId(context.getRetry().getAndIncrement())
                .nonce(nonce)
                .serverNonce(serverNonce)
                .gB(toByteBuf(gb))
                .build();

        ByteBuf innerData = TlSerializer.serialize(alloc, clientDHInnerData);
        ByteBuf innerDataWithHash = align(Unpooled.wrappedBuffer(sha1Digest(innerData), innerData), 16);

        AES256IGECipher encrypter = new AES256IGECipher(true, aesKey, tmpAesIv);
        ByteBuf dataWithHashEnc = encrypter.encrypt(innerDataWithHash);

        var req = SetClientDHParams.builder()
                .nonce(nonce)
                .serverNonce(serverNonce)
                .encryptedData(dataWithHashEnc)
                .build();

        dataWithHashEnc.release();

        return client.sendAuth(req);
    }

    private Mono<Void> handleDhGenOk(DhGenOk dhGenOk) {
        ByteBuf newNonceHash = sha1Digest(context.getNewNonce(), Unpooled.wrappedBuffer(new byte[]{1}),
                context.getAuthAuxHash()).slice(4, 16);

        if (!dhGenOk.nonce().equals(context.getNonce())) return emitError("Nonce mismatch");
        if (!dhGenOk.serverNonce().equals(context.getServerNonce())) return emitError("Server nonce mismatch");
        if (!dhGenOk.newNonceHash1().equals(newNonceHash)) return emitError("New nonce hash 1 mismatch");

        AuthorizationKeyHolder authKey = new AuthorizationKeyHolder(context.getAuthKey().retain());
        onAuthSink.emitValue(authKey, Sinks.EmitFailureHandler.FAIL_FAST);
        return Mono.empty();
    }

    private Mono<Void> handleDhGenRetry(DhGenRetry dhGenRetry) {
        ByteBuf newNonceHash = sha1Digest(context.getNewNonce(), Unpooled.wrappedBuffer(new byte[]{2}), context.getAuthAuxHash())
                .slice(4, 16);

        if (!dhGenRetry.nonce().equals(context.getNonce())) return emitError("Nonce mismatch");
        if (!dhGenRetry.serverNonce().equals(context.getServerNonce())) return emitError("Nonce mismatch");
        if (!dhGenRetry.newNonceHash2().equals(newNonceHash)) return emitError("New nonce hash 2 mismatch");

        ServerDHParams serverDHParams = context.getServerDHParams();
        log.debug("Retrying dh params extending, attempt: {}", context.getRetry());
        return handleServerDHParams(serverDHParams);
    }

    private Mono<Void> handleDhGenFail(DhGenFail dhGenFail) {
        ByteBuf newNonceHash = sha1Digest(context.getNewNonce(), Unpooled.wrappedBuffer(new byte[]{3}), context.getAuthAuxHash())
                .slice(4, 16);

        if (!dhGenFail.nonce().equals(context.getNonce())) return emitError("Nonce mismatch");
        if (!dhGenFail.serverNonce().equals(context.getServerNonce())) return emitError("Nonce mismatch");
        if (!dhGenFail.newNonceHash3().equals(newNonceHash)) return emitError("New nonce hash 3 mismatch");

        return emitError("Failed to create an authorization key");
    }

    private Mono<Void> emitError(String message) {
        return Mono.fromRunnable(() -> onAuthSink.emitError(new AuthorizationException(message),
                Sinks.EmitFailureHandler.FAIL_FAST));
    }
}
