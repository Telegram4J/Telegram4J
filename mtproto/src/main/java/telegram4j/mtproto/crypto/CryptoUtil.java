package telegram4j.mtproto.crypto;

import io.netty.buffer.ByteBuf;
import reactor.core.Exceptions;
import telegram4j.mtproto.PublicRsaKey;

import javax.crypto.Cipher;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.RSAPublicKeySpec;

final class CryptoUtil {

    private CryptoUtil() {
    }

    static final SecureRandom random = new SecureRandom();

    static ThreadLocal<MessageDigest> SHA1 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw Exceptions.propagate(e);
        }
    });

    static BigInteger fromByteArray(byte[] data) {
        return new BigInteger(1, data);
    }

    static byte[] toByteArray(ByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.markReaderIndex();
        buf.readBytes(data);
        buf.resetReaderIndex();
        return data;
    }

    static byte[] toByteArray(BigInteger val) {
        byte[] res = val.toByteArray();
        if (res[0] == 0) {
            byte[] res2 = new byte[res.length - 1];
            System.arraycopy(res, 1, res2, 0, res2.length);
            return res2;
        }
        return res;
    }

    static byte[] concat(byte[]... v) {
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
    static long pqPrimeLeemon(long n) {
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


    static byte[] sha1Digest(byte[] bytes) {
        MessageDigest sha1 = SHA1.get();
        sha1.reset();
        return sha1.digest(bytes);
    }

    static byte[] sha1Digest(byte[]... bytes) {
        MessageDigest sha1 = SHA1.get();
        sha1.reset();
        for (byte[] b : bytes) {
            sha1.update(b);
        }
        return sha1.digest();
    }

    static byte[] substring(byte[] src, int start, int len) {
        byte[] res = new byte[len];
        System.arraycopy(src, start, res, 0, len);
        return res;
    }

    static byte[] alignKeyZero(byte[] src, int size) {
        if (src.length == size) {
            return src;
        }

        if (src.length > size) {
            return substring(src, src.length - size, size);
        }
        return concat(new byte[size - src.length], src);
    }

    static byte[] xor(byte[] a, byte[] b) {
        byte[] res = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            res[i] = (byte) (a[i] ^ b[i]);
        }
        return res;
    }

    static byte[] align(byte[] src, int factor) {
        if (src.length % factor == 0) {
            return src;
        }
        int padding = factor - src.length % factor;

        return concat(src, random.generateSeed(padding));
    }
}
