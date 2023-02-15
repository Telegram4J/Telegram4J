package telegram4j.mtproto.auth;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.mtproto.MTProtoException;
import telegram4j.mtproto.PublicRsaKey;
import telegram4j.mtproto.client.MTProtoClient;
import telegram4j.mtproto.util.AES256IGECipher;
import telegram4j.mtproto.util.CryptoUtil;
import telegram4j.tl.TlDeserializer;
import telegram4j.tl.TlSerializer;
import telegram4j.tl.api.MTProtoObject;
import telegram4j.tl.mtproto.*;
import telegram4j.tl.request.mtproto.ImmutableReqPqMulti;
import telegram4j.tl.request.mtproto.ImmutableSetClientDHParams;
import telegram4j.tl.request.mtproto.ReqDHParams;

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
        return switch (obj.identifier()) {
            case ResPQ.ID -> {
                var resPQ = (ResPQ) obj;
                yield handleResPQ(resPQ);
            }
            case ServerDHParams.ID -> {
                var serverDHParams = (ServerDHParams) obj;
                yield handleServerDHParams(serverDHParams);
            }
            case DhGenOk.ID -> {
                var dhGenOk = (DhGenOk) obj;
                yield handleDhGenOk(dhGenOk);
            }
            case DhGenRetry.ID -> {
                var dhGenRetry = (DhGenRetry) obj;
                yield handleDhGenRetry(dhGenRetry);
            }
            case DhGenFail.ID -> {
                var dhGenFail = (DhGenFail) obj;
                yield handleDhGenFail(dhGenFail);
            }
            default -> emitError("Incorrect MTProto object: " + obj);
        };
    }

    // handling
    // ====================

    private Mono<Void> handleResPQ(ResPQ resPQ) {
        ByteBuf nonce = resPQ.nonce();

        if (!nonce.equals(context.getNonce())) return emitError("Nonce mismatch");

        var fingerprints = resPQ.serverPublicKeyFingerprints();
        var foundKey = context.getPublicRsaKeyRegister().findAny(fingerprints)
                .orElse(null);

        if (foundKey == null) {
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
        ByteBuf encryptedData = rsa(pqInnerDataBuf, foundKey.key());

        return client.sendAuth(ReqDHParams.builder()
                .nonce(nonce)
                .serverNonce(resPQ.serverNonce())
                .encryptedData(encryptedData)
                .p(pb)
                .q(qb)
                .publicKeyFingerprint(foundKey.fingerprint())
                .build());
    }

    private static ByteBuf rsa(ByteBuf data, PublicRsaKey key) {
        ByteBuf hash = sha1Digest(data);
        byte[] paddingb = new byte[255 - hash.readableBytes() - data.readableBytes()];
        random.nextBytes(paddingb);
        ByteBuf dataWithHash = Unpooled.wrappedBuffer(hash, data, Unpooled.wrappedBuffer(paddingb));
        BigInteger x = fromByteBuf(dataWithHash);
        BigInteger result = x.modPow(key.getExponent(), key.getModulus());
        return toByteBuf(result);
    }

    // Adapted version of:
    // https://github.com/andrew-ld/LL-mtproto/blob/217d27ac04151c085dcf0a2173f9a868e97e4cec/ll_mtproto/crypto/public_rsa.py#L86
    // FIXME: it's not working right now; don't know what's wrong yet
    private static ByteBuf rsePad(ByteBuf data, PublicRsaKey key) {
        if (data.readableBytes() > 144)
            throw new MTProtoException("Plain data length is more that 144 bytes");

        byte[] padding = new byte[192 - data.readableBytes()];
        random.nextBytes(padding);
        ByteBuf dataWithPadding = Unpooled.wrappedBuffer(data, Unpooled.wrappedBuffer(padding));
        ByteBuf dataPadReversed = CryptoUtil.reverse(dataWithPadding);

        final byte[] zeroIv = new byte[32];

        for (;;) {
            byte[] tempKeyBytes = new byte[32];
            random.nextBytes(tempKeyBytes);
            AES256IGECipher cipher = new AES256IGECipher(true,
                    tempKeyBytes, Unpooled.wrappedBuffer(zeroIv));
            var tempKey = Unpooled.wrappedBuffer(tempKeyBytes);

            ByteBuf dataWithHash = Unpooled.wrappedBuffer(dataPadReversed.retain(),
                    sha256Digest(tempKey, dataWithPadding));
            ByteBuf aesEncrypted = cipher.encrypt(dataWithHash);
            ByteBuf tempKeyXor = xor(tempKey, sha256Digest(aesEncrypted));
            ByteBuf keyAesEncrypted = Unpooled.wrappedBuffer(tempKeyXor, aesEncrypted);

            BigInteger x = fromByteBuf(keyAesEncrypted);
            if (x.compareTo(key.getModulus()) >= 0) {
                continue;
            }

            BigInteger result = x.modPow(key.getExponent(), key.getModulus());
            return toByteBuf(result);
        }
    }

    private Mono<Void> handleServerDHParams(ServerDHParams serverDHParams) {
        if (!serverDHParams.nonce().equals(context.getNonce())) return emitError("Nonce mismatch");
        if (!serverDHParams.serverNonce().equals(context.getServerNonce())) return emitError("Server nonce mismatch");

        ByteBuf encryptedAnswer = serverDHParams.encryptedAnswer();
        if (encryptedAnswer.readableBytes() % 16 != 0) {
            return emitError("encryptedAnswer size mismatch");
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

        ByteBuf decrypted = decrypter.decrypt(encryptedAnswer);
        int answerSize = decrypted.readableBytes();
        ByteBuf hash = decrypted.readSlice(20);
        ServerDHInnerData serverDHInnerData = TlDeserializer.deserialize(decrypted);

        int pad = decrypted.readableBytes();
        if (pad >= 16) {
            decrypted.release();
            return emitError("Too big padding for encryptedAnswer");
        }

        int dhInnerDataSize = answerSize - pad - 20;
        if (!hash.equals(sha1Digest(decrypted.slice(20, dhInnerDataSize)))) return emitError("SHA1 answerHash mismatch");

        decrypted.release();

        if (!serverDHInnerData.nonce().equals(context.getNonce())) return emitError("Nonce mismatch");
        if (!serverDHInnerData.serverNonce().equals(context.getServerNonce())) return emitError("Server nonce mismatch");

        BigInteger dhPrime = fromByteBuf(serverDHInnerData.dhPrime());
        // region dh checks
        if (dhPrime.bitLength() != 2048) {
            return emitError("dhPrime is not 2048-bit number");
        }

        // g generates a cyclic subgroup of prime order (p - 1) / 2, i.e. is a quadratic residue mod p.
        // Since g is always equal to 2, 3, 4, 5, 6 or 7, this is easily done using quadratic reciprocity law,
        // yielding a simple condition on
        // * p mod 4g - namely, p mod 8 = 7 for g = 2; p mod 3 = 2 for g = 3;
        // * no extra condition for g = 4;
        // * p mod 5 = 1 or 4 for g = 5;
        // * p mod 24 = 19 or 23 for g = 6;
        // * p mod 7 = 3, 5 or 6 for g = 7.

        boolean modOk = switch (serverDHInnerData.g()) {
            case 2 -> dhPrime.remainder(BigInteger.valueOf(8)).equals(BigInteger.valueOf(7));
            case 3 -> dhPrime.remainder(BigInteger.valueOf(3)).equals(BigInteger.TWO);
            case 4 -> true;
            case 5 -> {
                long remainder = dhPrime.remainder(BigInteger.valueOf(5)).longValueExact();
                yield remainder == 1 || remainder == 4;
            }
            case 6 -> {
                long remainder = dhPrime.remainder(BigInteger.valueOf(24)).longValueExact();
                yield remainder == 19 || remainder == 23;
            }
            case 7 -> {
                long remainder = dhPrime.remainder(BigInteger.valueOf(7)).longValueExact();
                yield remainder == 3 || remainder == 5 || remainder == 6;
            }
            default -> false;
        };

        if (!modOk) {
            return emitError("Bad dhPrime mod 4g");
        }

        // check whether p is a safe prime (meaning that both p and (p - 1) / 2 are prime)
        var primeStatus = context.getDhPrimeChecker().lookup(serverDHInnerData.dhPrime());
        switch (primeStatus) {
            case GOOD -> {}
            case BAD -> {
                return emitError("p or (p - 1) / 2 is not a prime number");
            }
            case UNKNOWN -> {
                // The certainty for isProbablyPrime() is selected from TD's is_prime() method
                // And inlined as `num.bitLength() > 2048 ? 128 : 64`
                // But received dhPrime is a safe 2048-bit number
                // https://github.com/tdlib/td/blob/cf1984844be7ec0c06762d8d617cbb20352ec9a2/tdutils/td/utils/BigNum.cpp#L150-#L159
                if (!dhPrime.isProbablePrime(64)) {
                    context.getDhPrimeChecker().addBadPrime(serverDHInnerData.dhPrime());
                    return emitError("p is not a prime number");
                }

                BigInteger halfDhPrime = dhPrime.subtract(BigInteger.ONE).divide(BigInteger.TWO);
                if (!halfDhPrime.isProbablePrime(64)) {
                    context.getDhPrimeChecker().addBadPrime(serverDHInnerData.dhPrime());
                    return emitError("(p - 1) / 2 is not a prime number");
                }
                context.getDhPrimeChecker().addGoodPrime(serverDHInnerData.dhPrime());
            }
        }

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

        context.setServerTimeDiff(serverDHInnerData.serverTime() - Math.toIntExact(System.currentTimeMillis()/1000));
        context.setAuthKey(alignKeyZero(toByteBuf(authKey), 256));
        context.setAuthKeyHash(sha1Digest(context.getAuthKey()).slice(0, 8));

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

        var req = ImmutableSetClientDHParams.of(nonce, serverNonce, dataWithHashEnc);

        dataWithHashEnc.release();

        return client.sendAuth(req);
    }

    private Mono<Void> handleDhGenOk(DhGenOk dhGenOk) {
        ByteBuf newNonceHash = sha1Digest(context.getNewNonce(), Unpooled.wrappedBuffer(new byte[]{1}), context.getAuthKeyHash())
                .slice(4, 16);

        if (!dhGenOk.nonce().equals(context.getNonce())) return emitError("Nonce mismatch");
        if (!dhGenOk.serverNonce().equals(context.getServerNonce())) return emitError("Server nonce mismatch");
        if (!dhGenOk.newNonceHash1().equals(newNonceHash)) return emitError("New nonce hash 1 mismatch");

        AuthorizationKeyHolder authKey = new AuthorizationKeyHolder(context.getAuthKey().retain());
        onAuthSink.emitValue(authKey, Sinks.EmitFailureHandler.FAIL_FAST);
        return Mono.empty();
    }

    private Mono<Void> handleDhGenRetry(DhGenRetry dhGenRetry) {
        ByteBuf newNonceHash = sha1Digest(context.getNewNonce(), Unpooled.wrappedBuffer(new byte[]{2}), context.getAuthKeyHash())
                .slice(4, 16);

        if (!dhGenRetry.nonce().equals(context.getNonce())) return emitError("Nonce mismatch");
        if (!dhGenRetry.serverNonce().equals(context.getServerNonce())) return emitError("Nonce mismatch");
        if (!dhGenRetry.newNonceHash2().equals(newNonceHash)) return emitError("New nonce hash 2 mismatch");

        ServerDHParams serverDHParams = context.getServerDHParams();
        log.debug("Retrying dh params extending, attempt: {}", context.getRetry());
        return handleServerDHParams(serverDHParams);
    }

    private Mono<Void> handleDhGenFail(DhGenFail dhGenFail) {
        ByteBuf newNonceHash = sha1Digest(context.getNewNonce(), Unpooled.wrappedBuffer(new byte[]{3}), context.getAuthKeyHash())
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
