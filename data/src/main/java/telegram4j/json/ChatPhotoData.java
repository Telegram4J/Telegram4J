package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableChatPhotoData.class)
@JsonDeserialize(as = ImmutableChatPhotoData.class)
public interface ChatPhotoData {

    static ImmutableChatPhotoData.Builder builder() {
        return ImmutableChatPhotoData.builder();
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
