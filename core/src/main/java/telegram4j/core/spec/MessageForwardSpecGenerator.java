package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.json.api.ChatId;
import telegram4j.json.api.Id;
import telegram4j.json.request.MessageForwardRequest;

import java.util.Optional;

@Value.Immutable
interface MessageForwardSpecGenerator extends Spec<MessageForwardRequest> {

    ChatId chatId();

    ChatId fromChatId();

    Optional<Boolean> disableNotification();

    Id messageId();

    @Override
    default MessageForwardRequest asRequest() {
        return MessageForwardRequest.builder()
                .chatId(chatId())
                .fromChatId(fromChatId())
                .disableNotification(disableNotification())
                .messageId(messageId())
                .build();
    }
}
