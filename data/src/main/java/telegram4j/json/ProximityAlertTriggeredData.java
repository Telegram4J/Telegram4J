package telegram4j.json;

import org.immutables.value.Value;

@Value.Immutable
public interface ProximityAlertTriggeredData {

    static ImmutableProximityAlertTriggeredData.Builder builder() {
        return ImmutableProximityAlertTriggeredData.builder();
    }

    UserData traveler();

    UserData watcher();

    int distance();
}
