package telegram4j.mtproto.util;

import io.netty.buffer.ByteBuf;
import reactor.core.Exceptions;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public final class AES256IGECipher {
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
        for (int i = 0; i < size / blockSize; i++) {
            int offset = i * blockSize;
            for (int j = 0; j < blockSize; j++) {
                buffer[j] = (byte) (data.getByte(offset + j) ^ y.getByte(yOffset + j));
            }

            doFinal(delegate, buffer);
            for (int j = 0; j < blockSize; j++) {
                buffer[blockSize + j] ^= x.getByte(xOffset + j);
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
        for (int i = 0; i < size / blockSize; i++) {
            int offset = i * blockSize;
            for (int j = 0; j < blockSize; j++) {
                buffer[j] = (byte) (data.getByte(offset + j) ^ x.getByte(xOffset + j));
            }

            doFinal(delegate, buffer);
            for (int j = 0; j < blockSize; j++) {
                buffer[blockSize + j] ^= y.getByte(yOffset + j);
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
        } catch (Throwable t) {
            throw Exceptions.propagate(t);
        }
    }

    static void initCipher(Cipher cipher, int mode, SecretKey secretKey) {
        try {
            cipher.init(mode, secretKey);
        } catch (Throwable t) {
            throw Exceptions.propagate(t);
        }
    }

    static void doFinal(Cipher cipher, byte[] buffer) {
        try {
            int blockSize = cipher.getBlockSize();
            cipher.doFinal(buffer, 0, blockSize, buffer, blockSize);
        } catch (Throwable t) {
            throw Exceptions.propagate(t);
        }
    }
}
