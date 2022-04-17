package telegram4j.core;

import reactor.netty.http.client.HttpClient;
import reactor.util.annotation.Nullable;
import telegram4j.core.event.EventDispatcher;
import telegram4j.core.util.parser.EntityParserFactory;
import telegram4j.mtproto.store.StoreLayout;

import java.util.Objects;
import java.util.Optional;

/** Shared MTProto telegram client resources. */
public final class MTProtoResources {
    private final StoreLayout storeLayout;
    private final EventDispatcher eventDispatcher;
    @Nullable
    private final EntityParserFactory defaultEntityParser;
    private final HttpClient httpClient;

    MTProtoResources(StoreLayout storeLayout, EventDispatcher eventDispatcher,
                     @Nullable EntityParserFactory defaultEntityParser, HttpClient httpClient) {
        this.storeLayout = Objects.requireNonNull(storeLayout, "storeLayout");
        this.eventDispatcher = Objects.requireNonNull(eventDispatcher, "eventDispatcher");
        this.defaultEntityParser = defaultEntityParser;
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    /**
     * Gets the global entity storage.
     *
     * @return The {@link StoreLayout} entity storage.
     */
    public StoreLayout getStoreLayout() {
        return storeLayout;
    }

    /**
     * Gets the event dispatcher which distributes updates to subscribers.
     *
     * @return The {@link EventDispatcher} event dispatcher.
     */
    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    /**
     * Gets the default factory of message entity parser, used if
     * a spec doesn't set own parser, if present.
     *
     * @return The factory of message entity parser, if present.
     */
    public Optional<EntityParserFactory> getDefaultEntityParser() {
        return Optional.ofNullable(defaultEntityParser);
    }

    /**
     * Gets http client for downloading non-proxied by telegram web files.
     *
     * @return The http client for downloading web files.
     */
    public HttpClient getHttpClient() {
        return httpClient;
    }
}
