package telegram4j.json.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import telegram4j.json.api.ChatId;
import telegram4j.json.api.Id;

@Value.Immutable
@JsonSerialize(as = ImmutableDeleteMessage.class)
@JsonDeserialize(as = ImmutableDeleteMessage.class)
public interface DeleteMessage {

    static ImmutableDeleteMessage.Builder builder() {
        return ImmutableDeleteMessage.builder();
    }

    @JsonProperty("chat_id")
    ChatId chatId();

    @JsonProperty("message_id")
    Id messageId();
}
