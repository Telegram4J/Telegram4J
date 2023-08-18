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
