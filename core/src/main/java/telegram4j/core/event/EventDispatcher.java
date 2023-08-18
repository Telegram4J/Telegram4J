/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package telegram4j.core.event;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.core.event.domain.Event;

/**
 * Distributes incoming {@link Event}s to a subscribers.
 * To retrieves {@link Flux} of {@link Event}s uses the {@link #on(Class)}
 * method with the event type in a parameters.
 * And to publish {@link #publish(Event)}
 */
public interface EventDispatcher {
    Logger log = Loggers.getLogger(EventDispatcher.class);

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
    default <E extends Event> Flux<E> on(Class<E> type) {
        return all().ofType(type);
    }

    Flux<Event> all();

    default Flux<Event> on(EventAdapter adapter) {
        return all()
                .flatMap(event -> Flux.defer(() -> adapter.hookOnEvent(event))
                        .onErrorResume(t -> {
                            log.error("Error while handling {} on {}",
                                    event.getClass().getSimpleName(), adapter.getClass().getCanonicalName());
                            return Mono.empty();
                        })
                        .then(Mono.just(event)));
    }

    /**
     * Publishes an {@link Event} to the dispatcher.
     *
     * @param event the {@link Event} instance to publish.
     */
    void publish(Event event);

    /** Close dispatcher and stop event redistributing. */
    Mono<Void> close();
}
