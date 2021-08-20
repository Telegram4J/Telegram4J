package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
public interface VoiceData {

    static ImmutableVoiceData.Builder builder() {
        return ImmutableVoiceData.builder();
    }

    @JsonProperty("file_id")
    String fileId();

    @JsonProperty("file_unique_id")
    String fileUniqueId();

    // in seconds
    int duration();

    @JsonProperty("mime_type")
    Optional<String> mimeType();

    @JsonProperty("file_size")
    Optional<Integer> fileSize();
}
