package telegram4j.core;

import telegram4j.core.dispatch.DefaultDispatchMapper;
import telegram4j.core.dispatch.DispatchMapper;
import telegram4j.core.dispatcher.DefaultEventDispatcher;
import telegram4j.core.dispatcher.EventDispatcher;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

public class ClientResources {

    public static final Duration DEFAULT_UPDATE_INTERVAL = Duration.ofMillis(300);

    public static final Supplier<DispatchMapper> DEFAULT_DISPATCH_MAPPER = DefaultDispatchMapper::new;

    public static final Supplier<EventDispatcher> DEFAULT_EVENT_DISPATCHER = () -> DefaultEventDispatcher.builder().build();

    /** Update check interval. */
    private final Duration updateInterval;
    /** Update event mapper. */
    private final DispatchMapper dispatchMapper;
    /** Event distributor. */
    private final EventDispatcher eventDispatcher;

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

    public Duration getUpdateInterval() {
        return updateInterval;
    }

    public DispatchMapper getDispatchMapper() {
        return dispatchMapper;
    }

    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }
}
