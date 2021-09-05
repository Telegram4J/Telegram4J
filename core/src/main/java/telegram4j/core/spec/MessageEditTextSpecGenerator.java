package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.core.object.MessageEntity;
import telegram4j.core.object.replymarkup.InlineKeyboardMarkup;
import telegram4j.json.ParseMode;
import telegram4j.json.api.ChatId;
import telegram4j.json.api.Id;
import telegram4j.json.request.MessageEditTextRequest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Value.Immutable
interface MessageEditTextSpecGenerator extends Spec<MessageEditTextRequest> {

    Optional<ChatId> chatId();

    Optional<Id> messageId();

    Optional<String> inlineMessageId();

    String text();

    Optional<ParseMode> parseMode();

    Optional<List<MessageEntity>> entities();

    Optional<Boolean> disableWebPagePreview();

    Optional<InlineKeyboardMarkup> replyMarkup();

    @Override
    default MessageEditTextRequest asRequest() {
        return MessageEditTextRequest.builder()
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
