package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.core.object.Id;
import telegram4j.core.object.InlineKeyboardMarkup;
import telegram4j.json.request.MessageEditReplyMarkup;

import java.util.Optional;

@Value.Immutable(singleton = true)
interface MessageEditReplyMarkupSpecGenerator extends Spec<MessageEditReplyMarkup> {

    Optional<Id> chatId();

    Optional<Id> messageId();

    Optional<String> inlineMessageId();

    Optional<InlineKeyboardMarkup> replyMarkup();

    @Override
    default MessageEditReplyMarkup asRequest() {
        return MessageEditReplyMarkup.builder()
                .chatId(chatId().map(Id::asLong))
                .messageId(messageId().map(Id::asLong))
                .inlineMessageId(inlineMessageId())
                .replyMarkup(replyMarkup().map(InlineKeyboardMarkup::getData))
                .build();
    }
}
