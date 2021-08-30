package telegram4j.json.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import telegram4j.json.InlineKeyboardMarkupData;
import telegram4j.json.MessageEntityData;
import telegram4j.json.ParseMode;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableMessageEditText.class)
@JsonDeserialize(as = ImmutableMessageEditText.class)
public interface MessageEditText {

    static ImmutableMessageEditText.Builder builder() {
        return ImmutableMessageEditText.builder();
    }

    @JsonProperty("chat_id")
    Optional<Long> chatId();

    @JsonProperty("message_id")
    Optional<Long> messageId();

    @JsonProperty("inline_message_id")
    Optional<String> inlineMessageId();

    String text();

    @JsonProperty("parse_mode")
    Optional<ParseMode> parseMode();

    Optional<List<MessageEntityData>> entities();

    @JsonProperty("disable_web_page_preview")
    Optional<Boolean> disableWebPagePreview();

    @JsonProperty("reply_markup")
    Optional<InlineKeyboardMarkupData> replyMarkup();
}
