package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.Event;
import telegram4j.tl.*;

import java.util.ArrayList;
import java.util.List;

import static telegram4j.core.event.dispatcher.UpdatesMapper.StateUpdateHandler.noOp;

public final class UpdatesMapper {
    private final List<Handler<?, ?>> handlers = new ArrayList<>();

    private UpdatesMapper() {
        // message updates
        addHandler(UpdateNewMessage.class, MessageUpdateHandlers::persistUpdateNewMessage,
                MessageUpdateHandlers::handleUpdateNewMessage);
        addHandler(UpdateNewChannelMessage.class, MessageUpdateHandlers::persistUpdateNewChannelMessage,
                MessageUpdateHandlers::handleUpdateNewChannelMessage);
        addHandler(UpdateEditMessage.class, MessageUpdateHandlers::persistUpdateEditMessage,
                MessageUpdateHandlers::handleUpdateEditMessage);
        addHandler(UpdateEditChannelMessage.class, MessageUpdateHandlers::persistUpdateEditChannelMessage,
                MessageUpdateHandlers::handleUpdateEditChannelMessage);
        addHandler(UpdatePinnedMessages.class, MessageUpdateHandlers::persistUpdatePinnedMessages,
                MessageUpdateHandlers::handleUpdatePinnedMessages);
        addHandler(UpdateDeleteMessages.class, MessageUpdateHandlers::persistUpdateDeleteMessages,
                MessageUpdateHandlers::handleUpdateDeleteMessages);
        addHandler(UpdateDeleteScheduledMessages.class, MessageUpdateHandlers::persistUpdateDeleteScheduledMessages,
                MessageUpdateHandlers::handleUpdateDeleteScheduledMessages);
        addHandler(UpdateDeleteChannelMessages.class, MessageUpdateHandlers::persistUpdateDeleteChannelMessages,
                MessageUpdateHandlers::handleUpdateDeleteChannelMessages);
        addHandler(UpdatePinnedChannelMessages.class, MessageUpdateHandlers::persistUpdatePinnedChannelMessages,
                MessageUpdateHandlers::handleUpdatePinnedChannelMessages);
        addHandler(UpdateMessagePoll.class, MessageUpdateHandlers::persistUpdateMessagePoll,
                MessageUpdateHandlers::handleUpdateMessagePoll);
        addHandler(UpdateMessagePollVote.class, MessageUpdateHandlers::persistUpdateMessagePollVote,
                MessageUpdateHandlers::handleUpdateMessagePollVote);
        // chat updates
        addHandler(UpdateChatParticipant.class, ChatUpdateHandlers::handleStateUpdateChatParticipant,
                ChatUpdateHandlers::handleUpdateChatParticipant);
        addHandler(UpdateChatParticipants.class, ChatUpdateHandlers::handleStateUpdateChatParticipants,
                ChatUpdateHandlers::handleUpdateChatParticipants);
        // channel updates
        addHandler(UpdateChannelParticipant.class, ChannelUpdateHandlers::handleStateUpdateChatParticipant,
                ChannelUpdateHandlers::handleUpdateChannelParticipant);
        // bot updates
        addHandler(UpdateBotInlineQuery.class, noOp(), BotUpdatesHandlers::handleUpdateBotInlineQuery);
        addHandler(UpdateBotCallbackQuery.class, noOp(), BotUpdatesHandlers::handleUpdateBotCallbackQuery);
        addHandler(UpdateInlineBotCallbackQuery.class, noOp(), BotUpdatesHandlers::handleUpdateInlineBotCallbackQuery);
    }

    public static final UpdatesMapper instance = new UpdatesMapper();

    private <U extends Update, O> void addHandler(Class<? extends U> type,
                                                 StateUpdateHandler<U, O> updateHandler,
                                                 UpdateHandler<U, O> handler) {
        handlers.add(new Handler<>(type, updateHandler, handler));
    }

    @SuppressWarnings("unchecked")
    public <U extends Update> Flux<Event> handle(UpdateContext<U> context) {
        return Mono.justOrEmpty(handlers.stream()
                        .filter(t -> t.type.isAssignableFrom(context.getUpdate().getClass()))
                        .findFirst())
                .map(t -> (Handler<U, Object>) t)
                .flatMapMany(t -> t.updateHandler.handle(context)
                        .map(obj -> StatefulUpdateContext.from(context, obj))
                        .defaultIfEmpty(StatefulUpdateContext.from(context, null))
                        .flatMapMany(t.handler::handle));
    }

    record Handler<U extends Update, O>(Class<? extends U> type,
                                        StateUpdateHandler<U, O> updateHandler,
                                        UpdateHandler<U, O> handler) {}

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
