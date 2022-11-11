package telegram4j.mtproto;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import reactor.util.annotation.Nullable;
import telegram4j.mtproto.util.CryptoUtil;
import telegram4j.tl.TlSerialUtil;

import java.math.BigInteger;
import java.util.Objects;

/** RSA exponent and modulus number tuple. */
public final class PublicRsaKey {
    private final BigInteger modulus;
    private final BigInteger exponent;

    private PublicRsaKey(BigInteger modulus, BigInteger exponent) {
        this.modulus = Objects.requireNonNull(modulus);
        this.exponent = Objects.requireNonNull(exponent);
    }

    /**
     * Compute a reversed tail of last 64 big-endian bits from serialized key sha 1 hash,
     * which uses in DH gen.
     *
     * @param key The RSA key.
     * @return The reversed tail in {@literal long} number of key hash.
     */
    public static long computeTail(PublicRsaKey key) {
        var alloc = UnpooledByteBufAllocator.DEFAULT;
        ByteBuf modulusBytes = TlSerialUtil.serializeBytes(alloc, CryptoUtil.toByteBuf(key.modulus));
        ByteBuf exponentBytes = TlSerialUtil.serializeBytes(alloc, CryptoUtil.toByteBuf(key.exponent));

        ByteBuf concat = Unpooled.wrappedBuffer(modulusBytes, exponentBytes);
        ByteBuf sha1 = CryptoUtil.sha1Digest(concat);
        concat.release();

        return sha1.getLongLE(sha1.readableBytes() - 8);
    }

    /**
     * Create new rsa key with given exponent and modulus.
     *
     * @param exponent The exponent number.
     * @param modulus The modulus number.
     * @return The new rsa key.
     */
    public static PublicRsaKey create(BigInteger exponent, BigInteger modulus) {
        return new PublicRsaKey(exponent, modulus);
    }

    /**
     * Gets a modulus number of rsa key.
     *
     * @return The modulus number.
     */
    public BigInteger getModulus() {
        return modulus;
    }

    /**
     * Gets an exponent number of rsa key.
     *
     * @return The exponent number.
     */
    public BigInteger getExponent() {
        return exponent;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublicRsaKey publicKey = (PublicRsaKey) o;
        return modulus.equals(publicKey.modulus) && exponent.equals(publicKey.exponent);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + modulus.hashCode();
        h += (h << 5) + exponent.hashCode();
        return h;
    }

    @Override
    public String toString() {
        return "PublicRsaKey{" +
                "modulus=" + modulus.toString(16) +
                ", exponent=" + exponent.toString(16) +
                '}';
    }
}
