package telegram4j.json.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import telegram4j.json.InlineKeyboardMarkupData;
import telegram4j.json.InputMediaData;

import java.util.Optional;

@Value.Immutable(singleton = true)
@JsonSerialize(as = ImmutableMessageEditReplyMarkup.class)
@JsonDeserialize(as = ImmutableMessageEditReplyMarkup.class)
public interface MessageEditReplyMarkup {

    static ImmutableMessageEditReplyMarkup.Builder builder() {
        return ImmutableMessageEditReplyMarkup.builder();
    }

    @JsonProperty("chat_id")
    Optional<Long> chatId();

    @JsonProperty("message_id")
    Optional<Long> messageId();

    @JsonProperty("inline_message_id")
    Optional<String> inlineMessageId();

    @JsonProperty("reply_markup")
    Optional<InlineKeyboardMarkupData> replyMarkup();
}
