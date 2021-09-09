package telegram4j.json.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import telegram4j.json.api.Id;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableGetUserProfilePhotosRequest.class)
@JsonDeserialize(as = ImmutableGetUserProfilePhotosRequest.class)
public interface GetUserProfilePhotosRequest {

    static ImmutableGetUserProfilePhotosRequest.Builder builder() {
        return ImmutableGetUserProfilePhotosRequest.builder();
    }

    @JsonProperty("user_id")
    Id userId();

    Optional<Integer> offset();

    Optional<Integer> limit();
}
