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
package telegram4j.mtproto.auth;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import telegram4j.tl.api.TlEncodingUtil;

import java.math.BigInteger;
import java.util.Objects;

import static telegram4j.mtproto.util.CryptoUtil.random;

/**
 * Validated Diffie-Hellman parameters.
 *
 * <p> The same checks are required by the authorization key generation, where the parameters
 * come from {@code server_DH_inner_data}, and by secret chats, where they come from
 * {@code messages.getDhConfig}. Instances of this class are only created after all of them pass.
 *
 * @see <a href="https://core.telegram.org/mtproto/security_guidelines">MTProto security guidelines</a>
 */
public final class DhParameters {
    /** Size of the private exponents and of the resulting shared key, in bytes. */
    public static final int KEY_SIZE = 256;
    public static final int DEFAULT_CERTAINTY = 64;

    private static final BigInteger SAFETY_RANGE = BigInteger.TWO.pow(2048 - 64);

    private final ByteBuf primeData;
    private final BigInteger prime;
    private final BigInteger g;

    private DhParameters(ByteBuf primeData, BigInteger prime, BigInteger g) {
        this.primeData = primeData;
        this.prime = prime;
        this.g = g;
    }

    /**
     * Validates the given parameters and creates a holder for them.
     *
     * @param data the {@code dh_prime} as received from the server; copied, not retained.
     * @param g the generator, always one of 2, 3, 4, 5, 6 or 7.
     * @param checker the cache of already verified primes; a prime absent from it is verified
     * and the outcome is remembered.
     * @throws AuthorizationException if any of the checks fails.
     * @return The validated parameters.
     */
    public static DhParameters create(ByteBuf data, int g, DhPrimeChecker checker) {
        Objects.requireNonNull(data);
        Objects.requireNonNull(checker);

        // an instance outlives the request its prime arrived in, notably for secret chats,
        // where the same parameters are reused for every chat until the server bumps the version
        ByteBuf primeData = TlEncodingUtil.copyAsUnpooled(data);

        BigInteger prime = new BigInteger(1, ByteBufUtil.getBytes(primeData));
        if (prime.bitLength() != 2048) {
            throw new AuthorizationException("dhPrime is not 2048-bit number");
        }

        // g generates a cyclic subgroup of prime order (p - 1) / 2, i.e. is a quadratic residue mod p.
        // Since g is always equal to 2, 3, 4, 5, 6 or 7, this is easily done using quadratic reciprocity law,
        // yielding a simple condition on
        // * p mod 4g - namely, p mod 8 = 7 for g = 2; p mod 3 = 2 for g = 3;
        // * no extra condition for g = 4;
        // * p mod 5 = 1 or 4 for g = 5;
        // * p mod 24 = 19 or 23 for g = 6;
        // * p mod 7 = 3, 5 or 6 for g = 7.

        boolean modOk = switch (g) {
            case 2 -> prime.remainder(BigInteger.valueOf(8)).equals(BigInteger.valueOf(7));
            case 3 -> prime.remainder(BigInteger.valueOf(3)).equals(BigInteger.TWO);
            case 4 -> true;
            case 5 -> {
                long remainder = prime.remainder(BigInteger.valueOf(5)).longValueExact();
                yield remainder == 1 || remainder == 4;
            }
            case 6 -> {
                long remainder = prime.remainder(BigInteger.valueOf(24)).longValueExact();
                yield remainder == 19 || remainder == 23;
            }
            case 7 -> {
                long remainder = prime.remainder(BigInteger.valueOf(7)).longValueExact();
                yield remainder == 3 || remainder == 5 || remainder == 6;
            }
            default -> false;
        };

        if (!modOk) {
            throw new AuthorizationException("Bad dhPrime mod 4g");
        }

        // check whether dhPrime is a safe prime (meaning that both dhPrime and (dhPrime - 1) / 2 are prime)
        var primeStatus = checker.lookup(primeData);
        switch (primeStatus) {
            case GOOD -> {}
            case BAD -> throw new AuthorizationException("dhPrime or (dhPrime - 1) / 2 is not a prime number");
            case UNKNOWN -> {
                // The certainty for isProbablyPrime() is selected from TD's is_prime() method
                // And inlined as `num.bitLength() > 2048 ? 128 : 64`
                // But received dhPrime is a safe 2048-bit number
                // https://github.com/tdlib/td/blob/cf1984844be7ec0c06762d8d617cbb20352ec9a2/tdutils/td/utils/BigNum.cpp#L150-#L159
                if (!prime.isProbablePrime(DEFAULT_CERTAINTY)) {
                    checker.addBadPrime(primeData);
                    throw new AuthorizationException("dhPrime is not a prime number");
                }

                BigInteger halfDhPrime = prime.subtract(BigInteger.ONE).divide(BigInteger.TWO);
                if (!halfDhPrime.isProbablePrime(DEFAULT_CERTAINTY)) {
                    checker.addBadPrime(primeData);
                    throw new AuthorizationException("(dhPrime - 1) / 2 is not a prime number");
                }
                checker.addGoodPrime(primeData);
            }
        }

        var params = new DhParameters(primeData, prime, BigInteger.valueOf(g));
        params.checkBounds(params.g, "g");
        return params;
    }

    /** {@return The {@code dh_prime} these parameters were created from} */
    public ByteBuf primeData() {
        return primeData;
    }

    /** {@return The {@code dh_prime} as a number} */
    public BigInteger prime() {
        return prime;
    }

    /** {@return The generator} */
    public BigInteger g() {
        return g;
    }

    /** {@return A new random private exponent of {@value #KEY_SIZE} bytes} */
    public static BigInteger generateSecret() {
        byte[] bytes = new byte[KEY_SIZE];
        random.nextBytes(bytes);
        return new BigInteger(1, bytes);
    }

    /** {@return {@code g^secret mod dh_prime}} */
    public BigInteger computePublicKey(BigInteger secret) {
        return g.modPow(secret, prime);
    }

    /**
     * Computes our public key as exactly {@value #KEY_SIZE} big-endian bytes, after checking it.
     *
     * <p> Leading zeros are harmless on the wire, where the value is read as an integer, and
     * padding keeps the size of the field from varying with the key.
     *
     * @throws AuthorizationException if the resulting key is out of bounds or out of safety range.
     * @return The public key, which the caller owns.
     */
    public ByteBuf computePublicKeyData(BigInteger secret) {
        BigInteger publicKey = computePublicKey(secret);
        checkPublicKey(publicKey, "g_b");
        return toFixedWidth(publicKey, KEY_SIZE);
    }

    /** {@return {@code peerPublicKey^secret mod dh_prime}} */
    public BigInteger computeSharedKey(BigInteger peerPublicKey, BigInteger secret) {
        return peerPublicKey.modPow(secret, prime);
    }

    /**
     * Computes the shared key as exactly {@value #KEY_SIZE} big-endian bytes.
     *
     * <p> The result of the exponentiation is just a number, and roughly one in 256 of them is
     * shorter than {@value #KEY_SIZE} bytes. Left-padding it with zeros is what every
     * implementation does and what no specification mentions; without it the key fingerprint and
     * every derivation offset silently shift, in a way that only shows up on the occasional
     * connection.
     *
     * @return The shared key, which the caller owns.
     */
    public ByteBuf computeSharedKeyData(BigInteger peerPublicKey, BigInteger secret) {
        return toFixedWidth(computeSharedKey(peerPublicKey, secret), KEY_SIZE);
    }

    static ByteBuf toFixedWidth(BigInteger value, int size) {
        byte[] data = value.toByteArray();
        // toByteArray() prepends a zero byte when the top bit of the magnitude is set
        int offset = data[0] == 0 ? 1 : 0;
        int length = data.length - offset;
        if (length > size) {
            throw new IllegalArgumentException("Value is wider than " + size + " bytes: " + length);
        }

        return Unpooled.buffer(size, size)
                .writeZero(size - length)
                .writeBytes(data, offset, length);
    }

    /**
     * Verifies that the value lies in {@code (1, dh_prime - 1)}.
     *
     * @throws AuthorizationException if it does not.
     */
    public void checkBounds(BigInteger value, String name) {
        BigInteger ubound = prime.subtract(BigInteger.ONE);
        if (value.compareTo(BigInteger.ONE) < 1 || value.compareTo(ubound) >= 0) {
            throw new AuthorizationException(name + " parameter is out of bounds");
        }
    }

    /**
     * Verifies that the public key lies in {@code (1, dh_prime - 1)} and additionally in
     * {@code (2^(2048-64), dh_prime - 2^(2048-64))}, which bounds the work needed to brute-force
     * the discrete logarithm.
     *
     * @throws AuthorizationException if it does not.
     */
    public void checkPublicKey(BigInteger value, String name) {
        checkBounds(value, name);

        BigInteger unsafeBound = prime.subtract(SAFETY_RANGE);
        if (value.compareTo(SAFETY_RANGE) <= 0 || value.compareTo(unsafeBound) > 0) {
            throw new AuthorizationException(name + " parameter is out of safety range");
        }
    }

    @Override
    public String toString() {
        return "DhParameters{g=" + g + '}';
    }
}
