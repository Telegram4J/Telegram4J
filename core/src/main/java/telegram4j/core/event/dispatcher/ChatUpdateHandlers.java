package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.chat.*;
import telegram4j.core.object.ExportedChatInvite;
import telegram4j.core.object.Id;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.chat.ChatParticipant;
import telegram4j.core.util.EntityFactory;
import telegram4j.tl.*;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;

public class ChatUpdateHandlers {

    // State handler
    // =====================

    static Mono<Void> handleStateUpdateChatParticipantAdd(UpdateContext<UpdateChatParticipantAdd> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onChatParticipantAdd(context.getUpdate(), context.getChats(), context.getUsers());
    }

    static Mono<Void> handleStateUpdateChatParticipantAdmin(UpdateContext<UpdateChatParticipantAdmin> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onChatParticipantAdmin(context.getUpdate(), context.getChats(), context.getUsers());
    }

    static Mono<Void> handleStateUpdateChatParticipantDelete(UpdateContext<UpdateChatParticipantDelete> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onChatParticipantDelete(context.getUpdate(), context.getChats(), context.getUsers());
    }

    static Mono<Void> handleStateUpdateChatParticipant(UpdateContext<UpdateChatParticipant> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onChatParticipant(context.getUpdate(), context.getChats(), context.getUsers());
    }

    static Mono<Void> handleStateUpdateChatParticipants(UpdateContext<UpdateChatParticipants> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onChatParticipants(context.getUpdate(), context.getChats(), context.getUsers());
    }

    // Update handler
    // =====================

    static Flux<ChatParticipantAddEvent> handleUpdateChatParticipantAdd(StatefulUpdateContext<UpdateChatParticipantAdd, Void> context) {
        UpdateChatParticipantAdd upd = context.getUpdate();

        Chat chat = Optional.ofNullable(context.getChats().get(upd.chatId()))
                .map(d -> EntityFactory.createChat(context.getClient(), d))
                .orElseThrow();
        User user = Optional.ofNullable(context.getUsers().get(upd.userId()))
                .filter(u -> u.identifier() == BaseUser.ID)
                .map(d -> new User(context.getClient(), (BaseUser) d))
                .orElseThrow();
        User inviter = Optional.ofNullable(context.getUsers().get(upd.inviterId()))
                .filter(u -> u.identifier() == BaseUser.ID)
                .map(d -> new User(context.getClient(), (BaseUser) d))
                .orElseThrow();
        Instant timestamp = Instant.ofEpochSecond(upd.date());

        return Flux.just(new ChatParticipantAddEvent(context.getClient(),
                chat, user, inviter, timestamp, upd.version()));
    }

    static Flux<ChatParticipantAdminEvent> handleUpdateChatParticipantAdmin(StatefulUpdateContext<UpdateChatParticipantAdmin, Void> context) {
        UpdateChatParticipantAdmin upd = context.getUpdate();

        Chat chat = Optional.ofNullable(context.getChats().get(upd.chatId()))
                .map(d -> EntityFactory.createChat(context.getClient(), d))
                .orElseThrow();
        User user = Optional.ofNullable(context.getUsers().get(upd.userId()))
                .filter(u -> u.identifier() == BaseUser.ID)
                .map(d -> new User(context.getClient(), (BaseUser) d))
                .orElseThrow();

        return Flux.just(new ChatParticipantAdminEvent(context.getClient(), chat, user,
                upd.isAdmin(), upd.version()));
    }

    static Flux<ChatParticipantDeleteEvent> handleUpdateChatParticipantDelete(StatefulUpdateContext<UpdateChatParticipantDelete, Void> context) {
        UpdateChatParticipantDelete upd = context.getUpdate();

        Chat chat = Optional.ofNullable(context.getChats().get(upd.chatId()))
                .map(d -> EntityFactory.createChat(context.getClient(), d))
                .orElseThrow();
        User user = Optional.ofNullable(context.getUsers().get(upd.userId()))
                .filter(u -> u.identifier() == BaseUser.ID)
                .map(d -> new User(context.getClient(), (BaseUser) d))
                .orElseThrow();

        return Flux.just(new ChatParticipantDeleteEvent(context.getClient(), chat, user, upd.version()));
    }

    static Flux<ChatParticipantUpdateEvent> handleUpdateChatParticipant(StatefulUpdateContext<UpdateChatParticipant, Void> context) {
        UpdateChatParticipant upd = context.getUpdate();

        Instant timestamp = Instant.ofEpochSecond(upd.date());
        ExportedChatInvite exportedChatInvite = Optional.ofNullable(upd.invite())
                .map(d -> new ExportedChatInvite(context.getClient(), d))
                .orElse(null);
        Chat chat = Optional.ofNullable(context.getChats().get(upd.chatId()))
                .map(d -> EntityFactory.createChat(context.getClient(), d))
                .orElseThrow();
        User user = Optional.ofNullable(context.getUsers().get(upd.userId()))
                .filter(u -> u.identifier() == BaseUser.ID)
                .map(d -> new User(context.getClient(), (BaseUser) d))
                .orElseThrow();
        User actor = Optional.ofNullable(context.getUsers().get(upd.actorId()))
                .filter(u -> u.identifier() == BaseUser.ID)
                .map(d -> new User(context.getClient(), (BaseUser) d))
                .orElseThrow();
        ChatParticipant oldParticipant = Optional.ofNullable(upd.prevParticipant())
                .map(d -> new ChatParticipant(context.getClient(), d))
                .orElse(null);
        ChatParticipant currentParticipant = Optional.ofNullable(upd.newParticipant())
                .map(d -> new ChatParticipant(context.getClient(), d))
                .orElse(null);

        return Flux.just(new ChatParticipantUpdateEvent(context.getClient(), timestamp,
                oldParticipant, currentParticipant, exportedChatInvite, upd.qts(),
                chat, actor, user));
    }

    // TODO: make ChatParticipant extends User or something like this to don't drop user data.
    static Flux<ChatEvent> handleUpdateChatParticipants(StatefulUpdateContext<UpdateChatParticipants, Void> context) {
        ChatParticipants chatParticipants = context.getUpdate().participants();
        switch (chatParticipants.identifier()) {
            case ChatParticipantsForbidden.ID: {
                ChatParticipantsForbidden upd = (ChatParticipantsForbidden) chatParticipants;
                Id chatId = Id.ofChat(upd.chatId());
                ChatParticipant selfParticipant = Optional.ofNullable(upd.selfParticipant())
                        .map(d -> new ChatParticipant(context.getClient(), d))
                        .orElse(null);

                return Flux.just(new ChatParticipantsUpdateEvent(context.getClient(), chatId, selfParticipant, null, null));
            }
            case BaseChatParticipants.ID: {
                BaseChatParticipants upd = (BaseChatParticipants) chatParticipants;
                Id chatId = Id.ofChat(upd.chatId());
                var participants = upd.participants().stream()
                        .map(d -> new ChatParticipant(context.getClient(), d))
                        .collect(Collectors.toList());
                int version = upd.version();

                return Flux.just(new ChatParticipantsUpdateEvent(context.getClient(), chatId, null, version, participants));
            }
            default:
                return Flux.error(new IllegalArgumentException("Unknown chat participants type: " + chatParticipants));
        }
    }

}
