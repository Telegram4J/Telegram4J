package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.domain.message.DeleteMessagesEvent;
import telegram4j.core.event.domain.message.EditMessageEvent;
import telegram4j.core.event.domain.message.SendMessageEvent;
import telegram4j.core.event.domain.message.UpdatePinnedMessagesEvent;
import telegram4j.core.object.Id;
import telegram4j.core.object.Message;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.util.EntityFactory;
import telegram4j.mtproto.store.ResolvedDeletedMessages;
import telegram4j.tl.*;
import telegram4j.tl.api.TlObject;

import java.util.Optional;
import java.util.stream.Collectors;

import static telegram4j.mtproto.util.TlEntityUtil.getRawPeerId;

class MessageUpdateHandlers {

    // State handler
    // =====================

    static Mono<Void> handleStateUpdateNewMessage(UpdateContext<UpdateNewMessageFields> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onNewMessage(context.getUpdate().message());
    }

    static Mono<telegram4j.tl.Message> handleStateUpdateEditMessage(UpdateContext<UpdateEditMessageFields> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onEditMessage(context.getUpdate().message());
    }

    static Mono<ResolvedDeletedMessages> handleStateUpdateDeleteMessages(UpdateContext<UpdateDeleteMessagesFields> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onDeleteMessages(context.getUpdate());
    }

    static Mono<Void> handleStateUpdatePinnedMessages(UpdateContext<UpdatePinnedMessagesFields> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onUpdatePinnedMessages(context.getUpdate());
    }

    // Update handler
    // =====================

    static Flux<SendMessageEvent> handleUpdateNewMessage(StatefulUpdateContext<UpdateNewMessageFields, Void> context) {
        MTProtoTelegramClient client = context.getClient();
        BaseMessageFields message = (BaseMessageFields) context.getUpdate().message();

        long chatId = getRawPeerId(message.peerId());
        BaseUser selfUser = context.getUsers().values().stream()
                .filter(BaseUser::self)
                .findFirst()
                .orElse(null);
        Chat chat = Optional.<TlObject>ofNullable(context.getChats().get(chatId))
                .or(() -> Optional.ofNullable(context.getUsers().get(chatId)))
                .map(c -> EntityFactory.createChat(client, c, selfUser))
                .orElse(null);
        var author = Optional.ofNullable(message.fromId())
                .flatMap(p -> {
                    long rawId = getRawPeerId(p);
                    switch (p.identifier()) {
                        case PeerUser.ID: return Optional.ofNullable(context.getUsers().get(rawId))
                                .map(u -> EntityFactory.createUser(client, u));
                        case PeerChat.ID:
                        case PeerChannel.ID: return Optional.ofNullable(context.getChats().get(rawId))
                                .map(u -> EntityFactory.createChat(client, u, null));
                        default: throw new IllegalArgumentException("Unknown peer type: " + p);
                    }
                })
                .orElse(null);
        Id resolvedChatId = Optional.ofNullable(chat)
                .map(Chat::getId)
                .orElseGet(() -> Id.of(message.peerId()));

        Message newMessage = EntityFactory.createMessage(client, message, resolvedChatId);

        return Flux.just(new SendMessageEvent(client, newMessage, chat, author));
    }

    static Flux<EditMessageEvent> handleUpdateEditMessage(StatefulUpdateContext<UpdateEditMessageFields, telegram4j.tl.Message> context) {
        MTProtoTelegramClient client = context.getClient();
        BaseMessageFields message = (BaseMessageFields) context.getUpdate().message();

        long chatId = getRawPeerId(message.peerId());
        var selfUser = context.getUsers().values().stream()
                .filter(BaseUser::self)
                .findFirst()
                .orElse(null);
        Chat chat = Optional.<TlObject>ofNullable(context.getChats().get(chatId))
                .or(() -> Optional.ofNullable(context.getUsers().get(chatId)))
                .map(c -> EntityFactory.createChat(client, c, selfUser))
                .orElse(null);
        var author = Optional.ofNullable(message.fromId())
                .flatMap(p -> {
                    long rawId = getRawPeerId(p);
                    switch (p.identifier()) {
                        case PeerUser.ID: return Optional.ofNullable(context.getUsers().get(rawId))
                                .map(u -> EntityFactory.createUser(client, u));
                        case PeerChat.ID:
                        case PeerChannel.ID: return Optional.ofNullable(context.getChats().get(rawId))
                                .map(u -> EntityFactory.createChat(client, u, null));
                        default: throw new IllegalArgumentException("Unknown peer type: " + p);
                    }
                })
                .orElse(null);
        Id resolvedId = Optional.ofNullable(chat)
                .map(Chat::getId)
                .orElseGet(() -> Id.of(message.peerId()));
        Message oldMessage = Optional.ofNullable(context.getOld())
                .map(d -> EntityFactory.createMessage(client, d, resolvedId))
                .orElse(null);
        Message newMessage = EntityFactory.createMessage(client, message, resolvedId);

        return Flux.just(new EditMessageEvent(client, newMessage, oldMessage, chat, author));
    }

    static Flux<DeleteMessagesEvent> handleUpdateDeleteMessages(StatefulUpdateContext<UpdateDeleteMessagesFields, ResolvedDeletedMessages> context) {
        return Mono.justOrEmpty(context.getOld())
                .flatMapMany(re -> {
                    Id chatId;
                    switch (re.getPeer().identifier()) {
                        case InputPeerChannel.ID:
                            InputPeerChannel peerChannel = (InputPeerChannel) re.getPeer();
                            chatId = Id.ofChannel(peerChannel.channelId(), peerChannel.accessHash());
                            break;
                        case InputPeerChat.ID:
                            InputPeerChat peerChat = (InputPeerChat) re.getPeer();
                            chatId = Id.ofChat(peerChat.chatId());
                            break;
                        case InputPeerUser.ID:
                            InputPeerUser peerUser = (InputPeerUser) re.getPeer();
                            chatId = Id.ofUser(peerUser.userId(), peerUser.accessHash());
                            break;
                        default:
                            throw new IllegalStateException();
                    }

                    var oldMessages = re.getMessages().stream()
                            .map(d -> EntityFactory.createMessage(context.getClient(), d, chatId))
                            .collect(Collectors.toList());

                    boolean scheduled = context.getUpdate().identifier() == UpdateDeleteScheduledMessages.ID;

                    return Flux.just(new DeleteMessagesEvent(context.getClient(), chatId,
                            scheduled, oldMessages, context.getUpdate().messages()));
                });
    }

    static Flux<UpdatePinnedMessagesEvent> handleUpdatePinnedMessages(StatefulUpdateContext<UpdatePinnedMessagesFields, Void> context) {
        UpdatePinnedMessagesFields upd = context.getUpdate();

        Id chatId = upd.identifier() == UpdatePinnedMessages.ID
                ? Id.of(((UpdatePinnedMessages) upd).peer())
                : Id.ofChannel(((UpdatePinnedChannelMessages) upd).channelId(), null);

        return Flux.just(new UpdatePinnedMessagesEvent(context.getClient(), upd.pinned(), chatId,
                upd.messages(), upd.pts(), upd.ptsCount()));
    }
}
