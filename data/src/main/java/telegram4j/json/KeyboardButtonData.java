package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableKeyboardButtonData.class)
@JsonDeserialize(as = ImmutableKeyboardButtonData.class)
public interface KeyboardButtonData {

    static ImmutableKeyboardButtonData.Builder builder() {
        return ImmutableKeyboardButtonData.builder();
    }

    String text();

    @JsonProperty("request_contact")
    Optional<Boolean> requestContact();

    @JsonProperty("request_location")
    Optional<Boolean> requestLocation();

    @JsonProperty("request_poll")
    Optional<KeyboardButtonPollType> requestPoll();
}
