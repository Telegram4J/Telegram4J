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
package telegram4j.mtproto.util;

import io.netty.buffer.ByteBufUtil;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class CryptoUtilTest {

    /** Width of a 2048-bit authorization key, the size that made a fixed width necessary. */
    static final int AUTH_KEY_SIZE = 256;

    @Test
    void padsAValueNarrowerThanAskedFor() {
        // the case that fails a handshake: an authorization key numerically small enough that its
        // shortest representation comes out a byte short of the key size. The modulus leaves the
        // top byte zero for about one key in two hundred
        var small = value(AUTH_KEY_SIZE - 1, 0x7f);
        assertEquals(AUTH_KEY_SIZE - 1, small.toByteArray().length, "the test value is not short");

        var padded = CryptoUtil.toFixedWidth(small, AUTH_KEY_SIZE);
        assertEquals(AUTH_KEY_SIZE, padded.readableBytes());
        assertEquals(0, padded.getByte(0), "the padding did not go in front");
        assertEquals(small, CryptoUtil.fromByteBuf(padded.copy()), "padding changed the value");
    }

    @Test
    void leavesAValueOfTheFullWidthAlone() {
        var full = value(AUTH_KEY_SIZE, 0x7f);
        assertEquals(AUTH_KEY_SIZE, full.toByteArray().length);

        var rendered = CryptoUtil.toFixedWidth(full, AUTH_KEY_SIZE);
        assertEquals(AUTH_KEY_SIZE, rendered.readableBytes());
        assertEquals(full, CryptoUtil.fromByteBuf(rendered.copy()));
    }

    @Test
    void dropsTheSignByteRatherThanCountingItAsWidth() {
        // toByteArray() prepends a zero whenever the top bit is set, and that byte is not part of
        // the value: counted as width it would make a full-width key look one byte too wide
        var topBitSet = value(AUTH_KEY_SIZE, 0xff);
        assertEquals(AUTH_KEY_SIZE + 1, topBitSet.toByteArray().length);
        assertEquals(0, topBitSet.toByteArray()[0]);

        var rendered = CryptoUtil.toFixedWidth(topBitSet, AUTH_KEY_SIZE);
        assertEquals(AUTH_KEY_SIZE, rendered.readableBytes());
        assertEquals(topBitSet, CryptoUtil.fromByteBuf(rendered.copy()));
    }

    @Test
    void refusesAValueThatDoesNotFit() {
        var tooWide = value(AUTH_KEY_SIZE + 1, 0x01);
        assertThrows(IllegalArgumentException.class,
                () -> CryptoUtil.toFixedWidth(tooWide, AUTH_KEY_SIZE));
    }

    @Test
    void rendersZeroAsAllZeroes() {
        assertEquals("0000000000000000",
                ByteBufUtil.hexDump(CryptoUtil.toFixedWidth(BigInteger.ZERO, 8)));
    }

    /** {@return A positive value exactly {@code width} bytes wide, starting with {@code first}} */
    static BigInteger value(int width, int first) {
        byte[] bytes = new byte[width];
        bytes[0] = (byte) first;
        bytes[width - 1] = 1;
        return new BigInteger(1, bytes);
    }
}
