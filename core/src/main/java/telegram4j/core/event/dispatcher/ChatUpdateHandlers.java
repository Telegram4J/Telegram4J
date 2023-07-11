package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.chat.ChatEvent;
import telegram4j.core.event.domain.chat.ChatParticipantUpdateEvent;
import telegram4j.core.event.domain.chat.ChatParticipantsUpdateEvent;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.ChatParticipant;
import telegram4j.core.object.chat.ExportedChatInvite;
import telegram4j.core.object.chat.GroupChatPeer;
import telegram4j.core.util.Id;
import telegram4j.tl.*;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

class ChatUpdateHandlers {

    // State handler
    // =====================

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

    static Flux<ChatParticipantUpdateEvent> handleUpdateChatParticipant(StatefulUpdateContext<UpdateChatParticipant, Void> context) {
        UpdateChatParticipant upd = context.getUpdate();

        Id chatId = Id.ofChat(upd.chatId());
        Instant timestamp = Instant.ofEpochSecond(upd.date());
        ExportedChatInvite exportedChatInvite = upd.invite() instanceof ChatInviteExported d
                ? new ExportedChatInvite(context.getClient(), d)
                : null;
        boolean joinRequests = upd.invite() == ChatInvitePublicJoinRequests.instance();
        var chat = (GroupChatPeer) Objects.requireNonNull(context.getChats().get(chatId));
        User user = Objects.requireNonNull(context.getUsers().get(Id.ofUser(upd.userId())));
        User actor = Objects.requireNonNull(context.getUsers().get(Id.ofUser(upd.actorId())));
        ChatParticipant oldParticipant = Optional.ofNullable(upd.prevParticipant())
                .map(d -> new ChatParticipant(context.getClient(), user, d, chat.getId()))
                .orElse(null);
        ChatParticipant currentParticipant = Optional.ofNullable(upd.newParticipant())
                .map(d -> new ChatParticipant(context.getClient(), user, d, chat.getId()))
                .orElse(null);

        return Flux.just(new ChatParticipantUpdateEvent(context.getClient(), timestamp,
                oldParticipant, currentParticipant,
                exportedChatInvite, joinRequests, chat, actor));
    }

    static Flux<ChatEvent> handleUpdateChatParticipants(StatefulUpdateContext<UpdateChatParticipants, Void> context) {
        ChatParticipants chatParticipants = context.getUpdate().participants();
        Id chatId = Id.ofChat(chatParticipants.chatId());
        var chat = (GroupChatPeer) Objects.requireNonNull(context.getChats().get(chatId));
        return switch (chatParticipants.identifier()) {
            case ChatParticipantsForbidden.ID -> {
                var upd = (ChatParticipantsForbidden) chatParticipants;

                ChatParticipant selfParticipant = Optional.ofNullable(upd.selfParticipant())
                        .map(d -> new ChatParticipant(context.getClient(),
                                context.getUsers().get(Id.ofUser(d.userId())), d, chat.getId()))
                        .orElse(null);

                yield Flux.just(new ChatParticipantsUpdateEvent(context.getClient(),
                        chat, selfParticipant, null, null));
            }
            case BaseChatParticipants.ID -> {
                var upd = (BaseChatParticipants) chatParticipants;

                var participants = upd.participants().stream()
                        .map(d -> new ChatParticipant(context.getClient(),
                                context.getUsers().get(Id.ofUser(d.userId())), d, chat.getId()))
                        .toList();

                yield Flux.just(new ChatParticipantsUpdateEvent(context.getClient(), chat,
                        null, upd.version(), participants));
            }
            default -> Flux.error(new IllegalArgumentException("Unknown chat participants type: " + chatParticipants));
        };
    }
}
