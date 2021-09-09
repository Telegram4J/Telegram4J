package telegram4j.json.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import telegram4j.json.BotCommandScopeData;

import java.util.Optional;

@Value.Immutable(singleton = true)
@JsonSerialize(as = ImmutableDeleteMyCommandsRequest.class)
@JsonDeserialize(as = ImmutableDeleteMyCommandsRequest.class)
public interface DeleteMyCommandsRequest {

    static ImmutableDeleteMyCommandsRequest.Builder builder() {
        return ImmutableDeleteMyCommandsRequest.builder();
    }

    Optional<BotCommandScopeData> scope();

    @JsonProperty("language_code")
    Optional<String> languageCode();
}
