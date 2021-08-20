package telegram4j.json;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableProximityAlertTriggeredData.class)
@JsonDeserialize(as = ImmutableProximityAlertTriggeredData.class)
public interface ProximityAlertTriggeredData {

    static ImmutableProximityAlertTriggeredData.Builder builder() {
        return ImmutableProximityAlertTriggeredData.builder();
    }

    UserData traveler();

    UserData watcher();

    int distance();
}
