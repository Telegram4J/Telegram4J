package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableVoiceChatScheduledData.class)
@JsonDeserialize(as = ImmutableVoiceChatScheduledData.class)
public interface VoiceChatScheduledData {

    static ImmutableVoiceChatScheduledData.Builder builder() {
        return ImmutableVoiceChatScheduledData.builder();
    }

    @JsonProperty("start_date")
    int startDate();
}
