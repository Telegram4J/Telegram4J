package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import telegram4j.core.event.Event;
import telegram4j.tl.Update;
import telegram4j.tl.UpdateNewMessage;

import java.util.ArrayList;
import java.util.List;

public final class UpdatesHandlers {
    private static final List<Tuple2<Class<? extends Update>, UpdateHandler<?>>> handlers = new ArrayList<>();

    static {

        addHandler(UpdateNewMessage.class, MessageUpdateHandlers::handleUpdateNewMessage);
    }

    static <U extends Update> void addHandler(Class<? extends U> type, UpdateHandler<U> handler) {
        handlers.add(Tuples.of(type, handler));
    }

    @SuppressWarnings("unchecked")
    public <E extends Event, U extends Update> Flux<E> handle(UpdateContext<U> context) {
        return Mono.justOrEmpty(handlers.stream()
                        .filter(TupleUtils.predicate((type, handler) -> type.isAssignableFrom(context.getUpdate().getClass())))
                        .map(Tuple2::getT2)
                        .findFirst())
                .map(handler -> (UpdateHandler<U>) handler)
                .flatMapMany(handler -> handler.handle(context))
                .map(event -> (E) event);
    }

    @FunctionalInterface
    interface UpdateHandler<U extends Update> {

        Flux<? extends Event> handle(UpdateContext<U> context);
    }
}
