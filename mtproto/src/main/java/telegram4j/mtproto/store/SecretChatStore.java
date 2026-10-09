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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.mtproto.secret.SecretChatState;

/**
 * Storage for secret chats, kept apart from {@link StoreLayout} because what it stores is not
 * the same kind of thing.
 *
 * <p> The rest of the store is a cache of what the server already knows, and losing it costs a
 * round trip. A secret chat exists only on the devices holding its key, so this is the primary
 * copy: losing it loses the conversation, and losing part of it is worse still, since a chat
 * whose key survived but whose sequence numbers did not is discarded by the peer as soon as it
 * speaks. Implementations must therefore treat these methods as durable writes rather than as
 * cache updates.
 *
 * <p> Only a client that opens secret chats needs an implementation of this.
 */
public interface SecretChatStore {

    /**
     * Retrieve the state of the secret chat with the specified id.
     *
     * @param chatId The {@link SecretChatState#id() id} of the secret chat.
     * @return A {@link Mono} emitting on successful completion the {@link SecretChatState},
     * or empty if no such chat is known.
     */
    Mono<SecretChatState> getSecretChat(int chatId);

    /**
     * Retrieve every known secret chat, including the ones still awaiting acceptance.
     *
     * <p> Used on startup, since nothing about a secret chat can be fetched from the server.
     *
     * @return A {@link Flux} emitting every stored {@link SecretChatState}.
     */
    Flux<SecretChatState> getSecretChats();

    /**
     * Stores the state of a secret chat, replacing any previous state of the same chat.
     *
     * @implSpec The implementation must make the state durable before the returned {@link Mono}
     * completes. Callers rely on this to persist an outgoing sequence number before the message
     * bearing it is sent; a message sent but not recorded leaves the peer permanently ahead and
     * ends the chat.
     *
     * @param state The state to store.
     * @return A {@link Mono} completing when the state is durable.
     */
    Mono<Void> updateSecretChat(SecretChatState state);

    /**
     * Deletes a secret chat and its keys.
     *
     * @implSpec The implementation must not retain the chat once the returned {@link Mono}
     * completes, and a persistent implementation must have removed it from its storage by then.
     * This is not a secure erase: the bytes of the key may survive in freed heap and in whatever
     * the filesystem does with the blocks the old file occupied.
     *
     * @param chatId The {@link SecretChatState#id() id} of the secret chat.
     * @return A {@link Mono} completing when the operation is done.
     */
    Mono<Void> deleteSecretChat(int chatId);
}
