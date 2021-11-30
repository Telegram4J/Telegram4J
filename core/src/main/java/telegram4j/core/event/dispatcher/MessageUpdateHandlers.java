package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.message.EditMessageEvent;
import telegram4j.core.event.domain.message.SendMessageEvent;
import telegram4j.tl.*;

class MessageUpdateHandlers {

    // State handler
    // =====================

    // why?!
    static Mono<Void> handleStateUpdateNewChannelMessage(UpdateContext<UpdateNewChannelMessage> context) {
        return context.getClient().getSession()
                .getMtProtoResources().getStoreLayout()
                .onNewMessage(context.getUpdate().message(), context.getChats(), context.getUsers());
    }

    static Mono<Void> handleStateUpdateNewMessage(UpdateContext<UpdateNewMessage> context) {
        return context.getClient().getSession()
                .getMtProtoResources().getStoreLayout()
                .onNewMessage(context.getUpdate().message(), context.getChats(), context.getUsers());
    }

    static Mono<Message> handleStateUpdateEditMessage(UpdateContext<UpdateEditMessage> context) {
        return context.getClient().getSession()
                .getMtProtoResources().getStoreLayout()
                .onEditMessage(context.getUpdate().message(), context.getChats(), context.getUsers());
    }

    // Update handler
    // =====================

    static Flux<SendMessageEvent> handleUpdateNewMessage(StatefulUpdateContext<UpdateNewMessage, Void> context) {
        Chat chat = context.getChats().isEmpty() ? null : context.getChats().get(0);
        User user = context.getUsers().isEmpty() ? null : context.getUsers().get(0);

        return Flux.just(new SendMessageEvent(context.getClient(), context.getUpdate().message(), chat, user));
    }

    static Flux<SendMessageEvent> handleUpdateNewChannelMessage(StatefulUpdateContext<UpdateNewChannelMessage, Void> context) {
        Chat chat = context.getChats().isEmpty() ? null : context.getChats().get(0);
        User user = context.getUsers().isEmpty() ? null : context.getUsers().get(0);

        return Flux.just(new SendMessageEvent(context.getClient(), context.getUpdate().message(), chat, user));
    }

    static Flux<EditMessageEvent> handleUpdateEditMessage(StatefulUpdateContext<UpdateEditMessage, Message> context) {
        Chat chat = context.getChats().isEmpty() ? null : context.getChats().get(0);
        User user = context.getUsers().isEmpty() ? null : context.getUsers().get(0);

        return Flux.just(new EditMessageEvent(context.getClient(), context.getUpdate().message(),
                context.getOld(), chat, user));
    }
}
