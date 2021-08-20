package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableChatData.class)
@JsonDeserialize(as = ImmutableChatData.class)
public interface ChatPhotoData {

    static ImmutableChatData.Builder builder() {
        return ImmutableChatData.builder();
    }

    @JsonProperty("small_file_id")
    String smallFileId();

    @JsonProperty("small_file_unique_id")
    String smallFileUniqueId();

    @JsonProperty("big_file_id")
    String bigFileId();

    @JsonProperty("big_file_unique_id")
    String bigFileUniqueId();
}
