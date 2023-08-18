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
package telegram4j.core.retriever;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.util.Id;
import telegram4j.mtproto.store.StoreLayout;

import java.util.Objects;
import java.util.function.Function;

/** Represents retrieval strategy to use for a given {@link MTProtoTelegramClient}. */
@FunctionalInterface
public interface EntityRetrievalStrategy extends Function<MTProtoTelegramClient, EntityRetriever> {

    /** Strategy that uses Telegram RPC API to retrieve objects. */
    EntityRetrievalStrategy RPC = RpcEntityRetriever::new;

    /** Strategy that uses {@link StoreLayout} cache to retrieve objects. */
    EntityRetrievalStrategy STORE = StoreEntityRetriever::new;

    /**
     * Strategy that consists of retrieving entities from {@link StoreLayout store} and
     * then send requests to Telegram RPC API if not found.
     * This is default strategy for the {@link MTProtoTelegramClient}.
     */
    EntityRetrievalStrategy STORE_FALLBACK_RPC = fallback(STORE, RPC);

    /**
     * Factory method to create fallback strategy from two given strategies.
     *
     * @param first The first delegate strategy to use.
     * @param second The second delegate strategy to use.
     * @return A new fallback strategy.
     */
    static EntityRetrievalStrategy fallback(EntityRetrievalStrategy first, EntityRetrievalStrategy second) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        return client -> new FallbackEntityRetriever(first.apply(client), second.apply(client));
    }

    /**
     * Factory method to create strategy which have settings to configure behavior
     * of {@link EntityRetriever#getUserById(Id)} and {@link EntityRetriever#getChatById(Id)} methods.
     *
     * @param delegateStrategy The delegate strategy to use.
     * @param chatPreference The option which controls result objects of {@link EntityRetriever#getChatById(Id)}.
     * @param userPreference The option which controls result objects of {@link EntityRetriever#getUserById(Id)}.
     * @return A new strategy with preferable methods.
     */
    static EntityRetrievalStrategy preferred(EntityRetrievalStrategy delegateStrategy,
                                             PreferredEntityRetriever.Setting chatPreference,
                                             PreferredEntityRetriever.Setting userPreference) {
        Objects.requireNonNull(delegateStrategy);
        Objects.requireNonNull(chatPreference);
        Objects.requireNonNull(userPreference);
        return client -> new PreferredEntityRetriever(delegateStrategy.apply(client), chatPreference, userPreference);
    }
}
