package telegram4j.json.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import telegram4j.json.MessageEntityData;
import telegram4j.json.ParseMode;
import telegram4j.json.ReplyMarkupData;
import telegram4j.json.api.ChatId;
import telegram4j.json.api.Id;

import java.util.List;
import java.util.Optional;

@Value.Immutable(singleton = true)
@JsonSerialize(as = ImmutableMessageEditCaptionRequest.class)
@JsonDeserialize(as = ImmutableMessageEditCaptionRequest.class)
public interface MessageEditCaptionRequest {

    static ImmutableMessageEditCaptionRequest.Builder builder() {
        return ImmutableMessageEditCaptionRequest.builder();
    }

    @JsonProperty("chat_id")
    Optional<ChatId> chatId();

    @JsonProperty("message_id")
    Optional<Id> messageId();

    @JsonProperty("inline_message_id")
    Optional<String> inlineMessageId();

    Optional<String> caption();

    @JsonProperty("parse_mode")
    Optional<ParseMode> parseMode();

    @JsonProperty("caption_entities")
    Optional<List<MessageEntityData>> captionEntities();

    @JsonProperty("reply_markup")
    Optional<ReplyMarkupData> replyMarkup();
}
