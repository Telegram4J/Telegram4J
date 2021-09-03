package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableVideoData.class)
@JsonDeserialize(as = ImmutableVideoData.class)
public interface VideoData extends FileFields, MediaFileFields, SizedMediaFile {

    static ImmutableVideoData.Builder builder() {
        return ImmutableVideoData.builder();
    }

    @Override
    @JsonProperty("file_id")
    String fileId();

    @Override
    @JsonProperty("file_unique_id")
    String fileUniqueId();

    @Override
    int width();

    @Override
    int height();

    @Override
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
