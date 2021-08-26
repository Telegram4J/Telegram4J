package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableUserProfilePhotosData.class)
@JsonDeserialize(as = ImmutableUserProfilePhotosData.class)
public interface UserProfilePhotosData {

    static ImmutableUserProfilePhotosData.Builder builder() {
        return ImmutableUserProfilePhotosData.builder();
    }

    @JsonProperty("total_count")
    int totalCount();

    List<List<PhotoSizeData>> photos();
}
