package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import telegram4j.core.event.domain.Event;
import telegram4j.tl.Update;

/** Interface for mapping incoming {@link telegram4j.tl.Update updates} into the {@link Event events}. */
public interface UpdatesMapper {

    /**
     * Map {@link Update} to possibly multiple {@link Event} objects.
     *
     * @param <U> The type of update to mapping.
     * @param context The update context with precomputed hash maps with chat/user entities.
     * @return A {@link Flux} emitting one or more mapped {@link Event events} objects.
     */
    <U extends Update> Flux<Event> handle(UpdateContext<U> context);
}
