package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.core.object.MessageEntity;
import telegram4j.core.object.replymarkup.ReplyMarkup;
import telegram4j.json.ParseMode;
import telegram4j.json.api.ChatId;
import telegram4j.json.api.Id;
import telegram4j.json.request.MessageCreateRequest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Value.Immutable
interface MessageCreateSpecGenerator extends Spec<MessageCreateRequest> {

    ChatId chatId();

    String text();

    Optional<ParseMode> parseMode();

    Optional<List<MessageEntity>> entities();

    Optional<Boolean> disableWebPagePreview();

    Optional<Boolean> disableNotification();

    Optional<Id> replyToMessageId();

    Optional<Boolean> allowSendingWithoutReply();

    Optional<ReplyMarkup> replyMarkup();

    @Override
    default MessageCreateRequest asRequest() {
        return MessageCreateRequest.builder()
                .chatId(chatId())
                .text(text())
                .parseMode(parseMode())
                .entities(entities().map(list -> list.stream()
                        .map(MessageEntity::getData)
                        .collect(Collectors.toList())))
                .disableWebPreview(disableWebPagePreview())
                .disableNotification(disableNotification())
                .replyToMessageId(replyToMessageId())
                .allowSendingWithoutReply(allowSendingWithoutReply())
                .replyMarkup(replyMarkup().map(ReplyMarkup::getData))
                .build();
    }
}
