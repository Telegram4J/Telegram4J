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
@JsonSerialize(as = ImmutableSendDocumentRequest.class)
@JsonDeserialize(as = ImmutableSendDocumentRequest.class)
public interface SendDocumentRequest extends BaseSendRequest, CaptionFields {

    static ImmutableSendDocumentRequest.Builder builder() {
        return ImmutableSendDocumentRequest.builder();
    }

    @JsonProperty("chat_id")
    ChatId chatId();

    @Override
    Optional<String> caption();

    @Override
    @JsonProperty("parse_mode")
    Optional<ParseMode> parseMode();

    @Override
    @JsonProperty("caption_entities")
    Optional<List<MessageEntityData>> captionEntities();

    @JsonProperty("disable_content_type_detection")
    Optional<Boolean> disableContentTypeDetection();

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
