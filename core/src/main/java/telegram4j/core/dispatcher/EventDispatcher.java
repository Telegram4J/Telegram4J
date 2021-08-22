package telegram4j.core.dispatcher;

import reactor.core.publisher.Flux;
import telegram4j.core.event.Event;

public interface EventDispatcher {

    <E extends Event> void publish(E event);

    <E extends Event> Flux<E> on(Class<E> type);
}
