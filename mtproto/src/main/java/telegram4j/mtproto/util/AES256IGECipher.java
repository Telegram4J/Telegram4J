package telegram4j.mtproto.util;

import io.netty.buffer.ByteBuf;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.security.GeneralSecurityException;

public final class AES256IGECipher {
    private static final VarHandle LONG_VIEW = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);

    private static final String AES_ECB_ALGORITHM = "AES/ECB/NoPadding";

    private final Cipher delegate;
    private final ByteBuf iv;

    public AES256IGECipher(boolean encrypt, byte[] key, ByteBuf iv) {
        this.delegate = newCipher(AES_ECB_ALGORITHM);
        this.iv = iv;

        SecretKey secretKey = new SecretKeySpec(key, "AES");
        initCipher(delegate, encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, secretKey);
    }

    public ByteBuf encrypt(ByteBuf data) {
        int size = data.readableBytes();
        int blockSize = delegate.getBlockSize();
        ByteBuf encrypted = data.alloc().buffer(size);
        // first 16 - input, second - output
        byte[] buffer = new byte[blockSize + blockSize];

        ByteBuf x = iv;
        int xOffset = blockSize;
        ByteBuf y = iv;
        int yOffset = 0;
        for (int i = 0, n = size/blockSize; i < n; i++) {
            int offset = i * blockSize;
            for (int b = 0; b < blockSize/8; b++) {
                int d = b * 8;
                LONG_VIEW.set(buffer, d, data.getLong(offset + d) ^ y.getLong(yOffset + d));
            }

            doFinal(delegate, buffer);
            for (int b = 0; b < blockSize/8; b++) {
                int d = b * 8;
                LONG_VIEW.set(buffer, blockSize + d, (long)LONG_VIEW.get(buffer, blockSize + d) ^ x.getLong(xOffset + d));
            }

            encrypted.writeBytes(buffer, blockSize, blockSize);

            x = data;
            yOffset = offset;
            y = encrypted;
            xOffset = offset;
        }

        data.release();
        iv.release();

        return encrypted;
    }

    public ByteBuf decrypt(ByteBuf data) {
        int size = data.readableBytes();
        int blockSize = delegate.getBlockSize();
        ByteBuf decrypted = data.alloc().buffer(size);
        // first 16b - input, second 16b - output
        byte[] buffer = new byte[blockSize + blockSize];

        ByteBuf x = iv;
        int xOffset = blockSize;
        ByteBuf y = iv;
        int yOffset = 0;

        for (int i = 0, n = size/blockSize; i < n; i++) {
            int offset = i * blockSize;

            for (int b = 0; b < blockSize/8; b++) {
                int d = b * 8;
                LONG_VIEW.set(buffer, d, data.getLong(offset + d) ^ x.getLong(xOffset + d));
            }

            doFinal(delegate, buffer);
            for (int b = 0; b < blockSize/8; b++) {
                int d = b * 8;
                LONG_VIEW.set(buffer, blockSize + d, (long)LONG_VIEW.get(buffer, blockSize + d) ^ y.getLong(yOffset + d));
            }

            decrypted.writeBytes(buffer, blockSize, blockSize);

            y = data;
            yOffset = offset;
            x = decrypted;
            xOffset = offset;
        }

        data.release();
        iv.release();

        return decrypted;
    }

    static Cipher newCipher(String algorithm) {
        try {
            return Cipher.getInstance(algorithm);
        } catch (GeneralSecurityException t) {
            throw new RuntimeException(t);
        }
    }

    static void initCipher(Cipher cipher, int mode, SecretKey secretKey) {
        try {
            cipher.init(mode, secretKey);
        } catch (GeneralSecurityException t) {
            throw new RuntimeException(t);
        }
    }

    static void doFinal(Cipher cipher, byte[] buffer) {
        try {
            int blockSize = cipher.getBlockSize();
            cipher.doFinal(buffer, 0, blockSize, buffer, blockSize);
        } catch (GeneralSecurityException t) {
            throw new RuntimeException(t);
        }
    }
}
