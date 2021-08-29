package telegram4j.core.dispatch;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.event.Event;

import java.util.ArrayList;
import java.util.List;

public class DefaultDispatchMapper implements DispatchMapper {

    private static final List<DispatchHandler<?, ?>> handlers = new ArrayList<>();

    static {
        handlers.add(new ChatDispatchHandlers.MessageCreate());
        handlers.add(new ChatDispatchHandlers.MessageUpdate());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Event, O> Mono<E> handle(UpdateContext<O> update) {
        return Flux.fromIterable(handlers)
                .map(handler -> (DispatchHandler<E, O>) handler)
                .filter(handler -> handler.canHandle(update))
                .singleOrEmpty()
                .flatMap(handler -> handler.handle(update));
    }
}
