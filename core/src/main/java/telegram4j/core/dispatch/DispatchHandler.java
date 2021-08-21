package telegram4j.core.dispatch;

import reactor.core.publisher.Mono;
import telegram4j.core.event.Event;

public interface DispatchHandler<E extends Event> {

    boolean canHandle(UpdateContext update);

    Mono<E> handle(UpdateContext update);
}
