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
    Mono<Void> close();
}
