package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.core.object.replymarkup.InlineKeyboardMarkup;
import telegram4j.json.api.ChatId;
import telegram4j.json.api.Id;
import telegram4j.json.request.MessageEditReplyMarkupRequest;

import java.util.Optional;

@Value.Immutable(singleton = true)
interface MessageEditReplyMarkupSpecGenerator extends Spec<MessageEditReplyMarkupRequest> {

    Optional<ChatId> chatId();

    Optional<Id> messageId();

    Optional<String> inlineMessageId();

    Optional<InlineKeyboardMarkup> replyMarkup();

    @Override
    default MessageEditReplyMarkupRequest asRequest() {
        return MessageEditReplyMarkupRequest.builder()
                .chatId(chatId())
                .messageId(messageId())
                .inlineMessageId(inlineMessageId())
                .replyMarkup(replyMarkup().map(InlineKeyboardMarkup::getData))
                .build();
    }
}
