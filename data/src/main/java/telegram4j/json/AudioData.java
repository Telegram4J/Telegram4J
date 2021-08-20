package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableAudioData.class)
@JsonDeserialize(as = ImmutableAudioData.class)
public interface AudioData {

    static ImmutableAudioData.Builder builder() {
        return ImmutableAudioData.builder();
    }

    @JsonProperty("file_id")
    String fileId();

    @JsonProperty("file_unique_id")
    String fileUniqueId();

    int duration();

    Optional<String> performer();

    Optional<String> title();

    @JsonProperty("file_name")
    Optional<String> fileName();

    @JsonProperty("mime_type")
    Optional<String> mimeType();

    @JsonProperty("file_size")
    Optional<Integer> fileSize();

    Optional<PhotoSizeData> thumb();
}
