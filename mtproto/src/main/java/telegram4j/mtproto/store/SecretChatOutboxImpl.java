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

import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * An outbox which keeps the last messages of each chat in memory.
 *
 * <p> Enough for a peer that loses a message while both sides are running, which is the common
 * case: a peer notices a hole on the very next message it receives. A hole opened before a restart
 * outlives this outbox and cannot be answered; {@link FileSecretChatOutbox} is the one that
 * survives that.
 */
public class SecretChatOutboxImpl implements SecretChatOutbox {

    /**
     * How many messages one chat keeps when the caller expresses no preference.
     *
     * <p> Sized against what it is for: a request normally names something recent, and anything
     * old enough to fall out was going to be asked for long after the peer gave up on the chat.
     */
    public static final int DEFAULT_CAPACITY = 32;

    protected final Map<Integer, NavigableMap<Integer, Sent>> messages = new ConcurrentHashMap<>();
    protected final int capacity;

    public SecretChatOutboxImpl() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * @param capacity How many messages each chat keeps, oldest dropped first; zero keeps none,
     * which turns answering resend requests off.
     */
    public SecretChatOutboxImpl(int capacity) {
        this.capacity = Math.max(0, capacity);
    }

    @Override
    public Mono<Void> add(int chatId, Sent message) {
        return Mono.fromRunnable(() -> put(chatId, message));
    }

    @Override
    public Mono<Sent> get(int chatId, int outSeqNo) {
        return Mono.fromSupplier(() -> {
            var chat = messages.get(chatId);
            return chat == null ? null : chat.get(outSeqNo);
        });
    }

    @Override
    public Mono<Void> deleteAll(int chatId) {
        return Mono.fromRunnable(() -> remove(chatId));
    }

    protected void put(int chatId, Sent message) {
        if (capacity == 0) {
            return;
        }

        var chat = messages.computeIfAbsent(chatId, id -> new ConcurrentSkipListMap<>());
        // the trim has to see its own insertion, and two sends racing would otherwise be able to
        // leave the chat over capacity
        synchronized (chat) {
            chat.put(message.outSeqNo(), message);
            while (chat.size() > capacity) {
                chat.pollFirstEntry();
            }
        }
    }

    protected void remove(int chatId) {
        messages.remove(chatId);
    }
}
