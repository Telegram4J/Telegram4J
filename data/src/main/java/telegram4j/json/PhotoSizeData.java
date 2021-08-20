package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutablePhotoSizeData.class)
@JsonDeserialize(as = ImmutablePhotoSizeData.class)
public interface PhotoSizeData {

    static ImmutablePhotoSizeData.Builder builder() {
        return ImmutablePhotoSizeData.builder();
    }

    @JsonProperty("file_id")
    String fileId();

    @JsonProperty("file_unique_id")
    String fileUniqueId();

    int width();

    int height();

    @JsonProperty("file_size")
    Optional<Integer> fileSize();
}
