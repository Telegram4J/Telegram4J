package telegram4j.mtproto.util;

import reactor.core.Exceptions;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public final class AES256IGECipher {
    private static final String AES_ECB_ALGORITHM = "AES/ECB/NoPadding";

    private final Cipher delegate;
    private final byte[] iv;

    public AES256IGECipher(boolean encrypt, byte[] key, byte[] iv) {
        this.delegate = newCipher(AES_ECB_ALGORITHM);
        this.iv = iv;

        SecretKey secretKey = new SecretKeySpec(key, "AES");
        initCipher(delegate, encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, secretKey);
    }

    public byte[] encrypt(byte[] data) {
        int blockSize = delegate.getBlockSize();
        byte[] encrypted = new byte[data.length];
        byte[] encryptedBlock = new byte[blockSize];

        byte[] x = iv;
        int xOffset = blockSize;
        byte[] y = iv;
        int yOffset = 0;
        for (int i = 0; i < data.length / blockSize; i++) {
            int offset = i * blockSize;
            for (int j = 0; j < blockSize; j++) {
                encryptedBlock[j] = (byte) (data[offset + j] ^ y[yOffset + j]);
            }

            encryptedBlock = doFinal(delegate, encryptedBlock);
            for (int j = 0; j < blockSize; j++) {
                encryptedBlock[j] ^= x[xOffset + j];
            }

            System.arraycopy(encryptedBlock, 0, encrypted, offset, blockSize);

            x = data;
            yOffset = offset;
            y = encrypted;
            xOffset = offset;
        }

        return encrypted;
    }

    public byte[] decrypt(byte[] data) {
        int blockSize = delegate.getBlockSize();
        byte[] decrypted = new byte[data.length];
        byte[] decryptedBlock = new byte[blockSize];

        byte[] x = iv;
        int xOffset = blockSize;
        byte[] y = iv;
        int yOffset = 0;
        for (int i = 0; i < data.length / blockSize; i++) {
            int offset = i * blockSize;
            for (int j = 0; j < blockSize; j++) {
                decryptedBlock[j] = (byte) (data[offset + j] ^ x[xOffset + j]);
            }

            decryptedBlock = doFinal(delegate, decryptedBlock);
            for (int j = 0; j < blockSize; j++) {
                decryptedBlock[j] ^= y[yOffset + j];
            }

            System.arraycopy(decryptedBlock, 0, decrypted, offset, blockSize);

            y = data;
            yOffset = offset;
            x = decrypted;
            xOffset = offset;
        }

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

    static byte[] doFinal(Cipher cipher, byte[] input) {
        try {
            return cipher.doFinal(input);
        } catch (Throwable t) {
            throw Exceptions.propagate(t);
        }
    }
}
