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
                .subscribe(tick -> sink.emitNext(tick, Sinks.EmitFailureHandler.FAIL_FAST)));
    }

    public Flux<Long> ticks() {
        return sink.asFlux();
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
