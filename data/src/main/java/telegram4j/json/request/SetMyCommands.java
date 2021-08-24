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
@JsonSerialize(as = ImmutableSetMyCommands.class)
@JsonDeserialize(as = ImmutableSetMyCommands.class)
public interface SetMyCommands {

    static ImmutableSetMyCommands.Builder builder() {
        return ImmutableSetMyCommands.builder();
    }

    List<BotCommandData> commands();

    Optional<BotCommandScopeData> scope();

    @JsonProperty("language_code")
    Optional<String> languageCode();
}
