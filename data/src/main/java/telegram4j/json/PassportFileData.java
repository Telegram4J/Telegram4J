package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutablePassportFileData.class)
@JsonDeserialize(as = ImmutablePassportFileData.class)
public interface PassportFileData extends FileFields {

    static ImmutablePassportFileData.Builder builder() {
        return ImmutablePassportFileData.builder();
    }

    @Override
    @JsonProperty("file_id")
    String fileId();

    @Override
    @JsonProperty("file_unique_id")
    String fileUniqueId();

    // always present, but marked as nullable for superclass compatibility
    @Override
    @JsonProperty("file_size")
    Optional<Integer> fileSize();

    @JsonProperty("file_date")
    int fileDate();
}
