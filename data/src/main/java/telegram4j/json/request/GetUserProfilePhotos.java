package telegram4j.json.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableGetUserProfilePhotos.class)
@JsonDeserialize(as = ImmutableGetUserProfilePhotos.class)
public interface GetUserProfilePhotos {

    static ImmutableGetUserProfilePhotos.Builder builder() {
        return ImmutableGetUserProfilePhotos.builder();
    }

    @JsonProperty("user_id")
    long userId();

    Optional<Integer> offset();

    Optional<Integer> limit();
}
