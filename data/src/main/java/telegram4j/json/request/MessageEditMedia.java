package telegram4j.json.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import telegram4j.json.InlineKeyboardMarkupData;
import telegram4j.json.InputMediaData;
import telegram4j.json.MessageEntityData;
import telegram4j.json.ParseMode;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableMessageEditMedia.class)
@JsonDeserialize(as = ImmutableMessageEditMedia.class)
public interface MessageEditMedia {

    static ImmutableMessageEditMedia.Builder builder() {
        return ImmutableMessageEditMedia.builder();
    }

    @JsonProperty("chat_id")
    Optional<Long> chatId();

    @JsonProperty("message_id")
    Optional<Long> messageId();

    @JsonProperty("inline_message_id")
    Optional<String> inlineMessageId();

    InputMediaData media();

    @JsonProperty("reply_markup")
    Optional<InlineKeyboardMarkupData> replyMarkup();
}
