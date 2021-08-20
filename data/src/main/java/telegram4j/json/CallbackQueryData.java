package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableCallbackQueryData.class)
@JsonDeserialize(as = ImmutableCallbackQueryData.class)
public interface CallbackQueryData {

    static ImmutableCallbackQueryData.Builder builder() {
        return ImmutableCallbackQueryData.builder();
    }

    String id();

    // NOTE: renamed due matches to generated #from method
    @JsonProperty("from")
    UserData fromUser();

    Optional<MessageData> message();

    @JsonProperty("inline_message_id")
    Optional<String> inlineMessageId();

    @JsonProperty("chat_instance")
    String chatInstance();

    Optional<String> data();

    @JsonProperty("game_short_name")
    Optional<String> gameShortName();
}
