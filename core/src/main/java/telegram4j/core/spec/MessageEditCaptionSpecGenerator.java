package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.core.object.MessageEntity;
import telegram4j.core.object.replymarkup.InlineKeyboardMarkup;
import telegram4j.json.ParseMode;
import telegram4j.json.api.ChatId;
import telegram4j.json.api.Id;
import telegram4j.json.request.MessageEditCaptionRequest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Value.Immutable(singleton = true)
interface MessageEditCaptionSpecGenerator extends Spec<MessageEditCaptionRequest> {

    Optional<ChatId> chatId();

    Optional<Id> messageId();

    Optional<String> inlineMessageId();

    Optional<String> caption();

    Optional<ParseMode> parseMode();

    Optional<List<MessageEntity>> captionEntities();

    Optional<InlineKeyboardMarkup> replyMarkup();

    @Override
    default MessageEditCaptionRequest asRequest() {
        return MessageEditCaptionRequest.builder()
                .chatId(chatId())
                .messageId(messageId())
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
