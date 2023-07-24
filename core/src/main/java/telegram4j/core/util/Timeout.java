package telegram4j.core.util;

import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.util.Objects;

/** A resettable schedule task. */
public class Timeout implements Disposable {
    private final Disposable.Swap delaySubscription = Disposables.swap();

    private final Scheduler timerScheduler;
    private final Sinks.Many<Long> sink;

    Timeout(Scheduler timerScheduler, Sinks.Many<Long> sink) {
        this.timerScheduler = timerScheduler;
        this.sink = sink;
    }

    /**
     * Creates new {@code Timeout} with specified scheduler and sink.
     *
     * @apiNote Preferably pass newly created {@code sink}, because timeout may close it.
     * @param timerScheduler The scheduler for tasks.
     * @param sink The sink for triggering tasks.
     * @return A new {@code Timeout}.
     */
    public static Timeout create(Scheduler timerScheduler, Sinks.Many<Long> sink) {
        Objects.requireNonNull(timerScheduler);
        Objects.requireNonNull(sink);
        return new Timeout(timerScheduler, sink);
    }

    /**
     * Resets timeout and sets new one with specified delay.
     *
     * @param delay The delay of task.
     */
    public void restart(Duration delay) {
        delaySubscription.update(Mono.delay(delay, timerScheduler)
                .subscribe(tick -> sink.emitNext(tick, Sinks.EmitFailureHandler.FAIL_FAST),
                        e -> sink.emitError(e, Sinks.EmitFailureHandler.FAIL_FAST)));
    }

    /**
     * {@return A {@code Flux} through which will be tasks triggered}
     * Note, any signals on that flux published on {@code timerScheduler}.
     */
    public Flux<Long> asFlux() {
        return sink.asFlux();
    }

    /**
     * Cancels current schedule and closes underlying sink.
     * After this operation timeout becomes unavailable.
     */
    public void close() {
        delaySubscription.dispose();
        sink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
    }

    /** Cancels current schedule without sink close. */
    @Override
    public void dispose() {
        delaySubscription.dispose();
    }

    /** {@return Whether current schedule is cancelled} */
    @Override
    public boolean isDisposed() {
        return delaySubscription.isDisposed();
    }
}
