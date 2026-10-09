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

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import telegram4j.mtproto.auth.DhParameters;
import telegram4j.mtproto.auth.TestDhParameters;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class SecretChatKeyExchangeTest {

    @Test
    void bothSidesReachTheSameKey() {
        var dh = TestDhParameters.create();

        // the initiator picks a and publishes g_a
        var a = DhParameters.generateSecret();
        var gA = dh.computePublicKeyData(a);

        // the other side picks b, derives the key from g_a, and answers with g_b and the
        // fingerprint of what it derived
        var b = DhParameters.generateSecret();
        var gB = dh.computePublicKeyData(b);
        var theirs = SecretChatKeyExchange.deriveKey(dh, gA, b);

        // the initiator derives from g_b and checks the fingerprint it was quoted
        var ours = SecretChatKeyExchange.deriveKey(dh, gB, a, theirs.id());

        assertEquals(theirs.id(), ours.id());
        assertEquals(theirs.value(), ours.value());
        assertEquals(DhParameters.KEY_SIZE, ours.value().readableBytes());
    }

    @Test
    void publicKeysAreAlwaysFullWidth() {
        var dh = TestDhParameters.create();

        // g^1 is just 3, and it must still go on the wire as 256 bytes
        var small = dh.computePublicKey(BigInteger.ONE);
        assertEquals(2, small.bitLength());

        var data = TestDhParameters.create().computePublicKeyData(DhParameters.generateSecret());
        assertEquals(DhParameters.KEY_SIZE, data.readableBytes());
    }

    @Test
    void rejectsDegeneratePeerKeys() {
        var dh = TestDhParameters.create();
        var secret = DhParameters.generateSecret();

        // a peer sending 1 would fix the shared key at 1 for any exponent we choose
        assertThrowsExactly(SecretChatException.class,
                () -> SecretChatKeyExchange.deriveKey(dh, Unpooled.wrappedBuffer(new byte[]{1}), secret));

        // and so would 0 or p - 1
        assertThrowsExactly(SecretChatException.class,
                () -> SecretChatKeyExchange.deriveKey(dh, Unpooled.wrappedBuffer(new byte[]{0}), secret));
        assertThrowsExactly(SecretChatException.class, () -> SecretChatKeyExchange.deriveKey(
                dh, toBuf(dh.prime().subtract(BigInteger.ONE)), secret));
    }

    @Test
    void rejectsKeysOutsideTheSafetyRange() {
        var dh = TestDhParameters.create();
        var secret = DhParameters.generateSecret();

        // in bounds, but small enough that the discrete logarithm is within reach
        assertThrowsExactly(SecretChatException.class,
                () -> SecretChatKeyExchange.deriveKey(dh, toBuf(BigInteger.TWO.pow(2048 - 65)), secret));
    }

    @Test
    void rejectsAFingerprintThatDoesNotMatch() {
        var dh = TestDhParameters.create();

        var a = DhParameters.generateSecret();
        var gB = dh.computePublicKeyData(DhParameters.generateSecret());

        var e = assertThrowsExactly(SecretChatException.class,
                () -> SecretChatKeyExchange.deriveKey(dh, gB, a, 0x0123456789abcdefL));
        assertTrue(e.getMessage().startsWith("Key fingerprint mismatch"), e.getMessage());
    }

    @Test
    void acceptsAShortPeerKey() {
        var dh = TestDhParameters.create();

        // nothing requires the peer to pad its public key, and MadelineProto does not;
        // a value one byte short must derive the same key as its padded form
        var b = DhParameters.generateSecret();
        var a = DhParameters.generateSecret();
        var gA = dh.computePublicKey(a);

        var padded = SecretChatKeyExchange.deriveKey(dh, toBuf(gA, DhParameters.KEY_SIZE), b);
        var bare = SecretChatKeyExchange.deriveKey(dh, toBuf(gA), b);

        assertEquals(padded.id(), bare.id());
    }

    static io.netty.buffer.ByteBuf toBuf(BigInteger value) {
        byte[] data = value.toByteArray();
        return data[0] == 0
                ? Unpooled.wrappedBuffer(data, 1, data.length - 1)
                : Unpooled.wrappedBuffer(data);
    }

    static io.netty.buffer.ByteBuf toBuf(BigInteger value, int size) {
        var bare = toBuf(value);
        return Unpooled.buffer(size, size)
                .writeZero(size - bare.readableBytes())
                .writeBytes(bare);
    }
}
