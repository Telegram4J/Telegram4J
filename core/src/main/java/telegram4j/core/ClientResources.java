package telegram4j.core;

import org.immutables.builder.Builder;
import reactor.util.annotation.Nullable;
import telegram4j.core.dispatch.DefaultDispatchMapper;
import telegram4j.core.dispatch.DispatchMapper;
import telegram4j.core.dispatcher.DefaultEventDispatcher;
import telegram4j.core.dispatcher.EventDispatcher;
import telegram4j.core.event.Event;
import telegram4j.core.store.Store;
import telegram4j.core.store.impl.LocalStoreLayout;

import java.time.Duration;
import java.util.function.Supplier;

/** A {@link TelegramClient}'s resources used in updates handling and events mapping. */
public class ClientResources {

    /** Optimized update check interval. */
    public static final Duration DEFAULT_UPDATE_INTERVAL = Duration.ofMillis(300);

    /** The {@link Supplier} which creates a {@link DefaultDispatchMapper} instances. */
    public static final Supplier<DispatchMapper> DEFAULT_DISPATCH_MAPPER = DefaultDispatchMapper::new;

    /** The {@link Supplier} which creates a {@link DefaultEventDispatcher} instances with default settings. */
    public static final Supplier<EventDispatcher> DEFAULT_EVENT_DISPATCHER = DefaultEventDispatcher::create;

    public static final Supplier<Store> DEFAULT_STORE = () -> Store.fromLayout(new LocalStoreLayout());

    /** Update check interval. */
    private final Duration updateInterval;
    /** Update event mapper. */
    private final DispatchMapper dispatchMapper;
    /** Event distributor. */
    private final EventDispatcher eventDispatcher;
    /** Objects store used across Telegram4J. */
    private final Store store;

    /** Constructs a {@link ClientResources} with default settings. */
    public ClientResources() {
        this.updateInterval = DEFAULT_UPDATE_INTERVAL;
        this.dispatchMapper = DEFAULT_DISPATCH_MAPPER.get();
        this.eventDispatcher = DEFAULT_EVENT_DISPATCHER.get();
        this.store = DEFAULT_STORE.get();
    }

    @Builder.Constructor
    ClientResources(@Nullable Duration updateInterval, @Nullable DispatchMapper dispatchMapper,
                    @Nullable EventDispatcher eventDispatcher, @Nullable Store store) {
        this.updateInterval = updateInterval != null ? updateInterval : DEFAULT_UPDATE_INTERVAL;
        this.dispatchMapper = dispatchMapper != null ? dispatchMapper : DEFAULT_DISPATCH_MAPPER.get();
        this.eventDispatcher = eventDispatcher != null ? eventDispatcher : DEFAULT_EVENT_DISPATCHER.get();
        this.store = store != null ? store : DEFAULT_STORE.get();
    }

    public static ClientResourcesBuilder builder() {
        return new ClientResourcesBuilder();
    }

    /**
     * Gets a <a href="https://en.wikipedia.org/wiki/Push_technology#Long_polling">long polling</a>
     * update's fetching interval.
     *
     * @return a {@link Duration} of update interval.
     */
    public Duration getUpdateInterval() {
        return updateInterval;
    }

    /**
     * Gets an updates' mapper used for event mapping
     * for subsequent publishing to {@link EventDispatcher}.
     *
     * @return a updates mapper.
     */
    public DispatchMapper getDispatchMapper() {
        return dispatchMapper;
    }

    /**
     * Gets an event dispatcher that publishes new {@link Event}s after mapping
     * in {@link DispatchMapper}
     *
     * @return an event dispatcher that distributes events to subscribers
     */
    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    /**
     * Gets an object {@link Store store} used to save entities after receiving updates
     *
     * @return an object store.
     */
    public Store getStore() {
        return store;
    }
}
