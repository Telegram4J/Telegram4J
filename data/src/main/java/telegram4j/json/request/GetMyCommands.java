package telegram4j.json.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import telegram4j.json.BotCommandScopeData;

import java.util.Optional;

@Value.Immutable(singleton = true)
@JsonSerialize(as = ImmutableGetMyCommands.class)
@JsonDeserialize(as = ImmutableGetMyCommands.class)
public interface GetMyCommands {

    static ImmutableGetMyCommands.Builder builder() {
        return ImmutableGetMyCommands.builder();
    }

    Optional<BotCommandScopeData> scope();

    @JsonProperty("language_code")
    Optional<String> languageCode();
}
