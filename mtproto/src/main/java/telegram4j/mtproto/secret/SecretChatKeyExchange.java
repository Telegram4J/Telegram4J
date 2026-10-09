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
package telegram4j.mtproto.secret;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import telegram4j.mtproto.auth.AuthKey;
import telegram4j.mtproto.auth.AuthorizationException;
import telegram4j.mtproto.auth.DhParameters;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The Diffie-Hellman half of establishing a secret chat, with no I/O of its own.
 *
 * <p> The exchange itself is ordinary: each side sends {@code g^x mod p} and raises what it
 * receives to its own exponent. What is easy to get wrong is everything around it, and that is
 * what this class exists to keep in one place: the peer's public key is attacker-controlled and
 * must be range-checked before use, the result must be padded to a fixed width before it becomes
 * a key, and the fingerprint the peer quotes must be compared against the key we derived rather
 * than trusted.
 *
 * @see <a href="https://core.telegram.org/api/end-to-end">End-to-End Encryption, Secret Chats</a>
 */
public final class SecretChatKeyExchange {

    private SecretChatKeyExchange() {
    }

    /** Length of what {@link #keyHash(AuthKey)} produces. */
    public static final int KEY_HASH_SIZE = 36;

    /**
     * The bytes clients show as the chat's encryption key, for the two people to read out to each
     * other and be sure nobody sits in between.
     *
     * <p> 128 bits of SHA-1 over the key, which is the original scheme, followed by 160 bits of
     * SHA-256, which layer 46 added. Apps show the first 32 bytes of it.
     *
     * <p> Computed once, from the key the chat started with, and kept unchanged when that key is
     * later replaced: a value that moved every hundred messages would have to be verified again
     * every hundred messages, which defeats the point of verifying it. TDLib computes it in the
     * same two places and nowhere else, both of them in the initial exchange.
     */
    public static byte[] keyHash(AuthKey key) {
        byte[] value = ByteBufUtil.getBytes(key.value());

        byte[] hash = new byte[KEY_HASH_SIZE];
        System.arraycopy(digest("SHA-1", value), 0, hash, 0, 16);
        System.arraycopy(digest("SHA-256", value), 0, hash, 16, 20);
        return hash;
    }

    private static byte[] digest(String algorithm, byte[] value) {
        try {
            return MessageDigest.getInstance(algorithm).digest(value);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing " + algorithm, e);
        }
    }

    /**
     * Derives the shared key from the peer's public key and our exponent.
     *
     * @param dh the parameters both sides agreed on, from {@code messages.getDhConfig}.
     * @param peerPublicKey the {@code g_a} or {@code g_a_or_b} the peer sent.
     * @param secret our private exponent.
     * @throws SecretChatException if the peer's public key is out of bounds or out of the range
     * in which the discrete logarithm is expensive.
     * @return The key, whose {@link AuthKey#id() id} is the fingerprint to quote to the peer.
     */
    public static AuthKey deriveKey(DhParameters dh, ByteBuf peerPublicKey, BigInteger secret) {
        BigInteger peer = new BigInteger(1, ByteBufUtil.getBytes(peerPublicKey));
        try {
            dh.checkPublicKey(peer, "g_a_or_b");
        } catch (AuthorizationException e) {
            // the peer chose this value, so a bad one is its protocol violation rather than ours
            throw new SecretChatException(e.getMessage());
        }

        return new AuthKey(dh.computeSharedKeyData(peer, secret));
    }

    /**
     * Derives the shared key and checks it against the fingerprint the peer quoted.
     *
     * <p> A mismatch means the two sides hold different keys, most often because one of them
     * padded the shared key differently. Continuing would produce a chat in which nothing can be
     * decrypted and every message looks tampered with.
     *
     * @param expectedFingerprint the {@code key_fingerprint} the peer sent.
     * @throws SecretChatException if the fingerprints differ, or on any failure of
     * {@link #deriveKey(DhParameters, ByteBuf, BigInteger)}.
     * @return The key, once it is known to be the same one the peer holds.
     */
    public static AuthKey deriveKey(DhParameters dh, ByteBuf peerPublicKey, BigInteger secret,
                                    long expectedFingerprint) {
        var key = deriveKey(dh, peerPublicKey, secret);
        if (key.id() != expectedFingerprint) {
            throw new SecretChatException("Key fingerprint mismatch, derived: 0x"
                    + Long.toHexString(key.id()) + ", peer quoted: 0x"
                    + Long.toHexString(expectedFingerprint));
        }
        return key;
    }
}
