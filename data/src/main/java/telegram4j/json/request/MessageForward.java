package telegram4j.json.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import telegram4j.json.api.ChatId;
import telegram4j.json.api.Id;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableMessageForward.class)
@JsonDeserialize(as = ImmutableMessageForward.class)
public interface MessageForward {

    static ImmutableMessageForward.Builder builder() {
        return ImmutableMessageForward.builder();
    }

    @JsonProperty("chat_id")
    ChatId chatId();

    @JsonProperty("from_chat_id")
    ChatId fromChatId();

    @JsonProperty("disable_notification")
    Optional<Boolean> disableNotification();

    @JsonProperty("message_id")
    Id messageId();
}
