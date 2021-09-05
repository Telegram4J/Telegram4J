package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.core.object.replymarkup.InlineKeyboardMarkup;
import telegram4j.json.api.ChatId;
import telegram4j.json.api.Id;
import telegram4j.json.request.StopPoll;

import java.util.Optional;

@Value.Immutable
interface StopPollSpecGenerator extends Spec<StopPoll> {

    ChatId chatId();

    Id messageId();

    Optional<InlineKeyboardMarkup> replyMarkup();

    @Override
    default StopPoll asRequest() {
        return StopPoll.builder()
                .chatId(chatId())
                .messageId(messageId())
                .replyMarkup(replyMarkup().map(InlineKeyboardMarkup::getData))
                .build();
    }
}
