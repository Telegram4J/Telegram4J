package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.core.object.MessageEntity;
import telegram4j.core.object.replymarkup.ReplyMarkup;
import telegram4j.json.ParseMode;
import telegram4j.json.api.ChatId;
import telegram4j.json.api.Id;
import telegram4j.json.request.MessageCopyRequest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Value.Immutable
interface MessageCopySpecGenerator extends Spec<MessageCopyRequest> {

    ChatId chatId();

    ChatId fromChatId();

    Id messageId();

    String caption();

    Optional<ParseMode> parseMode();

    Optional<List<MessageEntity>> captionEntities();

    Optional<Boolean> disableNotification();

    Optional<Id> replyToMessageId();

    Optional<Boolean> allowSendingWithoutReply();

    Optional<ReplyMarkup> replyMarkup();

    @Override
    default MessageCopyRequest asRequest() {
        return MessageCopyRequest.builder()
                .chatId(chatId())
                .fromChatId(fromChatId())
                .messageId(messageId())
                .parseMode(parseMode())
                .captionEntities(captionEntities().map(list -> list.stream()
                        .map(MessageEntity::getData)
                        .collect(Collectors.toList())))
                .disableNotification(disableNotification())
                .replyToMessageId(replyToMessageId())
                .allowSendingWithoutReply(allowSendingWithoutReply())
                .replyMarkup(replyMarkup().map(ReplyMarkup::getData))
                .build();
    }
}
