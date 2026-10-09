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
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import telegram4j.mtproto.auth.AuthKey;
import telegram4j.tl.SecretTlInfo;
import telegram4j.tl.secret.BaseDecryptedMessage;
import telegram4j.tl.secret.DecryptedMessageLayer;
import telegram4j.tl.secret.ImmutableDecryptedMessageLayer;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SecretChatCipherTest {

    static AuthKey key = randomKey(0x5EC2E7);

    @Test
    void roundTrip() {
        // the creator encrypts with x = 0 and decrypts with x = 8, the other side does the opposite,
        // so a payload is only readable by the side which did not write it
        var creator = new SecretChatCipher();
        var participant = new SecretChatCipher();

        var expected = layer("hi", 0, 1);

        ByteBuf payload = creator.encrypt(key, true, expected);
        assertEquals(key.id(), payload.getLongLE(0));
        assertEquals(expected, participant.decrypt(key, false, payload));
        payload.release();

        ByteBuf back = participant.encrypt(key, false, expected);
        assertEquals(expected, creator.decrypt(key, true, back));
        back.release();
    }

    @Test
    void rejectsMirroredMessage() {
        var cipher = new SecretChatCipher();

        // what the key derivation offset protects against: a message of ours replayed back to us.
        // The creator writes with x = 0 and reads with x = 8, so its own payload decrypts to
        // garbage and must be stopped by the message key check rather than parsed
        ByteBuf payload = cipher.encrypt(key, true, layer("hi", 0, 1));

        var e = assertThrowsExactly(SecretChatException.class, () -> cipher.decrypt(key, true, payload));
        assertEquals("Message key mismatch", e.getMessage());
        payload.release();
    }

    @Test
    void rejectsForeignKey() {
        var cipher = new SecretChatCipher();
        var other = randomKey(0xBADBAD);

        ByteBuf payload = cipher.encrypt(key, true, layer("hi", 0, 1));
        var e = assertThrowsExactly(SecretChatException.class, () -> cipher.decrypt(other, false, payload));
        assertTrue(e.getMessage().startsWith("Key fingerprint mismatch"));
        payload.release();
    }

    @Test
    void rejectsTamperedPayload() {
        var cipher = new SecretChatCipher();

        ByteBuf payload = cipher.encrypt(key, true, layer("hi", 0, 1));
        payload.setByte(payload.writerIndex() - 1, payload.getByte(payload.writerIndex() - 1) ^ 1);

        var e = assertThrowsExactly(SecretChatException.class, () -> cipher.decrypt(key, false, payload));
        assertEquals("Message key mismatch", e.getMessage());
        payload.release();
    }

    @Test
    void padsToBuckets() {
        // a one-character message and a much longer one must be indistinguishable by size
        var cipher = new SecretChatCipher();

        ByteBuf small = cipher.encrypt(key, true, layer("a", 0, 1));
        ByteBuf large = cipher.encrypt(key, true, layer("a".repeat(64), 0, 1));

        assertEquals(24 + 128, small.readableBytes());
        assertEquals(24 + 192, large.readableBytes());
        small.release();
        large.release();

        // and every payload is aligned and carries at least the minimal padding
        for (int i = 0; i < 512; i++) {
            int padded = SecretChatCipher.paddedSize(i);
            assertEquals(0, padded % 16);
            assertTrue(padded - i >= 12, "no padding for " + i);
        }
        assertEquals(1280 + 448, SecretChatCipher.paddedSize(1281));
    }

    @Test
    void readsForeignPayload() {
        // Produced by an implementation written separately from this one, straight from
        // https://core.telegram.org/api/end-to-end, for `key` and layer("hi", 0, 1).
        // It also carries the minimal 12..27 bytes of padding rather than our bucketed
        // amount, which no payload written by encrypt() ever does
        ByteBuf payload = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump(
                "ec1fdd87a222d431c2bfbd7ab43f2bf67ce4ddcaa5cc33c6b9825d9df4782667" +
                "e95e02235a201586805b75a113bbba660730f0468ddbac22c01457c6e2e53cd4" +
                "4c3c9306d795988398a3bc413dde4e3f48135d8c643c9a6776c74935f10a648c" +
                "df5acf327befaacc"));

        assertEquals(layer("hi", 0, 1), new SecretChatCipher().decrypt(key, false, payload));
        payload.release();
    }

    @Test
    void readsLegacyPayloadOnlyWhenAllowed() {
        // An MTProto 1.0 payload for `key` and layer("hi", 0, 1), built from MadelineProto's
        // Crypt::oldKdf and tryMTProtoV1Decrypt. This is what MadelineProto's first service
        // message in a chat looks like, before it learns that the peer supports layer 73
        ByteBuf payload = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump(
                "ec1fdd87a222d431b403e96c23d0f6e1581bc16bd0cd25e200ad21dd121f8b6b" +
                "6164e46b5cc7bfc00fb098ba90f9435c7a22745c0d66123276c3db954a1330ce" +
                "ad1125770f0541e139636e8d55dfe2d965ccd2f87c7a1283"));

        var strict = new SecretChatCipher();
        assertThrowsExactly(SecretChatException.class, () -> strict.decrypt(key, false, payload));

        var lenient = new SecretChatCipher(true);
        assertEquals(layer("hi", 0, 1), lenient.decrypt(key, false, payload));
        payload.release();
    }

    @Test
    void legacyModeStillRejectsGarbage() {
        // falling back to MTProto 1.0 must not turn a bad payload into a parsed one
        var lenient = new SecretChatCipher(true);

        ByteBuf payload = lenient.encrypt(key, true, layer("hi", 0, 1));
        payload.setByte(payload.writerIndex() - 1, payload.getByte(payload.writerIndex() - 1) ^ 1);

        assertThrowsExactly(SecretChatException.class, () -> lenient.decrypt(key, false, payload));
        payload.release();
    }

    @Test
    void neverWritesLegacy() {
        // the compatibility mode is read-only: what we send stays MTProto 2.0
        var lenient = new SecretChatCipher(true);

        ByteBuf payload = lenient.encrypt(key, true, layer("hi", 0, 1));
        assertEquals(layer("hi", 0, 1), new SecretChatCipher().decrypt(key, false, payload));
        payload.release();
    }

    @Test
    void survivesArbitraryLengths() {
        var creator = new SecretChatCipher();
        var participant = new SecretChatCipher();

        for (int i = 0; i < 300; i += 7) {
            var expected = layer("m".repeat(i), i, i + 1);
            ByteBuf payload = creator.encrypt(key, true, expected);
            assertEquals(expected, participant.decrypt(key, false, payload), "length " + i);
            payload.release();
        }
    }

    static ImmutableDecryptedMessageLayer layer(String text, int inSeqNo, int outSeqNo) {
        byte[] randomBytes = new byte[15];
        new Random(text.length()).nextBytes(randomBytes);

        return DecryptedMessageLayer.builder()
                .randomBytes(Unpooled.wrappedBuffer(randomBytes))
                .layer(SecretTlInfo.LAYER)
                .inSeqNo(inSeqNo)
                .outSeqNo(outSeqNo)
                .message(BaseDecryptedMessage.builder()
                        .randomId(text.hashCode())
                        .ttl(0)
                        .message(text)
                        .build())
                .build();
    }

    static AuthKey randomKey(long seed) {
        byte[] value = new byte[256];
        new Random(seed).nextBytes(value);
        return new AuthKey(Unpooled.wrappedBuffer(value));
    }
}
