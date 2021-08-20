package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableVideoNoteData.class)
@JsonDeserialize(as = ImmutableVideoNoteData.class)
public interface VideoNoteData {

    static ImmutableVideoNoteData.Builder builder() {
        return ImmutableVideoNoteData.builder();
    }

    @JsonProperty("file_id")
    String fileId();

    @JsonProperty("file_unique_id")
    String fileUniqueId();

    int length();

    // in seconds
    int duration();

    Optional<PhotoSizeData> thumb();

    @JsonProperty("file_size")
    Optional<Integer> fileSize();
}
