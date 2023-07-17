package telegram4j.mtproto.client.impl;

import reactor.util.annotation.Nullable;
import telegram4j.mtproto.client.ReconnectionContext;

import java.time.Duration;
import java.util.Optional;

final class ReconnectionContextImpl implements ReconnectionContext {
    private int iteration;
    @Nullable
    private Duration lastBackoff;
    private Throwable exception;
    private boolean resume = true;

    public void resetAfterConnect() {
        iteration = 0;
        exception = null;
        lastBackoff = null;
    }

    public void reset() {
        resetAfterConnect();
        resume = false;
    }

    public void increment() {
        iteration = Math.incrementExact(iteration);
    }

    public void setException(@Nullable Throwable exception) {
        this.exception = exception;
    }

    public void setResume(boolean resume) {
        this.resume = resume;
    }

    public void setLastBackoff(Duration lastBackoff) {
        this.lastBackoff = lastBackoff;
    }

    public boolean isResume() {
        return resume;
    }

    @Nullable
    public Throwable cause() {
        return exception;
    }

    @Override
    public int iteration() {
        return iteration;
    }

    @Override
    public Optional<Duration> lastBackoff() {
        return Optional.ofNullable(lastBackoff);
    }

    @Override
    public Optional<Throwable> exception() {
        return Optional.ofNullable(exception);
    }

    @Override
    public String toString() {
        return "IterationContext{" +
                "iteration=" + iteration +
                ", lastBackoff=" + lastBackoff +
                ", exception=" + exception +
                '}';
    }
}
