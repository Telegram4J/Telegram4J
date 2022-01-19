package telegram4j.core;

import reactor.util.annotation.Nullable;
import telegram4j.core.event.EventDispatcher;
import telegram4j.core.util.EntityParser;
import telegram4j.mtproto.MTProtoClientManager;
import telegram4j.mtproto.store.StoreLayout;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/** Shared MTProto telegram client resources. */
public class MTProtoResources {
    private final MTProtoClientManager clientManager;
    private final StoreLayout storeLayout;
    private final EventDispatcher eventDispatcher;
    @Nullable
    private final Function<String, EntityParser> defaultEntityParser;

    MTProtoResources(MTProtoClientManager clientManager, StoreLayout storeLayout, EventDispatcher eventDispatcher,
                     @Nullable Function<String, EntityParser> defaultEntityParser) {
        this.clientManager = Objects.requireNonNull(clientManager, "clientManager");
        this.storeLayout = Objects.requireNonNull(storeLayout, "storeLayout");
        this.eventDispatcher = Objects.requireNonNull(eventDispatcher, "eventDispatcher");
        this.defaultEntityParser = defaultEntityParser;
    }

    /**
     * Gets the MTProto client manager.
     *
     * @return The {@link MTProtoClientManager} client manager.
     */
    public MTProtoClientManager getClientManager() {
        return clientManager;
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
    public Optional<Function<String, EntityParser>> getDefaultEntityParser() {
        return Optional.ofNullable(defaultEntityParser);
    }
}
