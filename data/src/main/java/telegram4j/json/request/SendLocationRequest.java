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
@JsonSerialize(as = ImmutableSendLocationRequest.class)
@JsonDeserialize(as = ImmutableSendLocationRequest.class)
public interface SendLocationRequest extends BaseSendRequest {

    static ImmutableSendLocationRequest.Builder builder() {
        return ImmutableSendLocationRequest.builder();
    }

    ChatId chatId();

    float latitude();

    float longitude();

    @JsonProperty("horizontal_accuracy")
    Optional<Float> horizontalAccuracy();

    @JsonProperty("live_period")
    Optional<Integer> livePeriod();

    Optional<Integer> heading();

    @JsonProperty("proximity_alert_radius")
    Optional<Integer> proximityAlertRadius();

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
