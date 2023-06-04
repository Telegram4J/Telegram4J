package telegram4j.mtproto.client.impl;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.ScheduledFuture;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class Trigger {
    private final Runnable action;
    private final EventExecutor eventExecutor;
    private final long nanos;

    private ScheduledFuture<?> future;

    private Trigger(Runnable action, EventExecutor eventExecutor, Duration period) {
        this.action = action;
        this.eventExecutor = eventExecutor;
        this.nanos = period.toNanos();
    }

    public static Trigger create(Runnable action, EventExecutor eventExecutor, Duration period) {
        Trigger trigger = new Trigger(action, eventExecutor, period);
        trigger.restart();
        return trigger;
    }

    public void restart() {
        future = eventExecutor.scheduleWithFixedDelay(action, nanos, nanos, TimeUnit.NANOSECONDS);
    }

    public void cancel() {
        future.cancel(false);
    }
}
