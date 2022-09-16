package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.Event;
import telegram4j.tl.*;

import java.util.ArrayList;
import java.util.List;

public final class DefaultUpdatesMapper implements UpdatesMapper {
    private static final List<HandlerTuple<?, ?>> handlers = new ArrayList<>();

    static {
        // message updates
        addHandler(UpdateNewMessageFields.class, MessageUpdateHandlers::handleStateUpdateNewMessage,
                MessageUpdateHandlers::handleUpdateNewMessage);
        addHandler(UpdateEditMessageFields.class, MessageUpdateHandlers::handleStateUpdateEditMessage,
                MessageUpdateHandlers::handleUpdateEditMessage);
        addHandler(UpdateDeleteMessagesFields.class, MessageUpdateHandlers::handleStateUpdateDeleteMessages,
                MessageUpdateHandlers::handleUpdateDeleteMessages);
        addHandler(UpdatePinnedMessagesFields.class, MessageUpdateHandlers::handleStateUpdatePinnedMessages,
                MessageUpdateHandlers::handleUpdatePinnedMessages);
        // chat updates
        addHandler(UpdateChatParticipantAdd.class, ChatUpdateHandlers::handleStateUpdateChatParticipantAdd,
                ChatUpdateHandlers::handleUpdateChatParticipantAdd);
        addHandler(UpdateChatParticipantAdmin.class, ChatUpdateHandlers::handleStateUpdateChatParticipantAdmin,
                ChatUpdateHandlers::handleUpdateChatParticipantAdmin);
        addHandler(UpdateChatParticipantDelete.class, ChatUpdateHandlers::handleStateUpdateChatParticipantDelete,
                ChatUpdateHandlers::handleUpdateChatParticipantDelete);
        addHandler(UpdateChatParticipant.class, ChatUpdateHandlers::handleStateUpdateChatParticipant,
                ChatUpdateHandlers::handleUpdateChatParticipant);
        addHandler(UpdateChatParticipants.class, ChatUpdateHandlers::handleStateUpdateChatParticipants,
                ChatUpdateHandlers::handleUpdateChatParticipants);
        // channel updates
        addHandler(UpdateChannelParticipant.class, ChannelUpdateHandlers::handleStateUpdateChatParticipant,
                ChannelUpdateHandlers::handleUpdateChannelParticipant);
        // bot updates
        addHandler(UpdateBotInlineQuery.class, StateUpdateHandler.noOp(), BotUpdatesHandlers::handleUpdateBotInlineQuery);
        addHandler(UpdateBotCallbackQuery.class, StateUpdateHandler.noOp(), BotUpdatesHandlers::handleUpdateBotCallbackQuery);
        addHandler(UpdateInlineBotCallbackQuery.class, StateUpdateHandler.noOp(),
                BotUpdatesHandlers::handleUpdateInlineBotCallbackQuery);
    }

    public static final DefaultUpdatesMapper instance = new DefaultUpdatesMapper();

    private DefaultUpdatesMapper() {
    }

    static <U extends Update, O> void addHandler(Class<? extends U> type,
                                                 StateUpdateHandler<U, O> updateHandler,
                                                 UpdateHandler<U, O> handler) {
        handlers.add(new HandlerTuple<>(type, updateHandler, handler));
    }

    @SuppressWarnings("unchecked")
    @Override
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

        static <U extends Update, O> StateUpdateHandler<U, O> noOp() {
            return ctx -> Mono.empty();
        }

        Mono<O> handle(UpdateContext<U> context);
    }
}
