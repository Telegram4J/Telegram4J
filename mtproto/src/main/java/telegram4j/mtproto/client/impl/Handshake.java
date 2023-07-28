package telegram4j.mtproto.client.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.concurrent.ScheduledFuture;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.mtproto.PublicRsaKey;
import telegram4j.mtproto.auth.AuthKey;
import telegram4j.mtproto.auth.AuthorizationException;
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
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static telegram4j.mtproto.internal.Crypto.sha1Digest;
import static telegram4j.mtproto.internal.Crypto.sha256Digest;
import static telegram4j.mtproto.util.CryptoUtil.*;

final class Handshake extends ChannelInboundHandlerAdapter {

    private static final Logger log = Loggers.getLogger("telegram4j.mtproto.Handshake");

    private static final long RESPONSE_TIMEOUT = 3000;

    private final String clientId;
    private final AuthData authData;
    private final HandshakeContext context;

    private ScheduledFuture<?> timeoutSchedule;

    public Handshake(String clientId, AuthData authData, HandshakeContext context) {
        this.clientId = clientId;
        this.authData = authData;
        this.context = context;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {

        byte[] nonceb = new byte[16];
        random.nextBytes(nonceb);

        ByteBuf nonce = Unpooled.wrappedBuffer(nonceb);
        context.nonce(nonce);

        log.debug("[C:0x{}] Sending ReqPqMulti", clientId);
        write(ctx, ImmutableReqPqMulti.of(nonce));
    }

    private void write(ChannelHandlerContext ctx, MTProtoObject object) {
        ctx.writeAndFlush(object)
                .addListener(future -> {
                    if (future.isSuccess()) {
                        timeoutSchedule = ctx.executor().schedule(() -> {
                            ctx.pipeline().fireExceptionCaught(ReadTimeoutException.INSTANCE);
                        }, RESPONSE_TIMEOUT, TimeUnit.MILLISECONDS);
                    }
                });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof MTProtoObject obj)) {
            throw new IllegalStateException("Unexpected type of inbound: " + msg.getClass());
        }

        if (timeoutSchedule != null) {
            timeoutSchedule.cancel(true);
        }

        switch (obj.identifier()) {
            case ResPQ.ID -> handleResPQ(ctx, (ResPQ) obj);
            case ServerDHParams.ID -> handleServerDHParams(ctx, (ServerDHParams) obj);
            case DhGenOk.ID -> handleDhGenOk(ctx, (DhGenOk) obj);
            case DhGenRetry.ID -> handleDhGenRetry(ctx, (DhGenRetry) obj);
            case DhGenFail.ID -> handleDhGenFail(ctx, (DhGenFail) obj);
            default -> throw new AuthorizationException("Unexpected MTProto object: " + obj);
        }
    }

    // handling
    // ====================

    private void handleResPQ(ChannelHandlerContext ctx, ResPQ resPQ) {
        log.debug("[C:0x{}] Receiving ResPQ", clientId);

        ByteBuf nonce = resPQ.nonce();

        if (!nonce.equals(context.nonce())) throw new AuthorizationException("nonce mismatch");

        var fingerprints = resPQ.serverPublicKeyFingerprints();
        var foundKey = context.publicRsaKeyRegister().findAny(fingerprints).orElse(null);
        if (foundKey == null) {
            throw new AuthorizationException("Unknown server fingerprints: " + fingerprints.stream()
                    .map(Long::toHexString)
                    .collect(Collectors.joining(", ", "[", "]")));
        }

        BigInteger pq = fromByteBuf(resPQ.pq());
        BigInteger p = BigInteger.valueOf(pqFactorize(pq.longValueExact()));
        BigInteger q = pq.divide(p);

        if (p.longValueExact() > q.longValueExact()) {
            throw new AuthorizationException("Invalid factorization result. p: " + p + ", q: " + q + ", pq: " + pq);
        }

        ByteBuf pb = toByteBuf(p);
        ByteBuf qb = toByteBuf(q);

        byte[] newNonceb = new byte[32];
        random.nextBytes(newNonceb);

        ByteBuf newNonce = Unpooled.wrappedBuffer(newNonceb);

        context.newNonce(newNonce);
        context.serverNonce(resPQ.serverNonce());

        PQInnerData pqInnerData;
        if (context.expiresIn() != 0) {
            pqInnerData = PQInnerDataTempDc.builder()
                    .pq(resPQ.pq())
                    .p(pb)
                    .q(qb)
                    .nonce(nonce)
                    .serverNonce(resPQ.serverNonce())
                    .newNonce(newNonce)
                    .dc(authData.dc().getInternalId())
                    .expiresIn(context.expiresIn())
                    .build();
            context.expiresAt(System.currentTimeMillis() + context.expiresIn()*1000L);
        } else {
            pqInnerData = PQInnerDataDc.builder()
                    .pq(resPQ.pq())
                    .p(pb)
                    .q(qb)
                    .nonce(nonce)
                    .serverNonce(resPQ.serverNonce())
                    .newNonce(newNonce)
                    .dc(authData.dc().getInternalId())
                    .build();
        }

        ByteBuf pqInnerDataBuf = TlSerializer.serialize(ctx.alloc(), pqInnerData);
        ByteBuf encryptedData = rsa(pqInnerDataBuf, foundKey.key());

        log.debug("[C:0x{}] Sending ReqDHParams", clientId);
        write(ctx, ReqDHParams.builder()
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
    private static ByteBuf rsaPad(ByteBuf data, PublicRsaKey key) {
        if (data.readableBytes() > 144)
            throw new AuthorizationException("Plain data length is more that 144 bytes");

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

    private void handleServerDHParams(ChannelHandlerContext ctx, ServerDHParams serverDHParams) {
        log.debug("[C:0x{}] Receiving ServerDHParams", clientId);

        if (!serverDHParams.nonce().equals(context.nonce())) throw new AuthorizationException("nonce mismatch");
        if (!serverDHParams.serverNonce().equals(context.serverNonce())) throw new AuthorizationException("serverNonce mismatch");

        ByteBuf encryptedAnswer = serverDHParams.encryptedAnswer();
        if (encryptedAnswer.readableBytes() % 16 != 0) {
            throw new AuthorizationException("encryptedAnswer size mismatch");
        }

        ByteBuf serverNonceAndNewNonceSha1 = sha1Digest(context.serverNonce(), context.newNonce());
        ByteBuf tmpAesKey = Unpooled.wrappedBuffer(
                sha1Digest(context.newNonce(), context.serverNonce()),
                serverNonceAndNewNonceSha1.retainedSlice(0, 12));

        ByteBuf tmpAesIv = Unpooled.wrappedBuffer(
                serverNonceAndNewNonceSha1.slice(12, 8),
                sha1Digest(context.newNonce(), context.newNonce()),
                context.newNonce().retainedSlice(0, 4));

        var cipher = AES256IGECipher.create();
        cipher.init(false, tmpAesKey.retain(), tmpAesIv.retain());

        ByteBuf decrypted = cipher.decrypt(encryptedAnswer);
        int answerSize = decrypted.readableBytes();
        ByteBuf hash = decrypted.readSlice(20);
        ServerDHInnerData serverDHInnerData = TlDeserializer.deserialize(decrypted);

        int pad = decrypted.readableBytes();
        if (pad >= 16) {
            decrypted.release();
            throw new AuthorizationException("Too big padding for encryptedAnswer");
        }

        int dhInnerDataSize = answerSize - pad - 20;
        if (!hash.equals(sha1Digest(decrypted.slice(20, dhInnerDataSize)))) throw new AuthorizationException("SHA1(ServerDHInnerData) mismatch");

        decrypted.release();

        if (!serverDHInnerData.nonce().equals(context.nonce())) throw new AuthorizationException("nonce mismatch");
        if (!serverDHInnerData.serverNonce().equals(context.serverNonce())) throw new AuthorizationException("serverNonce mismatch");

        BigInteger dhPrime = fromByteBuf(serverDHInnerData.dhPrime());
        // region dh checks
        if (dhPrime.bitLength() != 2048) {
            throw new AuthorizationException("dhPrime is not 2048-bit number");
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
            throw new AuthorizationException("Bad dhPrime mod 4g");
        }

        // check whether dhPrime is a safe prime (meaning that both dhPrime and (dhPrime - 1) / 2 are prime)
        var primeStatus = context.dhPrimeChecker().lookup(serverDHInnerData.dhPrime());
        switch (primeStatus) {
            case GOOD -> {}
            case BAD -> throw new AuthorizationException("dhPrime or (dhPrime - 1) / 2 is not a prime number");
            case UNKNOWN -> {
                // The certainty for isProbablyPrime() is selected from TD's is_prime() method
                // And inlined as `num.bitLength() > 2048 ? 128 : 64`
                // But received dhPrime is a safe 2048-bit number
                // https://github.com/tdlib/td/blob/cf1984844be7ec0c06762d8d617cbb20352ec9a2/tdutils/td/utils/BigNum.cpp#L150-#L159
                if (!dhPrime.isProbablePrime(64)) {
                    context.dhPrimeChecker().addBadPrime(serverDHInnerData.dhPrime());
                    throw new AuthorizationException("dhPrime is not a prime number");
                }

                BigInteger halfDhPrime = dhPrime.subtract(BigInteger.ONE).divide(BigInteger.TWO);
                if (!halfDhPrime.isProbablePrime(64)) {
                    context.dhPrimeChecker().addBadPrime(serverDHInnerData.dhPrime());
                    throw new AuthorizationException("(dhPrime - 1) / 2 is not a prime number");
                }
                context.dhPrimeChecker().addGoodPrime(serverDHInnerData.dhPrime());
            }
        }

        byte[] bs = new byte[256];
        random.nextBytes(bs);

        BigInteger b = fromByteArray(bs);
        BigInteger g = BigInteger.valueOf(serverDHInnerData.g());
        BigInteger gb = g.modPow(b, dhPrime);
        BigInteger ga = fromByteBuf(serverDHInnerData.gA());

        BigInteger ubound = dhPrime.subtract(BigInteger.ONE);
        if (g.compareTo(BigInteger.ONE) < 1 || g.compareTo(ubound) >= 0)
            throw new AuthorizationException("g parameter is out of bounds");
        if (ga.compareTo(BigInteger.ONE) < 1 || ga.compareTo(ubound) >= 0)
            throw new AuthorizationException("gA parameter is out of bounds");
        if (gb.compareTo(BigInteger.ONE) < 1 || gb.compareTo(ubound) >= 0)
            throw new AuthorizationException("gB parameter is out of bounds");

        BigInteger safetyRange = BigInteger.TWO.pow(2048 - 64);
        BigInteger usafeBound = dhPrime.subtract(safetyRange);
        if (ga.compareTo(safetyRange) <= 0 || ga.compareTo(usafeBound) > 0)
            throw new AuthorizationException("gA parameter is out of safety range");
        if (gb.compareTo(safetyRange) <= 0 || gb.compareTo(usafeBound) > 0)
            throw new AuthorizationException("gB parameter is out of safety range");

        // endregion

        BigInteger authKey = ga.modPow(b, dhPrime);

        context.serverTimeDiff(serverDHInnerData.serverTime() - Math.toIntExact(System.currentTimeMillis()/1000));
        context.authKey(toByteBuf(authKey));
        context.authKeyHash(sha1Digest(context.authKey()).slice(0, 8));
        context.serverSalt(Long.reverseBytes(context.newNonce().getLong(0) ^ context.serverNonce().getLong(1)));

        ClientDHInnerData clientDHInnerData = ClientDHInnerData.builder()
                .retryId(context.getRetryAndIncrement())
                .nonce(context.nonce())
                .serverNonce(context.serverNonce())
                .gB(toByteBuf(gb))
                .build();

        ByteBuf innerData = TlSerializer.serialize(ctx.alloc(), clientDHInnerData);
        ByteBuf innerDataWithHash = align(Unpooled.wrappedBuffer(sha1Digest(innerData), innerData), 16);

        cipher.init(true, tmpAesKey, tmpAesIv);
        ByteBuf dataWithHashEnc = cipher.encrypt(innerDataWithHash);

        var req = ImmutableSetClientDHParams.of(context.nonce(), context.serverNonce(), dataWithHashEnc);

        dataWithHashEnc.release();

        log.debug("[C:0x{}] Sending SetClientDHParam", clientId);
        write(ctx, req);
    }

    private void handleDhGenOk(ChannelHandlerContext ctx, DhGenOk dhGenOk) {
        log.debug("[C:0x{}] Receiving DhGenOk", clientId);

        ByteBuf newNonceHash = sha1Digest(context.newNonce(), Unpooled.wrappedBuffer(new byte[]{1}), context.authKeyHash())
                .slice(4, 16);

        if (!dhGenOk.nonce().equals(context.nonce())) throw new AuthorizationException("nonce mismatch");
        if (!dhGenOk.serverNonce().equals(context.serverNonce())) throw new AuthorizationException("serverNonce mismatch");
        if (!dhGenOk.newNonceHash1().equals(newNonceHash)) throw new AuthorizationException("newNonceHash1 mismatch");

        Instant expiresAtTimestamp = context.expiresAt() != 0 ? Instant.ofEpochSecond(context.expiresAt()) : null;
        ctx.fireUserEventTriggered(new HandshakeCompleteEvent(
                new AuthKey(context.authKey(), expiresAtTimestamp),
                context.serverSalt(), context.serverTimeDiff()));
    }

    private void handleDhGenRetry(ChannelHandlerContext ctx, DhGenRetry dhGenRetry) {
        log.debug("[C:0x{}] Receiving DhGenRetry", clientId);

        ByteBuf newNonceHash = sha1Digest(context.newNonce(), Unpooled.wrappedBuffer(new byte[]{2}), context.authKeyHash())
                .slice(4, 16);

        if (!dhGenRetry.nonce().equals(context.nonce())) throw new AuthorizationException("nonce mismatch");
        if (!dhGenRetry.serverNonce().equals(context.serverNonce())) throw new AuthorizationException("serverNonce mismatch");
        if (!dhGenRetry.newNonceHash2().equals(newNonceHash)) throw new AuthorizationException("newNonceHash2 mismatch");

        // log.debug("Retrying dh params extending, attempt: {}", context.retry());

        throw new AuthorizationException();
    }

    private void handleDhGenFail(ChannelHandlerContext ctx, DhGenFail dhGenFail) {
        log.debug("[C:0x{}] Receiving DhGenFail", clientId);

        ByteBuf newNonceHash = sha1Digest(context.newNonce(), Unpooled.wrappedBuffer(new byte[]{3}), context.authKeyHash())
                .slice(4, 16);

        if (!dhGenFail.nonce().equals(context.nonce())) throw new AuthorizationException("nonce mismatch");
        if (!dhGenFail.serverNonce().equals(context.serverNonce())) throw new AuthorizationException("serverNonce mismatch");
        if (!dhGenFail.newNonceHash3().equals(newNonceHash)) throw new AuthorizationException("newNonceHash3 mismatch");

        throw new AuthorizationException("Failed to create an authorization key");
    }

    static ByteBuf align(ByteBuf src, int factor) {
        if (src.readableBytes() % factor == 0) {
            return src;
        }

        int padding = factor - src.readableBytes() % factor;
        byte[] paddingb = new byte[padding];
        random.nextBytes(paddingb);
        return Unpooled.wrappedBuffer(src, Unpooled.wrappedBuffer(paddingb));
    }

    // from https://github.com/tdlib/td/blob/64c718c0a1b02a28a6f628d98cd5fbde1d17c3fa/tdutils/td/utils/crypto.cpp
    static long pqGcd(long a, long b) {
        if (a == 0) return b;

        while ((a & 1) == 0) {
            a >>= 1;
        }

        // if ((b & 1) == 0)
        //     throw new IllegalArgumentException("a: " + a + ", b: " + b);

        while (true) {
            if (a > b) {
                a = a - b >> 1;
                while ((a & 1) == 0) {
                    a >>= 1;
                }
            } else if (b > a) {
                b = b - a >> 1;
                while ((b & 1) == 0) {
                    b >>= 1;
                }
            } else {
                return a;
            }
        }
    }

    // returns (c + a * b) % pq
    static long pqAddMul(long c, long a, long b, long pq) {
        while (b != 0) {
            if ((b & 1) != 0) {
                c += a;
                if (c >= pq) {
                    c -= pq;
                }
            }
            a += a;
            if (a >= pq) {
                a -= pq;
            }
            b >>= 1;
        }
        return c;
    }

    static long pqFactorize(long pq) {
        if (pq <= 2) return 1;
        if ((pq & 1) == 0) return 2;

        long g = 0;
        for (int i = 0, iter = 0; i < 3 || iter < 1000; i++) {
            long q = (17 + random.nextInt(32)) % (pq - 1);
            long x = Math.abs(random.nextLong()) % (pq - 1) + 1;
            long y = x;
            int lim = 1 << Math.min(5, i) + 18;
            for (int j = 1; j < lim; j++) {
                iter++;
                x = pqAddMul(q, x, x, pq);
                long z = x < y ? pq + x - y : x - y;
                g = pqGcd(z, pq);
                if (g != 1) {
                    break;
                }
                if ((j & j - 1) == 0) {
                    y = x;
                }
            }
            if (g > 1 && g < pq) {
                break;
            }
        }
        return g != 0 ? Math.min(pq / g, g) : g;
    }
}
