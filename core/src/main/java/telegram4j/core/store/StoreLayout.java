package telegram4j.core.store;

import reactor.core.publisher.Mono;
import telegram4j.json.MessageData;

public interface StoreLayout {

    Mono<Void> onMessageCreate(MessageData dispatch);

    Mono<MessageData> onMessageUpdate(MessageData dispatch);

    Mono<MessageData> getMessageById(long chatId, long messageId);
}
