package telegram4j.json.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import telegram4j.json.ReplyMarkupData;
import telegram4j.json.api.Id;

import java.util.Optional;

public interface BaseSendRequest {

    @JsonProperty("disable_notification")
    Optional<Boolean> disableNotification();

    @JsonProperty("reply_to_message_id")
    Optional<Id> replyToMessageId();

    @JsonProperty("allow_sending_without_reply")
    Optional<Boolean> allowSendingWithoutReply();

    @JsonProperty("reply_markup")
    Optional<ReplyMarkupData> replyMarkup();
}
