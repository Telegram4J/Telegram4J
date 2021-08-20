package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;

@Value.Immutable
public interface MessageAutoDeleteTimerChangedData {

    static ImmutableMessageAutoDeleteTimerChangedData.Builder builder() {
        return ImmutableMessageAutoDeleteTimerChangedData.builder();
    }

    @JsonProperty("message_auto_delete_time")
    int messageAutoDeleteTime();
}
