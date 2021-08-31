package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.core.object.Id;
import telegram4j.core.object.InlineKeyboardMarkup;
import telegram4j.json.request.StopPoll;

import java.util.Optional;

@Value.Immutable
interface StopPollSpecGenerator extends Spec<StopPoll> {

    Id chatId();

    Id messageId();

    Optional<InlineKeyboardMarkup> replyMarkup();

    @Override
    default StopPoll asRequest() {
        return StopPoll.builder()
                .chatId(chatId().asLong())
                .messageId(messageId().asLong())
                .replyMarkup(replyMarkup().map(InlineKeyboardMarkup::getData))
                .build();
    }
}
