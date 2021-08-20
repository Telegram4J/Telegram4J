package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableLocationData.class)
@JsonDeserialize(as = ImmutableLocationData.class)
public interface LocationData {

    static ImmutableLocationData.Builder builder() {
        return ImmutableLocationData.builder();
    }

    float longitude();

    float latitude();

    @JsonProperty("horizontal_accuracy")
    Optional<Float> horizontalAccuracy();

    @JsonProperty("live_period")
    Optional<Integer> livePeriod();

    @JsonProperty("heading")
    Optional<Integer> heading();

    @JsonProperty("proximity_alert_radius")
    Optional<Integer> proximityAlertRadius();
}
