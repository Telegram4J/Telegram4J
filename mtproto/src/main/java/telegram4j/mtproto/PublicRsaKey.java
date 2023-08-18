/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package telegram4j.mtproto;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import reactor.util.annotation.Nullable;
import telegram4j.mtproto.util.CryptoUtil;
import telegram4j.tl.TlSerialUtil;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Objects;

/** Value-based tuple of RSA exponent and modulus. */
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
     * @param sha1 The SHA-1 digest.
     * @param key The RSA key.
     * @return The reversed tail in {@literal long} number of key hash.
     */
    static long computeTail(MessageDigest sha1, PublicRsaKey key) {
        var alloc = UnpooledByteBufAllocator.DEFAULT;
        ByteBuf modulusBytes = TlSerialUtil.serializeBytes(alloc, CryptoUtil.toByteBuf(key.modulus));
        ByteBuf exponentBytes = TlSerialUtil.serializeBytes(alloc, CryptoUtil.toByteBuf(key.exponent));

        sha1.reset();
        try {
            sha1.update(modulusBytes.nioBuffer());
            sha1.update(exponentBytes.nioBuffer());
        } finally {
            modulusBytes.release();
            exponentBytes.release();
        }
        ByteBuf hash = Unpooled.wrappedBuffer(sha1.digest());

        return hash.getLongLE(hash.readableBytes() - 8);
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
        if (!(o instanceof PublicRsaKey that)) return false;
        return modulus.equals(that.modulus) && exponent.equals(that.exponent);
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
