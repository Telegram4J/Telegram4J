package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.Event;
import telegram4j.tl.*;

import java.util.ArrayList;
import java.util.List;

public final class UpdatesHandlers {
    private static final List<HandlerTuple<?, ?>> handlers = new ArrayList<>();

    static {
        // message updates
        addHandler(UpdateNewMessageFields.class, MessageUpdateHandlers::handleStateUpdateNewMessage,
                MessageUpdateHandlers::handleUpdateNewMessage);
        addHandler(UpdateEditMessageFields.class, MessageUpdateHandlers::handleStateUpdateEditMessage,
                MessageUpdateHandlers::handleUpdateEditMessage);
        addHandler(UpdateDeleteMessagesFields.class, MessageUpdateHandlers::handleStateUpdateDeleteMessages,
                MessageUpdateHandlers::handleUpdateDeleteMessages);
        // user updates
        addHandler(UpdateChannelUserTyping.class, UserUpdateHandlers::handleStateUpdateChannelUserTyping,
                UserUpdateHandlers::handleUpdateChannelUserTyping);
        addHandler(UpdateChatUserTyping.class, UserUpdateHandlers::handleStateUpdateChatUserTyping,
                UserUpdateHandlers::handleUpdateChatUserTyping);
        addHandler(UpdateUserName.class, UserUpdateHandlers::handleStateUpdateUserName,
                UserUpdateHandlers::handleUpdateUserName);
        addHandler(UpdateUserPhone.class, UserUpdateHandlers::handleStateUpdateUserPhone,
                UserUpdateHandlers::handleUpdateUserPhone);
        addHandler(UpdateUserPhoto.class, UserUpdateHandlers::handleStateUpdateUserPhoto,
                UserUpdateHandlers::handleUpdateUserPhoto);
        addHandler(UpdateUserStatus.class, UserUpdateHandlers::handleStateUpdateUserStatus,
                UserUpdateHandlers::handleUpdateUserStatus);
        addHandler(UpdateUserTyping.class, UserUpdateHandlers::handleStateUpdateUserTyping,
                UserUpdateHandlers::handleUpdateUserTyping);
        // chat updates
        addHandler(UpdateChatParticipantAdd.class,
                ChatUpdateHandlers::handleStateUpdateChatParticipantAdd,ChatUpdateHandlers::handleUpdateChatParticipantAdd);
        addHandler(UpdateChatParticipantAdmin.class,
                ChatUpdateHandlers::handleStateUpdateChatParticipantAdmin,ChatUpdateHandlers::handleUpdateChatParticipantAdmin);
        addHandler(UpdateChatParticipantDelete.class,
                ChatUpdateHandlers::handleStateUpdateChatParticipantDelete,ChatUpdateHandlers::handleUpdateChatParticipantDelete);
        addHandler(UpdateChatParticipant.class,
                ChatUpdateHandlers::handleStateUpdateChatParticipant, ChatUpdateHandlers::handleUpdateChatParticipant);
        addHandler(UpdateChatParticipants.class,
                ChatUpdateHandlers::handleStateUpdateChatParticipants, ChatUpdateHandlers::handleUpdateChatParticipants);
        // channel updates
        addHandler(UpdateChannelParticipant.class, ChannelUpdateHandlers::handleStateUpdateChatParticipant,
                ChannelUpdateHandlers::handleUpdateChannelParticipant);
    }

    public static final UpdatesHandlers instance = new UpdatesHandlers();

    private UpdatesHandlers() {
    }

    static <U extends Update, O> void addHandler(Class<? extends U> type,
                                                 StateUpdateHandler<U, O> updateHandler,
                                                 UpdateHandler<U, O> handler) {
        handlers.add(new HandlerTuple<>(type, updateHandler, handler));
    }

    @SuppressWarnings("unchecked")
    public <U extends Update> Flux<Event> handle(UpdateContext<U> context) {
        return Mono.justOrEmpty(handlers.stream()
                        .filter(t -> t.type.isAssignableFrom(context.getUpdate().getClass()))
                        .findFirst())
                .map(t -> (HandlerTuple<U, Object>) t)
                .flatMapMany(t -> t.updateHandler.handle(context)
                        .map(obj -> StatefulUpdateContext.from(context, obj))
                        .defaultIfEmpty(StatefulUpdateContext.from(context, null))
                        .flatMapMany(t.handler::handle));
    }

    static class HandlerTuple<U extends Update, O> {
        final Class<? extends U> type;
        final StateUpdateHandler<U, O> updateHandler;
        final UpdateHandler<U, O> handler;

        HandlerTuple(Class<? extends U> type, StateUpdateHandler<U, O> updateHandler, UpdateHandler<U, O> handler) {
            this.type = type;
            this.updateHandler = updateHandler;
            this.handler = handler;
        }
    }

    @FunctionalInterface
    interface UpdateHandler<U extends Update, O> {

        Flux<? extends Event> handle(StatefulUpdateContext<U, O> context);
    }

    @FunctionalInterface
    interface StateUpdateHandler<U extends Update, O> {

        Mono<O> handle(UpdateContext<U> context);
    }
}
