package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.json.api.ChatId;
import telegram4j.json.api.Id;
import telegram4j.core.object.InlineKeyboardMarkup;
import telegram4j.json.InputMediaData;
import telegram4j.json.request.MessageEditMedia;

import java.util.Optional;

@Value.Immutable
interface MessageEditMediaSpecGenerator extends Spec<MessageEditMedia> {

    Optional<ChatId> chatId();

    Optional<Id> messageId();

    Optional<String> inlineMessageId();

    InputMediaData media();

    Optional<InlineKeyboardMarkup> replyMarkup();

    @Override
    default MessageEditMedia asRequest() {
        return MessageEditMedia.builder()
                .chatId(chatId())
                .messageId(messageId())
                .inlineMessageId(inlineMessageId())
                .media(media())
                .replyMarkup(replyMarkup().map(InlineKeyboardMarkup::getData))
                .build();
    }
}
