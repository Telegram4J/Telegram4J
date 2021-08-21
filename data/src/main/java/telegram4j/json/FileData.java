package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableFileData.class)
@JsonDeserialize(as = ImmutableFileData.class)
public interface FileData extends FileFields {

    static ImmutableFileData.Builder builder() {
        return ImmutableFileData.builder();
    }

    @Override
    @JsonProperty("file_id")
    String fileId();

    @Override
    @JsonProperty("file_unique_id")
    String fileUniqueId();

    @Override
    @JsonProperty("file_size")
    Optional<Integer> fileSize();

    @JsonProperty("file_path")
    Optional<String> filePath();
}
