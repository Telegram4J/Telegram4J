package telegram4j.core.store;

import reactor.core.publisher.Mono;
import telegram4j.json.MessageData;
import telegram4j.json.UserData;

public interface StoreLayout {

    Mono<Void> onMessageCreate(MessageData dispatch);

    Mono<MessageData> onMessageUpdate(MessageData dispatch);

    Mono<MessageData> onMessageDelete(MessageData dispatch);

    Mono<UserData> onUserUpdate(UserData dispatch);

    Mono<MessageData> getMessageById(long chatId, long messageId);

    Mono<UserData> getUserById(long userId);
}
