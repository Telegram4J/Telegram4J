package telegram4j.json.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableGetChat.class)
@JsonDeserialize(as = ImmutableGetChat.class)
public interface GetChat {

    static ImmutableGetChat.Builder builder() {
        return ImmutableGetChat.builder();
    }

    @JsonProperty("chat_id")
    long chatId();
}
