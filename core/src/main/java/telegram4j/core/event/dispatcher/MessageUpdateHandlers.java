package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.message.EditMessageEvent;
import telegram4j.core.event.domain.message.SendMessageEvent;
import telegram4j.tl.*;

class MessageUpdateHandlers {

    // State handler
    // =====================

    static Mono<Void> handleStateUpdateNewMessage(UpdateContext<UpdateNewMessageFields> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onNewMessage(context.getUpdate().message(), context.getChats(), context.getUsers());
    }

    static Mono<Message> handleStateUpdateEditMessage(UpdateContext<UpdateEditMessageFields> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onEditMessage(context.getUpdate().message(), context.getChats(), context.getUsers());
    }

    // Update handler
    // =====================

    static Flux<SendMessageEvent> handleUpdateNewMessage(StatefulUpdateContext<UpdateNewMessageFields, Void> context) {
        Chat chat = context.getChats().isEmpty() ? null : context.getChats().get(0);
        User user = context.getUsers().isEmpty() ? null : context.getUsers().get(0);

        return Flux.just(new SendMessageEvent(context.getClient(), context.getUpdate().message(), chat, user));
    }

    static Flux<EditMessageEvent> handleUpdateEditMessage(StatefulUpdateContext<UpdateEditMessageFields, Message> context) {
        Chat chat = context.getChats().isEmpty() ? null : context.getChats().get(0);
        User user = context.getUsers().isEmpty() ? null : context.getUsers().get(0);

        return Flux.just(new EditMessageEvent(context.getClient(), context.getUpdate().message(),
                context.getOld(), chat, user));
    }
}
