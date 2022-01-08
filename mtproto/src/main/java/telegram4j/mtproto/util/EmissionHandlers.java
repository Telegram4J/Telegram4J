package telegram4j.mtproto.util;

import reactor.core.publisher.SignalType;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.concurrent.locks.LockSupport;

public final class EmissionHandlers {

    public static final Sinks.EmitFailureHandler DEFAULT_PARKING = park(Duration.ofNanos(10));

    public static Sinks.EmitFailureHandler park(Duration delay) {
        return new ParkEmissionHandler(delay.toNanos());
    }

    static class ParkEmissionHandler implements Sinks.EmitFailureHandler {
        final long nanos;

        ParkEmissionHandler(long nanos) {
            this.nanos = nanos;
        }

        @Override
        public boolean onEmitFailure(SignalType signalType, Sinks.EmitResult emitResult) {
            switch (signalType) {
                case ON_NEXT:
                    switch (emitResult) {
                        case FAIL_ZERO_SUBSCRIBER:
                        case FAIL_CANCELLED:
                        case FAIL_TERMINATED:
                            return false;
                        case FAIL_NON_SERIALIZED:
                            return true;
                        case FAIL_OVERFLOW:
                            LockSupport.parkNanos(nanos);
                            return true;
                        default:
                            throw new Sinks.EmissionException(emitResult, "Unknown emitResult value");
                    }

                case ON_COMPLETE:
                case ON_ERROR:
                    switch (emitResult) {
                        case FAIL_ZERO_SUBSCRIBER:
                        case FAIL_CANCELLED:
                        case FAIL_TERMINATED:
                        case FAIL_OVERFLOW:
                            return false;
                        case FAIL_NON_SERIALIZED:
                            LockSupport.parkNanos(nanos);
                            return true;
                        default:
                            throw new Sinks.EmissionException(emitResult, "Unknown emitResult value");
                    }

                default:
                    throw new IllegalStateException("Unknown signal type: " + signalType);
            }
        }
    }
}
