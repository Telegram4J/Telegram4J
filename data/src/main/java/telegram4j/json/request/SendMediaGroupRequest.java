package telegram4j.json.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import telegram4j.json.InputMediaData;
import telegram4j.json.api.ChatId;
import telegram4j.json.api.Id;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableSendMediaGroupRequest.class)
@JsonDeserialize(as = ImmutableSendMediaGroupRequest.class)
public interface SendMediaGroupRequest {

    static ImmutableSendMediaGroupRequest.Builder builder() {
        return ImmutableSendMediaGroupRequest.builder();
    }

    @JsonProperty("chat_id")
    ChatId chatId();

    List<InputMediaData> media();

    @JsonProperty("disable_notification")
    Optional<Boolean> disableNotification();

    @JsonProperty("reply_to_message_id")
    Optional<Id> replyToMessageId();

    @JsonProperty("allow_sending_without_reply")
    Optional<Boolean> allowSendingWithoutReply();
}
