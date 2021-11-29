package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.Event;
import telegram4j.tl.Update;
import telegram4j.tl.UpdateEditMessage;
import telegram4j.tl.UpdateNewMessage;

import java.util.ArrayList;
import java.util.List;

public final class UpdatesHandlers {
    private static final List<HandlerTuple<?, ?>> handlers = new ArrayList<>();

    static {

        addHandler(UpdateNewMessage.class, MessageUpdateHandlers::handleStateUpdateNewMessage,
                MessageUpdateHandlers::handleUpdateNewMessage);
        addHandler(UpdateEditMessage.class, MessageUpdateHandlers::handleStateUpdateEditMessage,
                MessageUpdateHandlers::handleUpdateEditMessage);
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
    public <U extends Update> Flux<? extends Event> handle(UpdateContext<U> context) {
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
        private final Class<? extends U> type;
        private final StateUpdateHandler<U, O> updateHandler;
        private final UpdateHandler<U, O> handler;

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
