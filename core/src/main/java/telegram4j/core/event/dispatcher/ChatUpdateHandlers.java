package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.chat.*;
import telegram4j.core.object.ExportedChatInvite;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.ChatParticipant;
import telegram4j.core.object.chat.GroupChat;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;

import java.time.Instant;
import java.util.Objects;
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

        GroupChat chat = (GroupChat) Objects.requireNonNull(context.getChats().get(upd.chatId()));
        User user = Objects.requireNonNull(context.getUsers().get(upd.userId()));
        User inviter = Objects.requireNonNull(context.getUsers().get(upd.inviterId()));
        Instant timestamp = Instant.ofEpochSecond(upd.date());

        return Flux.just(new ChatParticipantAddEvent(context.getClient(),
                chat, user, inviter, timestamp, upd.version()));
    }

    static Flux<ChatParticipantAdminEvent> handleUpdateChatParticipantAdmin(StatefulUpdateContext<UpdateChatParticipantAdmin, Void> context) {
        UpdateChatParticipantAdmin upd = context.getUpdate();

        if (!context.getChats().containsKey(upd.chatId())) {
            return Flux.empty();
        }

        GroupChat chat = (GroupChat) Objects.requireNonNull(context.getChats().get(upd.chatId()));
        User user = Objects.requireNonNull(context.getUsers().get(upd.userId()));

        return Flux.just(new ChatParticipantAdminEvent(context.getClient(), chat, user,
                upd.isAdmin(), upd.version()));
    }

    static Flux<ChatParticipantDeleteEvent> handleUpdateChatParticipantDelete(StatefulUpdateContext<UpdateChatParticipantDelete, Void> context) {
        UpdateChatParticipantDelete upd = context.getUpdate();

        if (!context.getChats().containsKey(upd.chatId())) {
            return Flux.empty();
        }

        GroupChat chat = (GroupChat) Objects.requireNonNull(context.getChats().get(upd.chatId()));
        User user = Objects.requireNonNull(context.getUsers().get(upd.userId()));

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
                .map(d -> new ExportedChatInvite(context.getClient(), d, context.getUsers().get(d.adminId())))
                .orElse(null);
        GroupChat chat = (GroupChat) Objects.requireNonNull(context.getChats().get(upd.chatId()));
        User user = Objects.requireNonNull(context.getUsers().get(upd.userId()));
        User actor = Objects.requireNonNull(context.getUsers().get(upd.actorId()));
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

                GroupChat chat = (GroupChat) Objects.requireNonNull(context.getChats().get(upd.chatId()));
                ChatParticipant selfParticipant = Optional.ofNullable(upd.selfParticipant())
                        .map(d -> new ChatParticipant(context.getClient(),
                                context.getUsers().get(d.userId()), d, chat.getId()))
                        .orElse(null);

                return Flux.just(new ChatParticipantsUpdateEvent(context.getClient(),
                        chat, selfParticipant, null, null));
            }
            case BaseChatParticipants.ID: {
                BaseChatParticipants upd = (BaseChatParticipants) chatParticipants;

                GroupChat chat = (GroupChat) Objects.requireNonNull(context.getChats().get(upd.chatId()));
                var participants = upd.participants().stream()
                        .map(d -> new ChatParticipant(context.getClient(),
                                context.getUsers().get(d.userId()), d, chat.getId()))
                        .collect(Collectors.toUnmodifiableList());

                return Flux.just(new ChatParticipantsUpdateEvent(context.getClient(), chat,
                        null, upd.version(), participants));
            }
            default:
                return Flux.error(new IllegalArgumentException("Unknown chat participants type: " + chatParticipants));
        }
    }

}
