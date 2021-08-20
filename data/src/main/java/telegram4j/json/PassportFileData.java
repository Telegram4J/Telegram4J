package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutablePassportFileData.class)
@JsonDeserialize(as = ImmutablePassportFileData.class)
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
