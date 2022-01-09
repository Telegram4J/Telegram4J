package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.chat.*;
import telegram4j.core.object.ExportedChatInvite;
import telegram4j.core.object.Id;
import telegram4j.tl.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
        Id chatId = Id.ofChat(context.getUpdate().chatId());
        Id userId = Id.ofUser(context.getUpdate().userId(), null);
        Id inviterId = Id.ofUser(context.getUpdate().inviterId(), null);
        Instant timestamp = Instant.ofEpochSecond(context.getUpdate().date());

        return Flux.just(new ChatParticipantAddEvent(context.getClient(),
                chatId, userId, inviterId, timestamp, context.getUpdate().version()));
    }

    static Flux<ChatParticipantAdminEvent> handleUpdateChatParticipantAdmin(StatefulUpdateContext<UpdateChatParticipantAdmin, Void> context) {
        Id chatId = Id.ofChat(context.getUpdate().chatId());
        Id userId = Id.ofUser(context.getUpdate().userId(), null);

        return Flux.just(new ChatParticipantAdminEvent(context.getClient(), chatId, userId,
                context.getUpdate().isAdmin(), context.getUpdate().version()));
    }

    static Flux<ChatParticipantDeleteEvent> handleUpdateChatParticipantDelete(StatefulUpdateContext<UpdateChatParticipantDelete, Void> context) {
        Id chatId = Id.ofChat(context.getUpdate().chatId());
        Id userId = Id.ofUser(context.getUpdate().userId(), null);

        return Flux.just(new ChatParticipantDeleteEvent(context.getClient(), chatId, userId, context.getUpdate().version()));
    }

    static Flux<ChatParticipantUpdateEvent> handleUpdateChatParticipant(StatefulUpdateContext<UpdateChatParticipant, Void> context) {
        Id chatId = Id.ofChat(context.getUpdate().chatId());
        Id userId = Id.ofUser(context.getUpdate().userId(), null);
        Id actorId = Id.ofUser(context.getUpdate().actorId(), null);
        Instant timestamp = Instant.ofEpochSecond(context.getUpdate().date());
        ExportedChatInvite exportedChatInvite = Optional.ofNullable(context.getUpdate().invite())
                .map(d -> new ExportedChatInvite(context.getClient(), d))
                .orElse(null);

        return Flux.just(new ChatParticipantUpdateEvent(context.getClient(), chatId, timestamp, actorId, userId,
                context.getUpdate().prevParticipant(), context.getUpdate().newParticipant(),
                exportedChatInvite, context.getUpdate().qts()));
    }

    static Flux<ChatEvent> handleUpdateChatParticipants(StatefulUpdateContext<UpdateChatParticipants, Void> context) {
        ChatParticipants chatParticipants = context.getUpdate().participants();
        switch (chatParticipants.identifier()) {
            case ChatParticipantsForbidden.ID: {
                ChatParticipantsForbidden upd = (ChatParticipantsForbidden) chatParticipants;
                Id chatId = Id.ofChat(upd.chatId());
                return Flux.just(new ChatParticipantsUpdateEvent(context.getClient(), chatId, upd.selfParticipant(), null, null));
            }
            case BaseChatParticipants.ID: {
                BaseChatParticipants upd = (BaseChatParticipants) chatParticipants;
                Id chatId = Id.ofChat(upd.chatId());
                List<ChatParticipant> participants = upd.participants();
                int version = upd.version();
                return Flux.just(new ChatParticipantsUpdateEvent(context.getClient(), chatId, null, version, participants));
            }
            default:
                return Flux.error(new IllegalArgumentException("Unknown chat participants type: " + chatParticipants));
        }
    }

}
