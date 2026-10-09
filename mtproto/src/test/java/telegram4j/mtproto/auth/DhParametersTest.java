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

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class DhParametersTest {

    static DhParameters params() {
        return TestDhParameters.create();
    }

    @Test
    void sharedKeyIsAlwaysFullWidth() {
        var dh = params();

        // a shared key is just a number, so about one in 256 of them needs left-padding.
        // Searching for a real one keeps this honest rather than asserting on a synthetic value
        BigInteger secret = null;
        BigInteger peerPublic = null;
        for (int i = 1; i < 4000 && secret == null; i++) {
            var candidate = BigInteger.valueOf(i);
            var pub = dh.computePublicKey(candidate);
            if (dh.computeSharedKey(pub, candidate).bitLength() <= 2040) {
                secret = candidate;
                peerPublic = pub;
            }
        }
        assertNotNull(secret, "no short shared key found to test against");

        var key = dh.computeSharedKeyData(peerPublic, secret);
        assertEquals(DhParameters.KEY_SIZE, key.readableBytes());
        assertEquals(0, key.getByte(0), "a short key must be padded on the left, not the right");

        // and the padded bytes must be the high-order ones, i.e. the value is preserved
        assertEquals(dh.computeSharedKey(peerPublic, secret),
                new BigInteger(1, ByteBufUtil.getBytes(key)));
    }

    @Test
    void fullWidthKeysAreUntouched() {
        var dh = params();

        var secret = DhParameters.generateSecret();
        var peerSecret = DhParameters.generateSecret();
        var key = dh.computeSharedKeyData(dh.computePublicKey(peerSecret), secret);

        assertEquals(DhParameters.KEY_SIZE, key.readableBytes());
        assertEquals(dh.computeSharedKey(dh.computePublicKey(peerSecret), secret),
                new BigInteger(1, ByteBufUtil.getBytes(key)));
    }

    @Test
    void bothSidesAgree() {
        var dh = params();

        var a = DhParameters.generateSecret();
        var b = DhParameters.generateSecret();

        var ours = dh.computeSharedKeyData(dh.computePublicKey(b), a);
        var theirs = dh.computeSharedKeyData(dh.computePublicKey(a), b);

        assertEquals(ours, theirs);
        assertEquals(new AuthKey(ours).id(), new AuthKey(theirs).id());
    }

    @Test
    void rejectsBadGenerator() {
        var prime = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump(TestDhParameters.PRIME));
        var checker = DhPrimeCheckerCache.instance();

        // g = 2 requires p mod 8 == 7, which this prime does not satisfy
        var e = assertThrowsExactly(AuthorizationException.class, () -> DhParameters.create(prime, 2, checker));
        assertEquals("Bad dhPrime mod 4g", e.getMessage());
    }

    @Test
    void rejectsPublicKeysOutOfSafetyRange() {
        var dh = params();

        assertThrowsExactly(AuthorizationException.class, () -> dh.checkPublicKey(BigInteger.ONE, "gA"));
        assertThrowsExactly(AuthorizationException.class, () -> dh.checkPublicKey(dh.prime(), "gA"));
        // inside the bounds but below 2^(2048-64), where the discrete logarithm is cheap
        assertThrowsExactly(AuthorizationException.class,
                () -> dh.checkPublicKey(BigInteger.TWO.pow(2048 - 65), "gA"));

        assertDoesNotThrow(() -> dh.checkPublicKey(dh.computePublicKey(DhParameters.generateSecret()), "gA"));
    }
}
