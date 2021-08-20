package telegram4j.json;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableVoiceChatEndedData.class)
@JsonDeserialize(as = ImmutableVoiceChatEndedData.class)
public interface VoiceChatEndedData {

    static ImmutableVoiceChatEndedData.Builder builder() {
        return ImmutableVoiceChatEndedData.builder();
    }

    int duration();
}
