package telegram4j.core;

import telegram4j.core.dispatch.DefaultDispatchMapper;
import telegram4j.core.dispatch.DispatchMapper;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

public class ClientResources {

    public static final Duration DEFAULT_UPDATE_INTERVAL = Duration.ofMillis(30);

    public static final Supplier<DispatchMapper> DEFAULT_DISPATCH_MAPPER = DefaultDispatchMapper::new;

    /** Update check interval. */
    private final Duration updateInterval;

    private final DispatchMapper dispatchMapper;

    public ClientResources() {
        this.updateInterval = DEFAULT_UPDATE_INTERVAL;
        this.dispatchMapper = DEFAULT_DISPATCH_MAPPER.get();
    }

    public ClientResources(Duration updateInterval, DispatchMapper dispatchMapper) {
        this.updateInterval = Objects.requireNonNull(updateInterval, "updateInterval");
        this.dispatchMapper = Objects.requireNonNull(dispatchMapper, "dispatchMapper");
    }

    public Duration getUpdateInterval() {
        return updateInterval;
    }

    public DispatchMapper getDispatchMapper() {
        return dispatchMapper;
    }
}
