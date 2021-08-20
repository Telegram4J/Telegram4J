package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;

@Value.Immutable
public interface PassportFileData {

    static ImmutablePassportFileData.Builder builder() {
        return ImmutablePassportFileData.builder();
    }

    @JsonProperty("file_id")
    String fileId();

    @JsonProperty("file_unique_id")
    String fileUniqueId();

    @JsonProperty("file_size")
    int fileSize();

    @JsonProperty("file_date")
    int fileDate();
}
