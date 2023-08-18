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
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import telegram4j.tl.Updates;

import java.util.Objects;

public class SinksUpdateDispatcher implements UpdateDispatcher {
    protected final Scheduler scheduler;
    protected final boolean disposeScheduler;
    protected final Sinks.Many<Updates> sink;
    protected final Sinks.EmitFailureHandler emitFailureHandler;

    public SinksUpdateDispatcher(Scheduler scheduler, boolean disposeScheduler,
                                 Sinks.Many<Updates> sink,
                                 Sinks.EmitFailureHandler emitFailureHandler) {
        this.scheduler = Objects.requireNonNull(scheduler);
        this.disposeScheduler = disposeScheduler;
        this.sink = Objects.requireNonNull(sink);
        this.emitFailureHandler = Objects.requireNonNull(emitFailureHandler);
    }

    @Override
    public Flux<Updates> all() {
        return sink.asFlux()
                .publishOn(scheduler);
    }

    @Override
    public void publish(Updates updates) {
        sink.emitNext(updates, emitFailureHandler);
    }

    @Override
    public Mono<Void> close() {
        return Mono.defer(() -> {
            sink.emitComplete(emitFailureHandler);

            if (disposeScheduler) {
                return scheduler.disposeGracefully();
            }
            return Mono.empty();
        });
    }
}
