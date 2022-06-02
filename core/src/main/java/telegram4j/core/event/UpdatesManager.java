package telegram4j.core.event;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.Event;
import telegram4j.tl.Updates;

public interface UpdatesManager {

    /**
     * Starts manager with enabling get difference scheduling
     * or other support services.
     *
     * @return A {@link Mono} emitting nothing.
     */
    Mono<Void> start();

    /**
     * Requests to check current update state and
     * get difference on detected gap.
     *
     * @return A {@link Mono} emitting nothing.
     */
    Mono<Void> fillGap();

    /**
     * Convert and checks received {@link Updates} to {@link Event} objects.
     *
     * @param updates The new updates box.
     * @return A {@link Flux} emitting mapped {@link Event events}.
     */
    Flux<Event> handle(Updates updates);

    /** Closes underling services/schedules. */
    void shutdown();
}
