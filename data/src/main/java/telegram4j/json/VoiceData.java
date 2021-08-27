package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableVoiceData.class)
@JsonDeserialize(as = ImmutableVoiceData.class)
public interface VoiceData extends FileFields, MediaFileFields {

    static ImmutableVoiceData.Builder builder() {
        return ImmutableVoiceData.builder();
    }

    @Override
    @JsonProperty("file_id")
    String fileId();

    @Override
    @JsonProperty("file_unique_id")
    String fileUniqueId();

    @Override
    int duration();

    @JsonProperty("mime_type")
    Optional<String> mimeType();

    @Override
    @JsonProperty("file_size")
    Optional<Integer> fileSize();
}
