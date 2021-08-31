package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.core.object.Id;
import telegram4j.core.object.InlineKeyboardMarkup;
import telegram4j.json.InputMediaData;
import telegram4j.json.request.MessageEditMedia;

import java.util.Optional;

@Value.Immutable
interface MessageEditMediaSpecGenerator extends Spec<MessageEditMedia> {

    Optional<Id> chatId();

    Optional<Id> messageId();

    Optional<String> inlineMessageId();

    InputMediaData media();

    Optional<InlineKeyboardMarkup> replyMarkup();

    @Override
    default MessageEditMedia asRequest() {
        return MessageEditMedia.builder()
                .chatId(chatId().map(Id::asLong))
                .messageId(messageId().map(Id::asLong))
                .inlineMessageId(inlineMessageId())
                .media(media())
                .replyMarkup(replyMarkup().map(InlineKeyboardMarkup::getData))
                .build();
    }
}
