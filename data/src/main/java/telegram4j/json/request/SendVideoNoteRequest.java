package telegram4j.json.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import telegram4j.json.ReplyMarkupData;
import telegram4j.json.api.ChatId;
import telegram4j.json.api.Id;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableSendVideoNoteRequest.class)
@JsonDeserialize(as = ImmutableSendVideoNoteRequest.class)
public interface SendVideoNoteRequest extends BaseSendRequest {

    static ImmutableSendVideoNoteRequest.Builder builder() {
        return ImmutableSendVideoNoteRequest.builder();
    }

    ChatId chatId();

    Optional<Integer> duration();

    Optional<Integer> length();

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
