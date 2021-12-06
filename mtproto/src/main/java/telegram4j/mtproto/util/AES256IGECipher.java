package telegram4j.mtproto.util;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public final class AES256IGECipher {
    private static final String AES_CBC_ALGORITHM = "AES/ECB/NoPadding";

    private final Cipher encryptor;
    private final Cipher decrypter;
    private final SecretKey secretKey;
    private final byte[] iv;

    public AES256IGECipher(byte[] key, byte[] iv) {
        this.encryptor = newCipher(AES_CBC_ALGORITHM);
        this.decrypter = newCipher(AES_CBC_ALGORITHM);
        this.secretKey = new SecretKeySpec(key, "AES");
        this.iv = iv.clone();
    }

    // TODO: replace byte[] usages to netty's ByteBuf
    public byte[] encrypt(byte[] data) {
        synchronized (encryptor) {
            initCipher(encryptor, Cipher.ENCRYPT_MODE, secretKey);

            int blockSize = encryptor.getBlockSize();
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

                encryptedBlock = doFinal(encryptor, encryptedBlock);
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
    }

    public byte[] decrypt(byte[] data) {
        synchronized (decrypter) {
            initCipher(decrypter, Cipher.DECRYPT_MODE, secretKey);

            int blockSize = decrypter.getBlockSize();
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

                decryptedBlock = doFinal(decrypter, decryptedBlock);
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
    }

    static Cipher newCipher(String algorithm) {
        try {
            return Cipher.getInstance(algorithm);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalArgumentException("Not a valid encryption algorithm", ex);
        } catch (NoSuchPaddingException ex) {
            throw new IllegalStateException("Should not happen", ex);
        }
    }

    static void initCipher(Cipher cipher, int mode, SecretKey secretKey) {
        try {
            cipher.init(mode, secretKey);
        } catch (InvalidKeyException ex) {
            throw new IllegalArgumentException("Unable to initialize due to invalid secret key", ex);
        }
    }

    static byte[] doFinal(Cipher cipher, byte[] input) {
        try {
            return cipher.doFinal(input);
        } catch (IllegalBlockSizeException ex) {
            throw new IllegalStateException("Unable to invoke Cipher due to illegal block size", ex);
        } catch (BadPaddingException ex) {
            throw new IllegalStateException("Unable to invoke Cipher due to bad padding", ex);
        }
    }
}
