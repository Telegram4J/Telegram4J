package telegram4j.mtproto.client;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.tl.Updates;

/** Dispatcher for redistributing {@link Updates} from Telegram API and event manager. */
public interface UpdateDispatcher {

    /** {@return A {@code Flux} view of dispatcher} */
    Flux<Updates> all();

    /**
     * Gets {@code Flux} of updates with specified type.
     *
     * @param <T> The type of updates to listen.
     * @param type The type of required updates.
     * @return A {@code Flux} emitting only updates of specified subtype.
     */
    default <T extends Updates> Flux<T> on(Class<T> type) {
        return all()
                .ofType(type);
    }

    /**
     * Publishes updates for all subscribers.
     *
     * @param updates The updates to emit.
     */
    void publish(Updates updates);

    /**
     * Closes underlying resources.
     *
     * @return A {@code Mono} emitting empty signals
     * or errors on completion.
     */
    Mono<Void> close();
}
