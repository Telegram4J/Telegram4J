package telegram4j.core.event;

import reactor.core.publisher.Flux;
import telegram4j.core.event.domain.Event;

/**
 * Distributes incoming {@link Event}s to a subscribers.
 * To retrieves {@link Flux} of {@link Event}s uses the {@link #on(Class)}
 * method with the event type in a parameters.
 * And to publish {@link #publish(Event)}
 */
public interface EventDispatcher {

    /**
     * Retrieves a {@link Flux} of the specified {@link Event} type for
     * subsequent processing and subscription.
     *
     * <p> This method doesn't handle errors occurred while processing
     * events, and occurred errors will terminate reactive sequence. For preventing
     * this behavior you should use special operators to handle them,
     * see <a href="https://projectreactor.io/docs/core/release/reference/#error.handling">this</a>
     * article o reactor wiki.
     *
     * @param type the event class of requested events.
     * @param <E> the event type.
     * @return a {@link Flux} of events.
     */
    <E extends Event> Flux<E> on(Class<E> type);

    /**
     * Publishes an {@link Event} to the dispatcher.
     *
     * @param event the {@link Event} instance to publish.
     */
    void publish(Event event);

    /** Close dispatcher and stop event redistributing. */
    void shutdown();
}
