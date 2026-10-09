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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** What both outboxes owe the protocol, checked against each of them. */
class SecretChatOutboxTest {

    static final int CHAT_ID = 4242;

    @TempDir
    Path directory;

    /** The implementations under test, each built the same way. */
    SecretChatOutbox memory(int capacity) {
        return new SecretChatOutboxImpl(capacity);
    }

    SecretChatOutbox file(int capacity) {
        return new FileSecretChatOutbox(directory, capacity, true);
    }

    @Test
    void keepsTheNewestAndDropsTheOldest() {
        for (var outbox : both(3)) {
            for (int i = 0; i < 10; i++) {
                outbox.add(CHAT_ID, sent(2 * i + 1)).block();
            }

            assertNull(outbox.get(CHAT_ID, 13).block(), "an old message was kept");
            assertNotNull(outbox.get(CHAT_ID, 15).block());
            assertNotNull(outbox.get(CHAT_ID, 19).block(), "the newest was dropped");
        }
    }

    @Test
    void keepsNothingWithoutCapacity() {
        // which is how a caller turns answering resend requests off
        for (var outbox : both(0)) {
            outbox.add(CHAT_ID, sent(1)).block();
            assertNull(outbox.get(CHAT_ID, 1).block());
        }
    }

    @Test
    void keepsChatsApart() {
        for (var outbox : both(4)) {
            outbox.add(CHAT_ID, sent(1)).block();
            outbox.add(-CHAT_ID, sent(3)).block();

            assertNull(outbox.get(CHAT_ID, 3).block());
            assertNotNull(outbox.get(-CHAT_ID, 3).block());
        }
    }

    @Test
    void forgetsAChatThatIsGone() {
        for (var outbox : both(4)) {
            outbox.add(CHAT_ID, sent(1)).block();
            outbox.add(-CHAT_ID, sent(1)).block();
            outbox.deleteAll(CHAT_ID).block();

            assertNull(outbox.get(CHAT_ID, 1).block());
            assertNotNull(outbox.get(-CHAT_ID, 1).block(), "forgot a chat that was not asked about");
        }
    }

    @Test
    void holdsWhatItWasGivenRatherThanAViewOfIt() {
        byte[] payload = {1, 2, 3};
        var sent = new SecretChatOutbox.Sent(1, 0, 143, false, payload);
        payload[0] = 4;

        assertArrayEquals(new byte[]{1, 2, 3}, sent.payload());
        sent.payload()[0] = 4;
        assertArrayEquals(new byte[]{1, 2, 3}, sent.payload());
    }

    @Test
    void onlyTheFileBasedOneOutlivesTheProcess() {
        memory(4).add(CHAT_ID, sent(1)).block();
        assertNull(memory(4).get(CHAT_ID, 1).block());

        file(4).add(CHAT_ID, sent(1)).block();
        assertNotNull(file(4).get(CHAT_ID, 1).block(), "the file outbox lost a message");
    }

    @Test
    void theFilesAreThereToBeLookedAt() throws Exception {
        var outbox = file(4);
        outbox.add(CHAT_ID, sent(1)).block();

        var file = directory.resolve("messages" + CHAT_ID + ".json");
        assertTrue(Files.exists(file), "expected " + file);
        assertTrue(Files.readString(file).contains("\"outSeqNo\""));
    }

    SecretChatOutbox[] both(int capacity) {
        return new SecretChatOutbox[]{memory(capacity), file(capacity)};
    }

    static SecretChatOutbox.Sent sent(int outSeqNo) {
        return new SecretChatOutbox.Sent(outSeqNo, outSeqNo + 1, 143, false,
                new byte[]{(byte) outSeqNo});
    }
}
