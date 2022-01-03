package telegram4j.mtproto.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import reactor.core.Exceptions;
import telegram4j.mtproto.PublicRsaKey;

import javax.crypto.Cipher;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.RSAPublicKeySpec;

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

    public static byte[] toByteArray(ByteBuf buf) {
        try {
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            return data;
        } finally {
            ReferenceCountUtil.safeRelease(buf);
        }
    }

    public static byte[] toByteArray(BigInteger val) {
        byte[] res = val.toByteArray();
        if (res[0] == 0) {
            byte[] res2 = new byte[res.length - 1];
            System.arraycopy(res, 1, res2, 0, res2.length);
            return res2;
        }
        return res;
    }

    public static byte[] concat(byte[]... v) {
        int len = 0;
        for (byte[] value : v) {
            len += value.length;
        }
        byte[] res = new byte[len];
        int offset = 0;
        for (byte[] bytes : v) {
            System.arraycopy(bytes, 0, res, offset, bytes.length);
            offset += bytes.length;
        }
        return res;
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

    public static byte[] rsaEncrypt(byte[] src, PublicRsaKey key) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(new RSAPublicKeySpec(key.getExponent(), key.getModulus()));
            Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(src);
        } catch (Exception e) {
            throw Exceptions.propagate(e);
        }
    }

    public static byte[] sha256Digest(byte[]... bytes) {
        MessageDigest sha256 = SHA256.get();
        sha256.reset();
        for (byte[] b : bytes) {
            sha256.update(b);
        }
        return sha256.digest();
    }

    public static byte[] sha1Digest(byte[] bytes) {
        MessageDigest sha1 = SHA1.get();
        sha1.reset();
        return sha1.digest(bytes);
    }

    public static byte[] sha1Digest(byte[]... bytes) {
        MessageDigest sha1 = SHA1.get();
        sha1.reset();
        for (byte[] b : bytes) {
            sha1.update(b);
        }
        return sha1.digest();
    }

    public static byte[] substring(byte[] src, int start, int len) {
        byte[] res = new byte[len];
        System.arraycopy(src, start, res, 0, len);
        return res;
    }

    public static byte[] alignKeyZero(byte[] src, int size) {
        if (src.length == size) {
            return src;
        }

        if (src.length > size) {
            return substring(src, src.length - size, size);
        }
        return concat(new byte[size - src.length], src);
    }

    public static byte[] xor(byte[] a, byte[] b) {
        byte[] res = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            res[i] = (byte) (a[i] ^ b[i]);
        }
        return res;
    }

    public static byte[] align(byte[] src, int factor) {
        if (src.length % factor == 0) {
            return src;
        }

        int padding = factor - src.length % factor;
        return concat(src, random.generateSeed(padding));
    }

    public static long readLongLE(byte[] bytes) {
        return (long) bytes[0] & 0xff |
                ((long) bytes[1] & 0xff) << 8 |
                ((long) bytes[2] & 0xff) << 16 |
                ((long) bytes[3] & 0xff) << 24 |
                ((long) bytes[4] & 0xff) << 32 |
                ((long) bytes[5] & 0xff) << 40 |
                ((long) bytes[6] & 0xff) << 48 |
                ((long) bytes[7] & 0xff) << 56;
    }

    public static AES256IGECipher createAesCipher(byte[] messageKey, ByteBuf authKeyBuf, boolean server) {
        int x = server ? 8 : 0;

        ByteBuf sha256a = Unpooled.wrappedBuffer(sha256Digest(
                messageKey, toByteArray(authKeyBuf.retainedSlice(x, 36))));

        ByteBuf sha256b = Unpooled.wrappedBuffer(sha256Digest(
                toByteArray(authKeyBuf.retainedSlice(x + 40, 36)), messageKey));

        byte[] aesKey = concat(
                toByteArray(sha256a.retainedSlice(0, 8)),
                toByteArray(sha256b.retainedSlice(8, 16)),
                toByteArray(sha256a.retainedSlice(24, 8)));

        byte[] aesIV = concat(
                toByteArray(sha256b.retainedSlice(0, 8)),
                toByteArray(sha256a.retainedSlice(8, 16)),
                toByteArray(sha256b.retainedSlice(24, 8)));
        sha256a.release();
        sha256b.release();

        return new AES256IGECipher(aesKey, aesIV);
    }
}
