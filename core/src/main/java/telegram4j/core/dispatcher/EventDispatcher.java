package telegram4j.core.dispatcher;

import reactor.core.publisher.Flux;
import telegram4j.core.event.Event;

/**
 * Distributes incoming {@link Event}s to a subscribers.
 * To retrieves {@link Flux} of {@link Event}s uses the {@link #on(Class)}
 * method with the event type in a parameters.
 * And to publish {@link #publish(Event)}
 */
public interface EventDispatcher {

    /**
     * Publishes an {@link Event} to the dispatcher.
     *
     * @param event the {@link Event} instance to publish.
     * @param <E> the event type.
     */
    <E extends Event> void publish(E event);

    /**
     * Retrieves a {@link Flux} of the specified {@link Event} type for subsequent processing and subscription.
     *
     * @param type the event class of requested events.
     * @param <E> the event type.
     * @return a {@link Flux} of events.
     */
    <E extends Event> Flux<E> on(Class<E> type);
}
