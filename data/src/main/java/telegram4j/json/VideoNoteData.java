package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableVideoNoteData.class)
@JsonDeserialize(as = ImmutableVideoNoteData.class)
public interface VideoNoteData extends FileFields, MediaFileFields {

    static ImmutableVideoNoteData.Builder builder() {
        return ImmutableVideoNoteData.builder();
    }

    @Override
    @JsonProperty("file_id")
    String fileId();

    @Override
    @JsonProperty("file_unique_id")
    String fileUniqueId();

    int length();

    @Override
    int duration();

    Optional<PhotoSizeData> thumb();

    @Override
    @JsonProperty("file_size")
    Optional<Integer> fileSize();
}
