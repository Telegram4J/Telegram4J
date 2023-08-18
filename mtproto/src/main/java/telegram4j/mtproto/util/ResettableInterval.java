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
package telegram4j.mtproto.util;

import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.util.concurrent.Queues;

import java.time.Duration;
import java.util.Objects;

public final class ResettableInterval implements Disposable {
    private final Scheduler timerScheduler;
    private final Disposable.Swap swap = Disposables.swap();
    private final Sinks.Many<Long> sink;

    public ResettableInterval(Scheduler timerScheduler) {
        this(timerScheduler, Sinks.many().multicast()
                .onBackpressureBuffer(Queues.XS_BUFFER_SIZE, false));
    }

    public ResettableInterval(Scheduler timerScheduler, Sinks.Many<Long> sink) {
        this.timerScheduler = Objects.requireNonNull(timerScheduler);
        this.sink = Objects.requireNonNull(sink);
    }

    public void start(Duration period) {
        start(Duration.ZERO, period);
    }

    public void start(Duration delay, Duration period) {
        swap.update(Flux.interval(delay, period, timerScheduler)
                .subscribe(tick -> sink.emitNext(tick, Sinks.EmitFailureHandler.FAIL_FAST),
                        e -> sink.emitError(e, Sinks.EmitFailureHandler.FAIL_FAST)));
    }

    public Flux<Long> ticks() {
        return sink.asFlux();
    }

    public void close() {
        swap.dispose();
        sink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
    }

    @Override
    public void dispose() {
        swap.dispose();
    }

    @Override
    public boolean isDisposed() {
        return swap.isDisposed();
    }
}
