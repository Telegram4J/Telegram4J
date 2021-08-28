package telegram4j.json.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable(singleton = true)
@JsonSerialize(as = ImmutableGetUpdates.class)
@JsonDeserialize(as = ImmutableGetUpdates.class)
public interface GetUpdates {

    static ImmutableGetUpdates.Builder builders() {
        return ImmutableGetUpdates.builder();
    }

    Optional<Integer> offset();

    Optional<Integer> limit();

    Optional<Integer> timeout();

    @JsonProperty("allowed_updates")
    Optional<List<String>> allowedUpdates();
}
