package telegram4j.mtproto.util;

import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.util.concurrent.Queues;

import java.time.Duration;

public final class ResettableInterval implements Disposable {
    private final Scheduler timerScheduler;
    private final Disposable.Swap swap = Disposables.swap();
    private final Sinks.Many<Long> ticks = Sinks.many().multicast()
            .onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);

    public ResettableInterval(Scheduler timerScheduler) {
        this.timerScheduler = timerScheduler;
    }

    public void start(Duration period) {
        start(Duration.ZERO, period);
    }

    public void start(Duration delay, Duration period) {
        swap.update(Flux.interval(delay, period, timerScheduler)
                .subscribe(tick -> ticks.emitNext(tick, Sinks.EmitFailureHandler.FAIL_FAST)));
    }

    public Flux<Long> ticks() {
        return ticks.asFlux();
    }

    @Override
    public void dispose() {
        Disposable d = swap.get();
        if (d != null) {
            d.dispose();
        }
    }

    @Override
    public boolean isDisposed() {
        Disposable d = swap.get();
        return d != null && d.isDisposed();
    }
}
