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
import java.util.Objects;
import java.util.stream.Collectors;

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
        this.client = Objects.requireNonNull(client);
        this.context = Objects.requireNonNull(context);
        this.onAuthSink = Objects.requireNonNull(onAuthSink);
        this.alloc = Objects.requireNonNull(alloc);
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
                return Mono.fromRunnable(() -> onAuthSink.emitError(
                        new AuthorizationException("Incorrect MTProto object: 0x" + Integer.toHexString(obj.identifier())),
                        Sinks.EmitFailureHandler.FAIL_FAST));
        }
    }

    private Mono<Void> handleResPQ(ResPQ resPQ) {
        ByteBuf serverNonce = resPQ.serverNonce();
        ByteBuf nonce = resPQ.nonce();

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
                    new AuthorizationException("Unknown server fingerprints: " + fingerprints.stream()
                            .map(Long::toHexString)
                            .collect(Collectors.joining(", ", "[", "]"))),
                    Sinks.EmitFailureHandler.FAIL_FAST));
        }

        BigInteger pq = fromByteBuf(resPQ.pq());
        BigInteger p = BigInteger.valueOf(pqFactorize(pq.longValueExact()));
        BigInteger q = pq.divide(p);

        ByteBuf pb = toByteBuf(p);
        ByteBuf qb = toByteBuf(q);

        if (p.longValueExact() > q.longValueExact()) {
            return Mono.fromRunnable(() -> onAuthSink.emitError(
                    new AuthorizationException("Invalid factorization result. p: "
                    + p + ", q: " + q + ", pq: " + pq),
                    Sinks.EmitFailureHandler.FAIL_FAST));
        }

        byte[] newNonceb = new byte[32];
        random.nextBytes(newNonceb);

        ByteBuf newNonce = Unpooled.wrappedBuffer(newNonceb);

        context.setNewNonce(newNonce);
        context.setServerNonce(serverNonce);

        PQInnerData pqInnerData = PQInnerDataDc.builder()
                .pq(resPQ.pq())
                .p(pb)
                .q(qb)
                .nonce(nonce)
                .serverNonce(serverNonce)
                .newNonce(newNonce)
                .dc(client.getDatacenter().getInternalId())
                .build();

        ByteBuf pqInnerDataBuf = TlSerializer.serialize(alloc, pqInnerData);
        ByteBuf hash = sha1Digest(pqInnerDataBuf);
        byte[] seedb = new byte[255 - hash.readableBytes() - pqInnerDataBuf.readableBytes()];
        random.nextBytes(seedb);
        ByteBuf seed = Unpooled.wrappedBuffer(seedb);
        ByteBuf dataWithHash = Unpooled.wrappedBuffer(hash, pqInnerDataBuf, seed);
        ByteBuf encrypted = rsaEncrypt(dataWithHash, key);

        return client.sendAuth(ReqDHParams.builder()
                .nonce(nonce)
                .serverNonce(serverNonce)
                .encryptedData(encrypted)
                .p(pb)
                .q(qb)
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
                .skipBytes(20); // TODO: answer hash

        ServerDHInnerData serverDHInnerData = TlDeserializer.deserialize(answer);
        answer.release();

        byte[] bs = new byte[256];
        random.nextBytes(bs);

        BigInteger b = fromByteArray(bs);
        BigInteger g = BigInteger.valueOf(serverDHInnerData.g());
        BigInteger dhPrime = fromByteBuf(serverDHInnerData.dhPrime());
        BigInteger gb = g.modPow(b, dhPrime);

        BigInteger authKey = fromByteBuf(serverDHInnerData.gA()).modPow(b, dhPrime);

        context.setAuthKey(alignKeyZero(toByteBuf(authKey), 256));
        context.setAuthAuxHash(sha1Digest(context.getAuthKey()).slice(0, 8));

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

        client.updateTimeOffset(serverDHInnerData.serverTime());

        var req = SetClientDHParams.builder()
                .nonce(nonce)
                .serverNonce(serverNonce)
                .encryptedData(dataWithHashEnc)
                .build();

        dataWithHashEnc.release();

        return client.sendAuth(req);
    }

    private Mono<Void> handleDhGenOk(DhGenOk dhGenOk) {
        ByteBuf newNonceHash = sha1Digest(context.getNewNonce(), DH_GEN_OK,
                context.getAuthAuxHash()).slice(4, 16);

        if (!dhGenOk.newNonceHash1().equals(newNonceHash)) {
            return Mono.fromRunnable(() -> onAuthSink.emitError(new AuthorizationException("New nonce hash 1 mismatch, excepted: "
                            + ByteBufUtil.hexDump(newNonceHash) + ", but received: "
                            + ByteBufUtil.hexDump(dhGenOk.newNonceHash1())),
                    Sinks.EmitFailureHandler.FAIL_FAST));
        }

        // TODO: can we just read 2 longs and XOR them?
        ByteBuf xorBuf = xor(context.getNewNonce().slice(0, 8),
                context.getServerNonce().slice(0, 8));
        long serverSalt = xorBuf.readLongLE();

        context.setServerSalt(serverSalt);

        AuthorizationKeyHolder authKey = new AuthorizationKeyHolder(context.getAuthKey().retain());
        onAuthSink.emitValue(authKey, Sinks.EmitFailureHandler.FAIL_FAST);
        return Mono.empty();
    }

    private Mono<Void> handleDhGenRetry(DhGenRetry dhGenRetry) {
        ByteBuf newNonceHash = sha1Digest(context.getNewNonce(), DH_GEN_RETRY, context.getAuthAuxHash())
                .slice(4, 16);

        if (!dhGenRetry.newNonceHash2().equals(newNonceHash)) {
            return Mono.fromRunnable(() -> onAuthSink.emitError(new AuthorizationException("New nonce hash 2 mismatch, excepted: "
                            + ByteBufUtil.hexDump(newNonceHash) + ", but received: "
                            + ByteBufUtil.hexDump(dhGenRetry.newNonceHash2())),
                    Sinks.EmitFailureHandler.FAIL_FAST));
        }

        ServerDHParams serverDHParams = context.getServerDHParams();
        log.debug("Retrying dh params extending, attempt: {}", context.getRetry());
        return handleServerDHParams(serverDHParams);
    }

    private Mono<Void> handleDhGenFail(DhGenFail dhGenFail) {
        ByteBuf newNonceHash = sha1Digest(context.getNewNonce(), DH_GEN_FAIL, context.getAuthAuxHash())
                .slice(4, 16);

        if (!dhGenFail.newNonceHash3().equals(newNonceHash)) {
            return Mono.fromRunnable(() -> onAuthSink.emitError(new AuthorizationException("New nonce hash 3 mismatch, excepted: "
                            + ByteBufUtil.hexDump(newNonceHash) + ", but received: "
                            + ByteBufUtil.hexDump(dhGenFail.newNonceHash3())),
                    Sinks.EmitFailureHandler.FAIL_FAST));
        }

        return Mono.fromRunnable(() -> onAuthSink.emitError(
                new AuthorizationException("Failed to create an authorization key"),
                Sinks.EmitFailureHandler.FAIL_FAST));
    }
}
