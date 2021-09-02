package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import telegram4j.json.api.ChatId;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableBotCommandScopeData.class)
@JsonDeserialize(as = ImmutableBotCommandScopeData.class)
public interface BotCommandScopeData {

    static ImmutableBotCommandScopeData.Builder builder() {
        return ImmutableBotCommandScopeData.builder();
    }

    BotCommandScopeType type();

    @JsonProperty("chat_id")
    Optional<ChatId> chatId();

    @JsonProperty("user_id")
    Optional<Long> userId();
}
