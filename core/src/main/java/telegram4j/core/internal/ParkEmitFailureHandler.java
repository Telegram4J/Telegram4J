package telegram4j.core.internal;

import reactor.core.publisher.SignalType;
import reactor.core.publisher.Sinks;

import java.util.concurrent.locks.LockSupport;

public class ParkEmitFailureHandler implements Sinks.EmitFailureHandler {
    private final long delay;

    public ParkEmitFailureHandler(long delay) {
        this.delay = delay;
    }

    @Override
    public boolean onEmitFailure(SignalType signalType, Sinks.EmitResult emitResult) {
        if (emitResult == Sinks.EmitResult.FAIL_NON_SERIALIZED) {
            LockSupport.parkNanos(delay);
            return true;
        }
        return false;
    }
}
