package telegram4j.core;

import telegram4j.core.dispatch.DefaultDispatchMapper;
import telegram4j.core.dispatch.DispatchMapper;
import telegram4j.core.dispatcher.DefaultEventDispatcher;
import telegram4j.core.dispatcher.EventDispatcher;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

/** A {@link TelegramClient}'s resources used in updates handling and events mapping. */
public class ClientResources {

    /** Optimized update check interval. */
    public static final Duration DEFAULT_UPDATE_INTERVAL = Duration.ofMillis(300);

    /** The {@link Supplier} which creates a {@link DefaultDispatchMapper} instances. */
    public static final Supplier<DispatchMapper> DEFAULT_DISPATCH_MAPPER = DefaultDispatchMapper::new;

    /** The {@link Supplier} which creates a {@link DefaultEventDispatcher} instances with default settings. */
    public static final Supplier<EventDispatcher> DEFAULT_EVENT_DISPATCHER = () -> DefaultEventDispatcher.builder().build();

    /** Update check interval. */
    private final Duration updateInterval;
    /** Update event mapper. */
    private final DispatchMapper dispatchMapper;
    /** Event distributor. */
    private final EventDispatcher eventDispatcher;

    /** Constructs a {@link ClientResources} with default settings. */
    public ClientResources() {
        this.updateInterval = DEFAULT_UPDATE_INTERVAL;
        this.dispatchMapper = DEFAULT_DISPATCH_MAPPER.get();
        this.eventDispatcher = DEFAULT_EVENT_DISPATCHER.get();
    }

    public ClientResources(Duration updateInterval, DispatchMapper dispatchMapper, EventDispatcher eventDispatcher) {
        this.updateInterval = Objects.requireNonNull(updateInterval, "updateInterval");
        this.dispatchMapper = Objects.requireNonNull(dispatchMapper, "dispatchMapper");
        this.eventDispatcher = Objects.requireNonNull(eventDispatcher, "eventDispatcher");
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
}
