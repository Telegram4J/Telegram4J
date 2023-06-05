package telegram4j.mtproto.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.FastThreadLocal;
import reactor.core.Exceptions;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/** Utility class with set of most-used cryptography and bytebuf methods. */
public final class CryptoUtil {

    private CryptoUtil() {
    }

    public static final SecureRandom random;

    private static final FastThreadLocal<MessageDigest> SHA256;

    static {
        SecureRandom possibleStrong;
        try {
            possibleStrong = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            possibleStrong = new SecureRandom();
        }

        random = possibleStrong;

        SHA256 = new FastThreadLocal<>() {
            @Override
            protected MessageDigest initialValue() throws Exception {
                return MessageDigest.getInstance("SHA-256");
            }
        };
    }

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

    // from https://github.com/tdlib/td/blob/64c718c0a1b02a28a6f628d98cd5fbde1d17c3fa/tdutils/td/utils/crypto.cpp
    private static long pqGcd(long a, long b) {
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
    private static long pqAddMul(long c, long a, long b, long pq) {
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

    public static long pqFactorize(long pq) {
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

    private static MessageDigest createDigest(String alg) {
        try {
            return MessageDigest.getInstance(alg);
        } catch (NoSuchAlgorithmException e) {
            throw Exceptions.propagate(e);
        }
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
        MessageDigest sha1 = createDigest("SHA-1");
        sha1.reset();
        for (ByteBuf b : bufs) {
            sha1.update(b.nioBuffer());
        }
        return Unpooled.wrappedBuffer(sha1.digest());
    }

    public static ByteBuf alignKeyZero(ByteBuf src, int size) {
        if (src.readableBytes() == size) {
            return src;
        } else if (src.readableBytes() > size) {
            return src.slice(src.readableBytes() - size, size);
        } else {
            ByteBuf align = Unpooled.wrappedBuffer(new byte[size - src.readableBytes()]);
            return Unpooled.wrappedBuffer(align, src);
        }
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

    public static AES256IGECipher createAesCipher(ByteBuf messageKey, ByteBuf authKey, boolean inbound) {
        int x = inbound ? 8 : 0;

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

        return new AES256IGECipher(!inbound, toByteArray(aesKey), aesIV);
    }

    // TODO: can be done with System.arraycopy()
    public static ByteBuf reverse(ByteBuf buf) {
        ByteBuf result = buf.alloc().buffer(buf.readableBytes());
        for (int i = buf.writerIndex() - 1; i >= buf.readerIndex(); i--) {
            result.writeByte(buf.getByte(i));
        }
        return result;
    }
}
