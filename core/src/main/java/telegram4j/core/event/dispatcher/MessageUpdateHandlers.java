package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.domain.message.EditMessageEvent;
import telegram4j.core.event.domain.message.SendMessageEvent;
import telegram4j.core.object.Id;
import telegram4j.core.object.Message;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.util.EntityFactory;
import telegram4j.tl.*;
import telegram4j.tl.api.TlObject;

import java.util.Optional;

class MessageUpdateHandlers {

    // State handler
    // =====================

    static Mono<Void> handleStateUpdateNewMessage(UpdateContext<UpdateNewMessageFields> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onNewMessage(context.getUpdate().message(), context.getChats(), context.getUsers());
    }

    static Mono<telegram4j.tl.Message> handleStateUpdateEditMessage(UpdateContext<UpdateEditMessageFields> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onEditMessage(context.getUpdate().message(), context.getChats(), context.getUsers());
    }

    // Update handler
    // =====================

    static Flux<SendMessageEvent> handleUpdateNewMessage(StatefulUpdateContext<UpdateNewMessageFields, Void> context) {
        MTProtoTelegramClient client = context.getClient();
        UpdateNewMessageFields upd = context.getUpdate();

        var chatData = context.getChats().isEmpty() ? null : context.getChats().get(0);
        var userData = context.getUsers().isEmpty() ? null : context.getUsers().get(0);

        Chat chat = Optional.<TlObject>ofNullable(chatData)
                .or(() -> Optional.ofNullable(userData))
                .map(c -> EntityFactory.createChat(client, c))
                .orElse(null);

        User user = Optional.ofNullable(userData)
                .filter(u -> u.identifier() == BaseUser.ID)
                .map(u -> (BaseUser) u)
                .map(d -> new User(client, d))
                .orElse(null);

        Peer peerId = ((BaseMessageFields) upd.message()).peerId();

        Id resolvedChatId = Optional.ofNullable(chat)
                .map(Chat::getId)
                .orElseGet(() -> Id.of(peerId));

        Message newMessage = EntityFactory.createMessage(client, upd.message(), resolvedChatId);

        return Flux.just(new SendMessageEvent(client, newMessage, chat, user));
    }

    static Flux<EditMessageEvent> handleUpdateEditMessage(StatefulUpdateContext<UpdateEditMessageFields, telegram4j.tl.Message> context) {
        MTProtoTelegramClient client = context.getClient();
        UpdateEditMessageFields upd = context.getUpdate();

        var chatData = context.getChats().isEmpty() ? null : context.getChats().get(0);
        var userData = context.getUsers().isEmpty() ? null : context.getUsers().get(0);

        Chat chat = Optional.<TlObject>ofNullable(chatData)
                .or(() -> Optional.ofNullable(userData))
                .map(c -> EntityFactory.createChat(client, c))
                .orElse(null);

        Peer peerId = ((BaseMessageFields) upd.message()).peerId();

        Id resolvedId = Optional.ofNullable(chat)
                .map(Chat::getId)
                .orElseGet(() -> Id.of(peerId));

        Message oldMessage = Optional.ofNullable(context.getOld())
                .map(d -> EntityFactory.createMessage(client, d, resolvedId))
                .orElse(null);

        Message newMessage = EntityFactory.createMessage(client, upd.message(), resolvedId);

        return Flux.just(new EditMessageEvent(client, newMessage, oldMessage, chat));
    }
}
