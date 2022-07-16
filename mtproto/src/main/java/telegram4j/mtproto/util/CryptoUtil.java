package telegram4j.mtproto.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import reactor.core.Exceptions;
import telegram4j.mtproto.PublicRsaKey;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/** Utility class with set of most-used cryptography and bytebuf methods. */
public final class CryptoUtil {

    private CryptoUtil() {
    }

    public static final SecureRandom random = new SecureRandom();

    static ThreadLocal<MessageDigest> SHA1 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw Exceptions.propagate(e);
        }
    });

    static ThreadLocal<MessageDigest> SHA256 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw Exceptions.propagate(e);
        }
    });

    public static ThreadLocal<MessageDigest> MD5 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw Exceptions.propagate(e);
        }
    });

    public static BigInteger fromByteArray(byte[] data) {
        return new BigInteger(1, data);
    }

    public static BigInteger fromByteBuf(ByteBuf data) {
        return new BigInteger(1, toByteArray(data));
    }

    public static ByteBuf toByteBuf(BigInteger val) {
        byte[] res = val.toByteArray();
        if (res[0] == 0) {
            return Unpooled.wrappedBuffer(res, 1, res.length - 1);
        }
        return Unpooled.wrappedBuffer(res);
    }

    public static byte[] toByteArray(ByteBuf buf) {
        try {
            return ByteBufUtil.getBytes(buf);
        } finally {
            ReferenceCountUtil.safeRelease(buf);
        }
    }

    // from https://github.com/zhukov/webogram/blob/6c8b8474194ed8a76c4cf70db303c0c1cd86891f/app/js/lib/bin_utils.js#L597
    public static long pqPrimeLeemon(long n) {
        long g = 0;
        for (int i = 0; i < 3; i++) {
            int q = (random.nextInt(128) & 15) + 17;
            long x = random.nextInt(1000000000) + 1;
            long y = x;
            int lim = 1 << i + 18;

            for (int j = 1; j < lim; j++) {
                long a = x, b = x, c = q;
                while (b != 0) {
                    if ((b & 1) != 0) {
                        c += a;
                        if (c >= n) {
                            c -= n;
                        }
                    }
                    a += a;
                    if (a >= n) {
                        a -= n;
                    }
                    b >>= 1;
                }
                x = c;
                long z = x < y ? y - x : x - y;
                long b1 = n;
                while (z != 0 && b1 != 0) {
                    while ((b1 & 1) == 0) {
                        b1 >>= 1;
                    }
                    while ((z & 1) == 0) {
                        z >>= 1;
                    }
                    if (z > b1) {
                        z -= b1;
                    } else {
                        b1 -= z;
                    }
                }
                g = b1 == 0 ? z : b1;
                if (g != 1) {
                    break;
                }
                if ((j & j - 1) == 0) {
                    y = x;
                }
            }
            if (g > 1) {
                break;
            }
        }

        return Math.min(n / g, g);
    }

    public static ByteBuf sha256Digest(ByteBuf... bufs) {
        MessageDigest sha256 = SHA256.get();
        sha256.reset();
        for (ByteBuf b : bufs) {
            sha256.update(b.nioBuffer());
        }
        return Unpooled.wrappedBuffer(sha256.digest());
    }

    public static ByteBuf sha1Digest(ByteBuf... bufs) {
        MessageDigest sha1 = SHA1.get();
        sha1.reset();
        for (ByteBuf b : bufs) {
            sha1.update(b.nioBuffer());
        }
        return Unpooled.wrappedBuffer(sha1.digest());
    }

    public static ByteBuf rsaEncrypt(ByteBuf src, PublicRsaKey key) {
        BigInteger num = fromByteBuf(src);
        return toByteBuf(num.modPow(key.getExponent(), key.getModulus()));
    }

    public static ByteBuf alignKeyZero(ByteBuf src, int size) {
        if (src.readableBytes() == size) {
            return src;
        }

        if (src.readableBytes() > size) {
            return src.slice(src.readableBytes() - size, size);
        }
        ByteBuf align = Unpooled.wrappedBuffer(new byte[size - src.readableBytes()]);
        return Unpooled.wrappedBuffer(align, src);
    }

    public static ByteBuf xor(ByteBuf a, ByteBuf b) {
        ByteBuf res = Unpooled.buffer(a.readableBytes());
        for (int i = 0, n = a.readableBytes(); i < n; i++) {
            res.writeByte((byte) (a.getByte(i) ^ b.getByte(i)));
        }
        return res;
    }

    public static ByteBuf align(ByteBuf src, int factor) {
        if (src.readableBytes() % factor == 0) {
            return src;
        }

        int padding = factor - src.readableBytes() % factor;
        byte[] paddingb = new byte[padding];
        random.nextBytes(paddingb);
        return Unpooled.wrappedBuffer(src, Unpooled.wrappedBuffer(paddingb));
    }

    public static void reverse(ByteBuf data) {
        int n = data.readableBytes();
        for (int i = 0, mid = n >> 1, j = n - 1; i < mid; i++, j--) {
            byte ib = data.getByte(i);
            byte jb = data.getByte(j);
            data.setByte(i, jb);
            data.setByte(j, ib);
        }
    }

    public static AES256IGECipher createAesCipher(ByteBuf messageKey, ByteBuf authKey, boolean server) {
        int x = server ? 8 : 0;

        ByteBuf sha256a = sha256Digest(messageKey, authKey.slice(x, 36));
        ByteBuf sha256b = sha256Digest(authKey.slice(x + 40, 36), messageKey);

        ByteBuf aesKey = Unpooled.wrappedBuffer(
                sha256a.retainedSlice(0, 8),
                sha256b.retainedSlice(8, 16),
                sha256a.retainedSlice(24, 8));

        ByteBuf aesIV = Unpooled.wrappedBuffer(
                sha256b.retainedSlice(0, 8),
                sha256a.retainedSlice(8, 16),
                sha256b.retainedSlice(24, 8));
        sha256a.release();
        sha256b.release();

        return new AES256IGECipher(!server, toByteArray(aesKey), aesIV);
    }
}
