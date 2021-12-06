package telegram4j.core.event;

import reactor.core.publisher.Flux;
import telegram4j.core.event.domain.Event;

public interface EventDispatcher {

    <E extends Event> Flux<E> on(Class<E> type);

    void publish(Event event);
}
