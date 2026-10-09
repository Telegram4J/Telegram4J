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
package telegram4j.mtproto.store;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.auth.AuthKey;
import telegram4j.mtproto.secret.RekeyState;
import telegram4j.mtproto.secret.SecretChatState;
import telegram4j.mtproto.secret.SeqNoState;

import java.math.BigInteger;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SecretChatStoreTest {

    @TempDir
    Path directory;

    FileStoreLayout open() {
        var store = new FileStoreLayout(new StoreLayoutImpl(c -> c), directory.resolve("t4j.bin"));
        store.initialize().block();
        // the file layout only persists once the store knows whose session it is
        store.onAuthorization(telegram4j.tl.auth.BaseAuthorization.builder()
                .user(telegram4j.tl.ImmutableBaseUser.builder().id(1337).self(true).build())
                .build()).block();
        store.updateDataCenter(DataCenter.production(DataCenter.Type.REGULAR, 2,
                "149.154.167.50", 443, false, false, false, null)).block();
        return store;
    }

    @Test
    void survivesRestart() {
        var original = ready();

        var store = open();
        store.updateSecretChat(original).block();
        store.close().block();

        // a fresh store over the same file is what a restart looks like
        var restored = open().getSecretChat(original.id()).block();
        assertNotNull(restored, "the chat did not survive the restart");

        assertEquals(original.id(), restored.id());
        assertEquals(original.accessHash(), restored.accessHash());
        assertEquals(original.peerId(), restored.peerId());
        assertEquals(original.creator(), restored.creator());
        assertEquals(original.status(), restored.status());
        assertEquals(original.layer(), restored.layer());
        assertEquals(original.ttl(), restored.ttl());
        assertEquals(original.remainingMessages(), restored.remainingMessages());
        assertEquals(original.keyCreatedAt(), restored.keyCreatedAt());

        assertEquals(original.key().id(), restored.key().id(), "key fingerprint changed");
        assertEquals(original.key().value(), restored.key().value());
        assertEquals(original.oldKey().id(), restored.oldKey().id());

        assertEquals(original.seqNo().inSeqNo(), restored.seqNo().inSeqNo());
        assertEquals(original.seqNo().outSeqNo(), restored.seqNo().outSeqNo());
        assertEquals(original.seqNo().remoteInSeqNo(), restored.seqNo().remoteInSeqNo());
    }

    @Test
    void restoredCountersKeepTheirParity() {
        // the parity is derived from `creator`, so a chat restored with the wrong flag would
        // start writing sequence numbers the peer reads as its own
        var original = ready();
        int expectedNext = original.seqNo().allocate().outSeqNo();

        var store = open();
        store.updateSecretChat(original).block();
        store.close().block();

        var restored = open().getSecretChat(original.id()).block();
        assertNotNull(restored);
        // the same allocation on the restored state must yield the same numbers
        assertEquals(expectedNext, restored.seqNo().allocate().outSeqNo());
    }

    @Test
    void keepsPendingRequestSecret() {
        // a chat we asked for but which nobody has accepted yet is only recoverable if the
        // private exponent outlives the process
        var secret = new BigInteger(1, randomBytes(256, 7));
        var pending = SecretChatState.builder(9001, 42L, 1234L, true)
                .status(SecretChatState.Status.REQUESTED)
                .secret(secret)
                .build();

        var store = open();
        store.updateSecretChat(pending).block();
        store.close().block();

        var restored = open().getSecretChat(9001).block();
        assertNotNull(restored);
        assertEquals(SecretChatState.Status.REQUESTED, restored.status());
        assertEquals(secret, restored.secret());
        assertNull(restored.key());
    }

    @Test
    void forgetsTheSecretOnceKeyed() {
        // the exponent exists only to reach the key; keeping it afterwards would leave the
        // material to re-derive that key in storage for the lifetime of the chat
        var chat = SecretChatState.builder(9002, 42L, 1234L, true)
                .status(SecretChatState.Status.REQUESTED)
                .secret(new BigInteger(1, randomBytes(256, 13)))
                .build()
                .toBuilder()
                .key(new AuthKey(Unpooled.wrappedBuffer(randomBytes(256, 14))), Instant.ofEpochSecond(1_700_000_000L))
                .status(SecretChatState.Status.READY)
                .build();
        assertNull(chat.secret());

        var store = open();
        store.updateSecretChat(chat).block();
        store.close().block();

        var restored = open().getSecretChat(9002).block();
        assertNotNull(restored);
        assertNull(restored.secret(), "the exponent reached storage");
        assertNotNull(restored.key());
    }

    @Test
    void keepsRekeyInProgress() {
        var secret = new BigInteger(1, randomBytes(256, 11));
        var chat = ready().toBuilder()
                .rekey(RekeyState.requested(0x0123456789abcdefL, secret))
                .build();

        var store = open();
        store.updateSecretChat(chat).block();
        store.close().block();

        var restored = open().getSecretChat(chat.id()).block();
        assertNotNull(restored);
        assertEquals(RekeyState.Status.REQUESTED, restored.rekey().status());
        assertEquals(0x0123456789abcdefL, restored.rekey().exchangeId());
        assertEquals(secret, restored.rekey().secret());
    }

    @Test
    void whatTheStoreHandsOutCannotChangeUnderIt() {
        // the store keeps what it was given rather than a copy, so a state that could be mutated
        // would let a caller advance the stored counters without ever writing them back, and
        // leave nothing to roll back to when the write failed
        var store = open();
        store.updateSecretChat(ready()).block();

        var read = store.getSecretChat(4242).block();
        assertNotNull(read);

        var advanced = read.toBuilder().seqNo(read.seqNo().allocate().next()).build();
        assertNotEquals(read.seqNo().outSeqNo(), advanced.seqNo().outSeqNo());

        var stillStored = store.getSecretChat(4242).block();
        assertNotNull(stillStored);
        assertEquals(read.seqNo().outSeqNo(), stillStored.seqNo().outSeqNo(),
                "deriving a new state advanced the stored one");
    }

    @Test
    void deleteRemovesTheChat() {
        var chat = ready();

        var store = open();
        store.updateSecretChat(chat).block();
        store.deleteSecretChat(chat.id()).block();
        store.close().block();

        assertNull(open().getSecretChat(chat.id()).block());
    }

    @Test
    void readsFilesWrittenBeforeSecretChatsExisted() {
        // an existing session file is revision 1 and simply has no chats in it
        var store = open();
        store.close().block();

        var reopened = open();
        assertEquals(0, reopened.getSecretChats().count().block());
        assertEquals(1337, reopened.getSelfId().block());
    }

    static SecretChatState ready() {
        return SecretChatState.builder(4242, 0x1122334455667788L, 999L, true)
                .status(SecretChatState.Status.READY)
                .seqNo(new SeqNoState(true, 5, 7, 4))
                // twice, so that the first key ends up as the old one still accepted in flight
                .key(new AuthKey(Unpooled.wrappedBuffer(randomBytes(256, 1))), Instant.ofEpochSecond(1_700_000_000L))
                .key(new AuthKey(Unpooled.wrappedBuffer(randomBytes(256, 2))), Instant.ofEpochSecond(1_700_000_500L))
                .layer(143)
                .ttl(60)
                .remainingMessages(93)
                .build();
    }

    static byte[] randomBytes(int size, long seed) {
        byte[] bytes = new byte[size];
        new Random(seed).nextBytes(bytes);
        return bytes;
    }
}
