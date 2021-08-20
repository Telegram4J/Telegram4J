package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableMessageAutoDeleteTimerChangedData.class)
@JsonDeserialize(as = ImmutableMessageAutoDeleteTimerChangedData.class)
public interface MessageAutoDeleteTimerChangedData {

    static ImmutableMessageAutoDeleteTimerChangedData.Builder builder() {
        return ImmutableMessageAutoDeleteTimerChangedData.builder();
    }

    @JsonProperty("message_auto_delete_time")
    int messageAutoDeleteTime();
}
