package telegram4j.json;

import org.immutables.value.Value;

@Value.Immutable
public interface VoiceChatEndedData {

    static ImmutableVoiceChatEndedData.Builder builder() {
        return ImmutableVoiceChatEndedData.builder();
    }

    int duration();
}
