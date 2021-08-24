package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableStickerData.class)
@JsonDeserialize(as = ImmutableStickerData.class)
public interface StickerData extends FileFields, SizedMediaFile {

    static ImmutableStickerData.Builder builder() {
        return ImmutableStickerData.builder();
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

    @JsonProperty("is_animated")
    boolean isAnimated();

    Optional<PhotoSizeData> thumb();

    Optional<String> emoji();

    @JsonProperty("set_name")
    Optional<String> setName();

    @JsonProperty("mask_position")
    Optional<String> maskPosition();

    @Override
    @JsonProperty("file_size")
    Optional<Integer> fileSize();
}
