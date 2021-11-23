package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import telegram4j.core.event.domain.message.MessageCreateEvent;
import telegram4j.tl.Chat;
import telegram4j.tl.UpdateNewMessage;
import telegram4j.tl.User;

class MessageUpdateHandlers {

    static Flux<MessageCreateEvent> handleUpdateNewMessage(UpdateContext<UpdateNewMessage> context) {
        Chat chat = context.getChats().isEmpty() ? null : context.getChats().get(0);
        User user = context.getUsers().isEmpty() ? null : context.getUsers().get(0);

        return Flux.just(new MessageCreateEvent(context.getClient(), context.getUpdate().message(), chat, user));
    }
}
