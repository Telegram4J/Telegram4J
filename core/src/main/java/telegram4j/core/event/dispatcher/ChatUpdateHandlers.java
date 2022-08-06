package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.chat.*;
import telegram4j.core.object.ExportedChatInvite;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.ChatParticipant;
import telegram4j.core.object.chat.GroupChat;
import telegram4j.core.util.EntityFactory;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;

class ChatUpdateHandlers {

    // State handler
    // =====================

    static Mono<Void> handleStateUpdateChatParticipantAdd(UpdateContext<UpdateChatParticipantAdd> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onChatParticipantAdd(context.getUpdate());
    }

    static Mono<Void> handleStateUpdateChatParticipantAdmin(UpdateContext<UpdateChatParticipantAdmin> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onChatParticipantAdmin(context.getUpdate());
    }

    static Mono<Void> handleStateUpdateChatParticipantDelete(UpdateContext<UpdateChatParticipantDelete> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onChatParticipantDelete(context.getUpdate());
    }

    static Mono<Void> handleStateUpdateChatParticipant(UpdateContext<UpdateChatParticipant> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onChatParticipant(context.getUpdate());
    }

    static Mono<Void> handleStateUpdateChatParticipants(UpdateContext<UpdateChatParticipants> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onChatParticipants(context.getUpdate().participants());
    }

    // Update handler
    // =====================

    // I couldn't figure out when the following events are called,
    // so I haven't tested them and can't be sure about mapping:
    // - UpdateChatParticipantAdd
    // - UpdateChatParticipantAdmin
    // - UpdateChatParticipantDelete

    static Flux<ChatParticipantAddEvent> handleUpdateChatParticipantAdd(StatefulUpdateContext<UpdateChatParticipantAdd, Void> context) {
        UpdateChatParticipantAdd upd = context.getUpdate();

        if (!context.getChats().containsKey(upd.chatId())) {
            return Flux.empty();
        }

        GroupChat chat = Optional.ofNullable(context.getChats().get(upd.chatId()))
                .map(d -> (GroupChat) EntityFactory.createChat(context.getClient(), d, null))
                .orElseThrow();
        User user = Optional.ofNullable(context.getUsers().get(upd.userId()))
                .map(d -> new User(context.getClient(), d))
                .orElseThrow();
        User inviter = Optional.ofNullable(context.getUsers().get(upd.inviterId()))
                .map(d -> new User(context.getClient(), d))
                .orElseThrow();
        Instant timestamp = Instant.ofEpochSecond(upd.date());

        return Flux.just(new ChatParticipantAddEvent(context.getClient(),
                chat, user, inviter, timestamp, upd.version()));
    }

    static Flux<ChatParticipantAdminEvent> handleUpdateChatParticipantAdmin(StatefulUpdateContext<UpdateChatParticipantAdmin, Void> context) {
        UpdateChatParticipantAdmin upd = context.getUpdate();

        if (!context.getChats().containsKey(upd.chatId())) {
            return Flux.empty();
        }

        GroupChat chat = Optional.ofNullable(context.getChats().get(upd.chatId()))
                .map(d -> (GroupChat) EntityFactory.createChat(context.getClient(), d, null))
                .orElseThrow();
        User user = Optional.ofNullable(context.getUsers().get(upd.userId()))
                .map(d -> new User(context.getClient(), d))
                .orElseThrow();

        return Flux.just(new ChatParticipantAdminEvent(context.getClient(), chat, user,
                upd.isAdmin(), upd.version()));
    }

    static Flux<ChatParticipantDeleteEvent> handleUpdateChatParticipantDelete(StatefulUpdateContext<UpdateChatParticipantDelete, Void> context) {
        UpdateChatParticipantDelete upd = context.getUpdate();

        if (!context.getChats().containsKey(upd.chatId())) {
            return Flux.empty();
        }

        GroupChat chat = Optional.ofNullable(context.getChats().get(upd.chatId()))
                .map(d -> (GroupChat) EntityFactory.createChat(context.getClient(), d, null))
                .orElseThrow();
        User user = Optional.ofNullable(context.getUsers().get(upd.userId()))
                .map(d -> new User(context.getClient(), d))
                .orElseThrow();

        return Flux.just(new ChatParticipantDeleteEvent(context.getClient(), chat, user, upd.version()));
    }

    static Flux<ChatParticipantUpdateEvent> handleUpdateChatParticipant(StatefulUpdateContext<UpdateChatParticipant, Void> context) {
        UpdateChatParticipant upd = context.getUpdate();

        if (!context.getChats().containsKey(upd.chatId())) {
            return Flux.empty();
        }

        Instant timestamp = Instant.ofEpochSecond(upd.date());
        ExportedChatInvite exportedChatInvite = Optional.ofNullable(upd.invite())
                .map(e -> TlEntityUtil.unmapEmpty(e, ChatInviteExported.class))
                .map(d -> {
                    User admin = Optional.ofNullable(context.getUsers().get(d.adminId()))
                            .map(u -> EntityFactory.createUser(context.getClient(), u))
                            .orElseThrow();

                    return new ExportedChatInvite(context.getClient(), d, admin);
                })
                .orElse(null);
        GroupChat chat = Optional.ofNullable(context.getChats().get(upd.chatId()))
                .map(d -> (GroupChat) EntityFactory.createChat(context.getClient(), d, null))
                .orElseThrow();
        User user = Optional.ofNullable(context.getUsers().get(upd.userId()))
                .map(d -> new User(context.getClient(), d))
                .orElseThrow();
        User actor = Optional.ofNullable(context.getUsers().get(upd.actorId()))
                .map(d -> new User(context.getClient(), d))
                .orElseThrow();
        ChatParticipant oldParticipant = Optional.ofNullable(upd.prevParticipant())
                .map(d -> new ChatParticipant(context.getClient(), user, d, chat.getId()))
                .orElse(null);
        ChatParticipant currentParticipant = Optional.ofNullable(upd.newParticipant())
                .map(d -> new ChatParticipant(context.getClient(), user, d, chat.getId()))
                .orElse(null);

        return Flux.just(new ChatParticipantUpdateEvent(context.getClient(), timestamp,
                oldParticipant, currentParticipant, exportedChatInvite, upd.qts(),
                chat, actor));
    }

    static Flux<ChatEvent> handleUpdateChatParticipants(StatefulUpdateContext<UpdateChatParticipants, Void> context) {
        ChatParticipants chatParticipants = context.getUpdate().participants();
        switch (chatParticipants.identifier()) {
            case ChatParticipantsForbidden.ID: {
                ChatParticipantsForbidden upd = (ChatParticipantsForbidden) chatParticipants;

                User selfUser = Optional.ofNullable(upd.selfParticipant())
                        .map(p -> context.getUsers().get(context.getClient().getSelfId().asLong()))
                        .map(u -> EntityFactory.createUser(context.getClient(), u))
                        .orElse(null);
                GroupChat chat = Optional.ofNullable(context.getChats().get(upd.chatId()))
                        .map(d -> (GroupChat) EntityFactory.createChat(context.getClient(), d, null))
                        .orElseThrow();
                ChatParticipant selfParticipant = Optional.ofNullable(upd.selfParticipant())
                        .map(d -> new ChatParticipant(context.getClient(), selfUser, d, chat.getId()))
                        .orElse(null);

                return Flux.just(new ChatParticipantsUpdateEvent(context.getClient(), chat, selfParticipant, null, null));
            }
            case BaseChatParticipants.ID: {
                BaseChatParticipants upd = (BaseChatParticipants) chatParticipants;

                GroupChat chat = Optional.ofNullable(context.getChats().get(upd.chatId()))
                        .map(d -> (GroupChat) EntityFactory.createChat(context.getClient(), d, null))
                        .orElseThrow();
                var participants = upd.participants().stream()
                        .map(d -> {
                            User user = Optional.ofNullable(context.getUsers().get(d.userId()))
                                    .map(u -> EntityFactory.createUser(context.getClient(), u))
                                    .orElse(null);

                            return new ChatParticipant(context.getClient(), user, d, chat.getId());
                        })
                        .collect(Collectors.toUnmodifiableList());
                int version = upd.version();

                return Flux.just(new ChatParticipantsUpdateEvent(context.getClient(), chat, null, version, participants));
            }
            default:
                return Flux.error(new IllegalArgumentException("Unknown chat participants type: " + chatParticipants));
        }
    }

}
