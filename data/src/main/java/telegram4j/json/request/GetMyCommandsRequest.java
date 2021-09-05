package telegram4j.json.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import telegram4j.json.BotCommandScopeData;

import java.util.Optional;

@Value.Immutable(singleton = true)
@JsonSerialize(as = ImmutableGetMyCommandsRequest.class)
@JsonDeserialize(as = ImmutableGetMyCommandsRequest.class)
public interface GetMyCommandsRequest {

    static ImmutableGetMyCommandsRequest.Builder builder() {
        return ImmutableGetMyCommandsRequest.builder();
    }

    Optional<BotCommandScopeData> scope();

    @JsonProperty("language_code")
    Optional<String> languageCode();
}
