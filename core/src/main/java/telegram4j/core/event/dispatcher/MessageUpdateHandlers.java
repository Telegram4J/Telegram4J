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
import telegram4j.tl.BaseMessageFields;
import telegram4j.tl.BaseUser;
import telegram4j.tl.UpdateEditMessageFields;
import telegram4j.tl.UpdateNewMessageFields;
import telegram4j.tl.api.TlObject;

import java.util.Optional;

import static telegram4j.mtproto.util.TlEntityUtil.getRawPeerId;

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
        BaseMessageFields message = (BaseMessageFields) context.getUpdate().message();

        var chatData = context.getChats().get(getRawPeerId(message.peerId()));
        var userData = Optional.ofNullable(message.fromId())
                .map(p -> context.getUsers().get(getRawPeerId(p)))
                .filter(u -> u.identifier() == BaseUser.ID)
                .map(u -> (BaseUser) u);
        Chat chat = Optional.<TlObject>ofNullable(chatData)
                .or(() -> userData)
                .map(c -> EntityFactory.createChat(client, c))
                .orElse(null);
        User user = userData
                .map(d -> new User(client, d))
                .orElse(null);
        Id resolvedChatId = Optional.ofNullable(chat)
                .map(Chat::getId)
                .orElseGet(() -> Id.of(message.peerId()));

        Message newMessage = EntityFactory.createMessage(client, message, resolvedChatId);

        return Flux.just(new SendMessageEvent(client, newMessage, chat, user));
    }

    static Flux<EditMessageEvent> handleUpdateEditMessage(StatefulUpdateContext<UpdateEditMessageFields, telegram4j.tl.Message> context) {
        MTProtoTelegramClient client = context.getClient();
        BaseMessageFields message = (BaseMessageFields) context.getUpdate().message();

        var chatData = context.getChats().get(getRawPeerId(message.peerId()));
        var userData = Optional.ofNullable(message.fromId())
                .map(p -> context.getUsers().get(getRawPeerId(p)))
                .filter(u -> u.identifier() == BaseUser.ID)
                .map(u -> (BaseUser) u);
        Chat chat = Optional.<TlObject>ofNullable(chatData)
                .or(() -> userData)
                .map(c -> EntityFactory.createChat(client, c))
                .orElse(null);
        Id resolvedId = Optional.ofNullable(chat)
                .map(Chat::getId)
                .orElseGet(() -> Id.of(message.peerId()));
        Message oldMessage = Optional.ofNullable(context.getOld())
                .map(d -> EntityFactory.createMessage(client, d, resolvedId))
                .orElse(null);
        Message newMessage = EntityFactory.createMessage(client, message, resolvedId);

        return Flux.just(new EditMessageEvent(client, newMessage, oldMessage, chat));
    }
}
