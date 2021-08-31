package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.core.object.Id;
import telegram4j.core.object.InlineKeyboardMarkup;
import telegram4j.core.object.MessageEntity;
import telegram4j.json.ParseMode;
import telegram4j.json.request.MessageEditCaption;
import telegram4j.json.request.MessageEditText;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Value.Immutable(singleton = true)
interface MessageEditCaptionSpecGenerator extends Spec<MessageEditCaption> {

    Optional<Id> chatId();

    Optional<Id> messageId();

    Optional<String> inlineMessageId();

    Optional<String> caption();

    Optional<ParseMode> parseMode();

    Optional<List<MessageEntity>> captionEntities();

    Optional<InlineKeyboardMarkup> replyMarkup();

    @Override
    default MessageEditCaption asRequest() {
        return MessageEditCaption.builder()
                .chatId(chatId().map(Id::asLong))
                .messageId(messageId().map(Id::asLong))
                .inlineMessageId(inlineMessageId())
                .caption(caption())
                .parseMode(parseMode())
                .captionEntities(captionEntities().map(list -> list.stream()
                        .map(MessageEntity::getData)
                        .collect(Collectors.toList())))
                .replyMarkup(replyMarkup().map(InlineKeyboardMarkup::getData))
                .build();
    }
}
