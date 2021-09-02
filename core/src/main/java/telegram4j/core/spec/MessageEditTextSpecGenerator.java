package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.json.api.ChatId;
import telegram4j.json.api.Id;
import telegram4j.core.object.InlineKeyboardMarkup;
import telegram4j.core.object.MessageEntity;
import telegram4j.json.ParseMode;
import telegram4j.json.request.MessageEditText;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Value.Immutable
interface MessageEditTextSpecGenerator extends Spec<MessageEditText> {

    Optional<ChatId> chatId();

    Optional<Id> messageId();

    Optional<String> inlineMessageId();

    String text();

    Optional<ParseMode> parseMode();

    Optional<List<MessageEntity>> entities();

    Optional<Boolean> disableWebPagePreview();

    Optional<InlineKeyboardMarkup> replyMarkup();

    @Override
    default MessageEditText asRequest() {
        return MessageEditText.builder()
                .chatId(chatId())
                .messageId(messageId())
                .inlineMessageId(inlineMessageId())
                .text(text())
                .parseMode(parseMode())
                .entities(entities().map(list -> list.stream()
                        .map(MessageEntity::getData)
                        .collect(Collectors.toList())))
                .disableWebPagePreview(disableWebPagePreview())
                .replyMarkup(replyMarkup().map(InlineKeyboardMarkup::getData))
                .build();
    }
}
