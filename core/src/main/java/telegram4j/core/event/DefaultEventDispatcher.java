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
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import telegram4j.core.event.domain.Event;

import java.util.Objects;

/** Default event dispatcher implementation based on {@link Sinks.Many} processor. */
public class DefaultEventDispatcher implements EventDispatcher {

    protected final Scheduler scheduler;
    protected final boolean disposeScheduler;
    protected final Sinks.Many<Event> sink;
    protected final Sinks.EmitFailureHandler emissionHandler;

    public DefaultEventDispatcher(Scheduler scheduler, boolean disposeScheduler,
                                  Sinks.Many<Event> sink, Sinks.EmitFailureHandler emissionHandler) {
        this.scheduler = Objects.requireNonNull(scheduler);
        this.disposeScheduler = disposeScheduler;
        this.sink = Objects.requireNonNull(sink);
        this.emissionHandler = Objects.requireNonNull(emissionHandler);
    }

    @Override
    public Flux<Event> all() {
        return sink.asFlux()
                .publishOn(scheduler);
    }

    @Override
    public void publish(Event event) {
        if (log.isTraceEnabled()) {
            log.trace(event.toString());
        }

        sink.emitNext(event, emissionHandler);
    }

    @Override
    public Mono<Void> close() {
        return Mono.defer(() -> {
            sink.emitComplete(emissionHandler);

            if (disposeScheduler) {
                return scheduler.disposeGracefully();
            }
            return Mono.empty();
        });
    }
}
