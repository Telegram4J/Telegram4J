package telegram4j.core.dispatch;

import reactor.core.publisher.Mono;
import telegram4j.core.TelegramClient;
import telegram4j.core.event.MessageCreateEvent;
import telegram4j.core.event.MessageUpdateEvent;
import telegram4j.core.object.Message;
import telegram4j.json.MessageData;

class ChatDispatchHandlers {

    static class MessageCreate implements DispatchHandler<MessageCreateEvent, Void> {

        @Override
        public boolean canHandle(UpdateContext<Void> update) {
            return update.getUpdateData().message().isPresent();
        }

        @Override
        public Mono<MessageCreateEvent> handle(UpdateContext<Void> update) {
            TelegramClient client = update.getClient();
            Message message = update.getUpdateData().message()
                    .map(messageData -> new Message(client, messageData))
                    .orElseThrow(IllegalStateException::new);

            return Mono.just(new MessageCreateEvent(client, message));
        }
    }

    static class MessageUpdate implements DispatchHandler<MessageUpdateEvent, MessageData> {

        @Override
        public boolean canHandle(UpdateContext<MessageData> update) {
            return update.getUpdateData().editedMessage().isPresent();
        }

        @Override
        public Mono<MessageUpdateEvent> handle(UpdateContext<MessageData> update) {
            TelegramClient client = update.getClient();
            Message currentMessage = update.getUpdateData().editedMessage()
                    .map(messageData -> new Message(client, messageData))
                    .orElseThrow(IllegalStateException::new);

            Message oldMessage = update.getOldData() != null ? new Message(client, update.getOldData()) : null;

            return Mono.just(new MessageUpdateEvent(client, currentMessage, oldMessage));
        }
    }
}
