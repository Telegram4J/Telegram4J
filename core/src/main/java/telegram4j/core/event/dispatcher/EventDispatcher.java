package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import telegram4j.core.event.Event;

public interface EventDispatcher {

    <E extends Event> Flux<E> on(Class<E> type);

    void publish(Event event);
}
