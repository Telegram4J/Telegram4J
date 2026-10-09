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

import reactor.core.publisher.Mono;
import telegram4j.mtproto.secret.SecretChatState;

import java.util.Objects;

/**
 * The messages a secret chat has sent, kept so that it can send them again.
 *
 * <p> Sequence numbers leave the peer no way to step over a message it never got: it stops at the
 * hole and holds everything that follows, so one lost message silently stops the conversation in
 * that direction while both sides still believe the chat works. The peer says what it is missing
 * with {@code decryptedMessageActionResend}, and only a side which kept the message can answer.
 *
 * <p> Unlike {@link SecretChatState}, nothing here is part of the protocol: this is a cache with
 * a retention policy, and losing all of it costs the ability to answer a resend request and
 * nothing else. That is why it is kept apart from the chat state, whose whole purpose is that a sequence
 * number reaches the disk before the message carrying it reaches the peer. An implementation may
 * therefore keep as much or as little as it likes, and may lose writes without telling anyone.
 *
 * <p> Nothing is ever offered in place of a message an outbox does not have. Such a stand-in has
 * to carry some {@code in_seq_no} and layer, and those are not free to choose: a peer stopped at a
 * hole is holding the messages after it, and both fields are checked against theirs. TDLib records
 * what the stand-in carried and then replays what it was holding through the same check, where
 * anything lower fails as not monotonic — an error it answers by discarding the chat. The only
 * values certain to pass are those of the lost message's immediate neighbours, which is why
 * {@link #get} is the whole of what a caller needs: it asks for those two by number. Anything
 * further away is a guess that ends the chat when it is wrong, so it is left to a caller who takes
 * that risk knowingly.
 *
 * <p> Implementations must be safe for concurrent use.
 *
 * @see SecretChatOutboxImpl
 * @see FileSecretChatOutbox
 * @see <a href="https://core.telegram.org/api/end-to-end/seq_no">Sequence numbers in Secret Chats</a>
 */
public interface SecretChatOutbox {

    /**
     * Records a message that has been sent.
     *
     * @param chatId The chat it was sent to.
     * @param message The message, as it went out.
     * @return A {@link Mono} completing when it has been taken.
     */
    Mono<Void> add(int chatId, Sent message);

    /**
     * Looks up a message by the number the peer quotes.
     *
     * @param chatId The chat.
     * @param outSeqNo The wire {@code out_seq_no} of the message.
     * @return A {@link Mono} emitting the message, or empty when it is no longer kept.
     */
    Mono<Sent> get(int chatId, int outSeqNo);

    /**
     * Forgets everything kept for one chat, which is what a chat being torn down leaves to do.
     *
     * @param chatId The chat.
     * @return A {@link Mono} completing when it is gone.
     */
    Mono<Void> deleteAll(int chatId);

    /**
     * One message as it was sent.
     *
     * @param outSeqNo The wire {@code out_seq_no} it carries, which is what identifies it to the peer.
     * @param inSeqNo The wire {@code in_seq_no} it carries.
     * @param layer The layer it announced.
     * @param service Whether it went out as {@code messages.sendEncryptedService}, which decides
     * the method a replay has to use.
     * @param payload The encrypted body, as handed to the server. Sending this again unchanged is
     * the only way a message can be sent again at all: everything the peer checks it against is
     * inside it, and it is already encrypted under the chat key, so keeping it gives away nothing
     * that keeping the key does not.
     */
    record Sent(int outSeqNo, int inSeqNo, int layer, boolean service, byte[] payload) {

        public Sent {
            payload = Objects.requireNonNull(payload, "payload").clone();
        }

        @Override
        public byte[] payload() {
            return payload.clone();
        }

        @Override
        public String toString() {
            return "Sent{outSeqNo=" + outSeqNo + ", inSeqNo=" + inSeqNo + ", layer=" + layer
                    + ", service=" + service + ", payload=" + payload.length + " bytes}";
        }
    }
}
