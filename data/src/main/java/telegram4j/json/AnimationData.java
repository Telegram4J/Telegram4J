package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableAnimationData.class)
@JsonDeserialize(as = ImmutableAnimationData.class)
public interface AnimationData extends FileFields {

    static ImmutableAnimationData.Builder builder() {
        return ImmutableAnimationData.builder();
    }

    @Override
    @JsonProperty("file_id")
    String fileId();

    @Override
    @JsonProperty("file_unique_id")
    String fileUniqueId();

    int width();

    int height();

    // in seconds
    int duration();

    Optional<PhotoSizeData> thumb();

    @JsonProperty("file_name")
    Optional<String> fileName();

    @JsonProperty("mime_type")
    Optional<String> mimeType();

    @Override
    @JsonProperty("file_size")
    Optional<Integer> fileSize();
}
