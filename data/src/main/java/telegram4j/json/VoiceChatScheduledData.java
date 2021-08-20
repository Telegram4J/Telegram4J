package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;

@Value.Immutable
public interface VoiceChatScheduledData {

    static ImmutableVoiceChatScheduledData.Builder builder() {
        return ImmutableVoiceChatScheduledData.builder();
    }

    @JsonProperty("start_date")
    int startDate();
}
