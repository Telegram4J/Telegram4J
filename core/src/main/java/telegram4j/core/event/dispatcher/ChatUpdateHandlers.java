package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.chat.*;
import telegram4j.core.event.domain.user.UpdateChannelUserTypingEvent;
import telegram4j.core.object.Id;
import telegram4j.tl.*;

import java.time.Instant;

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
        return Flux.just(new ChatParticipantAddEvent(context.getClient(), context.getUpdate().chatId(),context.getUpdate().userId(),context.getUpdate().inviterId(), Instant.ofEpochSecond(context.getUpdate().date()),context.getUpdate().version()));
    }

    static Flux<ChatParticipantAdminEvent> handleUpdateChatParticipantAdmin(StatefulUpdateContext<UpdateChatParticipantAdmin, Void> context) {
        return Flux.just(new ChatParticipantAdminEvent(context.getClient(), context.getUpdate().chatId(),context.getUpdate().userId(),context.getUpdate().isAdmin(),context.getUpdate().version()));
    }

    static Flux<ChatParticipantDeleteEvent> handleUpdateChatParticipantDelete(StatefulUpdateContext<UpdateChatParticipantDelete, Void> context) {
        return Flux.just(new ChatParticipantDeleteEvent(context.getClient(), context.getUpdate().chatId(),context.getUpdate().userId(),context.getUpdate().version()));
    }

    static Flux<ChatParticipantUpdateEvent> handleUpdateChatParticipant(StatefulUpdateContext<UpdateChatParticipant, Void> context) {
        return Flux.just(new ChatParticipantUpdateEvent(context.getClient(), context.getUpdate().chatId(),Instant.ofEpochSecond(context.getUpdate().date()),context.getUpdate().actorId(),context.getUpdate().userId(),context.getUpdate().prevParticipant(),context.getUpdate().newParticipant(),context.getUpdate().invite(),context.getUpdate().qts()));
    }

    static Flux<ChatParticipantsUpdateEvent> handleUpdateChatParticipants(StatefulUpdateContext<UpdateChatParticipants, Void> context) {
        return Flux.just(new ChatParticipantsUpdateEvent(context.getClient(), context.getUpdate().participants()));
    }

}
