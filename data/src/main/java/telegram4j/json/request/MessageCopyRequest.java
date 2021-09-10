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

@Value.Immutable
@JsonSerialize(as = ImmutableMessageCopyRequest.class)
@JsonDeserialize(as = ImmutableMessageCopyRequest.class)
public interface MessageCopyRequest extends BaseSendRequest, CaptionFields {

    static ImmutableMessageCopyRequest.Builder builder() {
        return ImmutableMessageCopyRequest.builder();
    }

    @JsonProperty("chat_id")
    ChatId chatId();

    @JsonProperty("from_chat_id")
    ChatId fromChatId();

    @JsonProperty("message_id")
    Id messageId();

    @Override
    Optional<String> caption();

    @Override
    @JsonProperty("parse_mode")
    Optional<ParseMode> parseMode();

    @Override
    @JsonProperty("caption_entities")
    Optional<List<MessageEntityData>> captionEntities();

    @Override
    @JsonProperty("disable_notification")
    Optional<Boolean> disableNotification();

    @Override
    @JsonProperty("reply_to_message_id")
    Optional<Id> replyToMessageId();

    @Override
    @JsonProperty("allow_sending_without_reply")
    Optional<Boolean> allowSendingWithoutReply();

    @Override
    @JsonProperty("reply_markup")
    Optional<ReplyMarkupData> replyMarkup();
}