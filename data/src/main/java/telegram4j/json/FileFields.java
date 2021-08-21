package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public interface FileFields {

    @JsonProperty("file_id")
    String fileId();

    @JsonProperty("file_unique_id")
    String fileUniqueId();

    @JsonProperty("file_size")
    Optional<Integer> fileSize();
}
