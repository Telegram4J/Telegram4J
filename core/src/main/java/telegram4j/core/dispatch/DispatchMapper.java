package telegram4j.core.dispatch;

import reactor.core.publisher.Mono;
import telegram4j.core.event.Event;

public interface DispatchMapper {

    <E extends Event, O> Mono<E> handle(UpdateContext<O> update);
}
