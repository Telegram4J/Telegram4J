package telegram4j.json.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import telegram4j.json.BotCommandData;
import telegram4j.json.BotCommandScopeData;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableSetMyCommandsRequest.class)
@JsonDeserialize(as = ImmutableSetMyCommandsRequest.class)
public interface SetMyCommandsRequest {

    static ImmutableSetMyCommandsRequest.Builder builder() {
        return ImmutableSetMyCommandsRequest.builder();
    }

    List<BotCommandData> commands();

    Optional<BotCommandScopeData> scope();

    @JsonProperty("language_code")
    Optional<String> languageCode();
}
