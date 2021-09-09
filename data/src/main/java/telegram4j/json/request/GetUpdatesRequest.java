package telegram4j.json.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable(singleton = true)
@JsonSerialize(as = ImmutableGetUpdatesRequest.class)
@JsonDeserialize(as = ImmutableGetUpdatesRequest.class)
public interface GetUpdatesRequest {

    static ImmutableGetUpdatesRequest.Builder builders() {
        return ImmutableGetUpdatesRequest.builder();
    }

    Optional<Integer> offset();

    Optional<Integer> limit();

    Optional<Integer> timeout();

    @JsonProperty("allowed_updates")
    Optional<List<String>> allowedUpdates();
}
