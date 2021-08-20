package telegram4j.json.methods;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import telegram4j.json.UserData;

@Value.Immutable
@JsonSerialize(as = ImmutableGetMeRequest.class)
@JsonDeserialize(as = ImmutableGetMeRequest.class)
public interface GetMeRequest {

    static ImmutableGetMeRequest.Builder builder() {
        return ImmutableGetMeRequest.builder();
    }

    boolean ok();

    UserData result();
}
