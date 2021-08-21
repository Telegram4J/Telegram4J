package telegram4j.core;

import java.time.Duration;
import java.util.Objects;

public class ClientResources {

    public static final Duration DEFAULT_UPDATE_INTERVAL = Duration.ofMillis(30);

    /** Update check interval. */
    private final Duration updateInterval;

    public ClientResources() {
        this.updateInterval = DEFAULT_UPDATE_INTERVAL;
    }

    public ClientResources(Duration updateInterval) {
        this.updateInterval = Objects.requireNonNull(updateInterval, "updateInterval");
    }

    public Duration getUpdateInterval() {
        return updateInterval;
    }
}
