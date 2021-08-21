package telegram4j.core.dispatch;

import reactor.core.publisher.Mono;
import telegram4j.core.TelegramClient;
import telegram4j.core.event.MessageCreateEvent;
import telegram4j.core.object.Message;

class ChatDispatchHandlers {

    static class MessageCreate implements DispatchHandler<MessageCreateEvent> {

        @Override
        public boolean canHandle(UpdateContext update) {
            return update.getUpdateData().message().isPresent();
        }

        @Override
        public Mono<MessageCreateEvent> handle(UpdateContext update) {
            TelegramClient client = update.getClient();
            Message message = update.getUpdateData().message()
                    .map(messageData -> new Message(client, messageData))
                    .orElseThrow(IllegalStateException::new);

            return Mono.just(new MessageCreateEvent(client, message));
        }
    }
}
