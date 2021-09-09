package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.core.object.replymarkup.ReplyMarkup;
import telegram4j.json.api.ChatId;
import telegram4j.json.api.Id;
import telegram4j.json.request.SendLocationRequest;

import java.util.Optional;

@Value.Immutable
interface SendLocationSpecGenerator extends Spec<SendLocationRequest> {

    ChatId chatId();

    float latitude();

    float longitude();

    Optional<Float> horizontalAccuracy();

    Optional<Integer> livePeriod();

    Optional<Integer> heading();

    Optional<Integer> proximityAlertRadius();

    Optional<Boolean> disableNotification();

    Optional<Id> replyToMessageId();

    Optional<Boolean> allowSendingWithoutReply();

    Optional<ReplyMarkup> replyMarkup();

    @Override
    default SendLocationRequest asRequest() {
        return SendLocationRequest.builder()
                .chatId(chatId())
                .latitude(latitude())
                .longitude(longitude())
                .horizontalAccuracy(horizontalAccuracy())
                .livePeriod(livePeriod())
                .heading(heading())
                .proximityAlertRadius(proximityAlertRadius())
                .disableNotification(disableNotification())
                .replyToMessageId(replyToMessageId())
                .allowSendingWithoutReply(allowSendingWithoutReply())
                .replyMarkup(replyMarkup().map(ReplyMarkup::getData))
                .build();
    }
}
