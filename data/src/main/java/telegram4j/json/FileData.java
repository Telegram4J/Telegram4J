package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableFileData.class)
@JsonDeserialize(as = ImmutableFileData.class)
public interface FileData {

    static ImmutableFileData.Builder builder() {
        return ImmutableFileData.builder();
    }

    @JsonProperty("file_id")
    String fileId();

    @JsonProperty("file_unique_id")
    String fileUniqueId();

    @JsonProperty("file_size")
    Optional<Integer> fileSize();

    @JsonProperty("file_path")
    Optional<String> filePath();
}
