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
