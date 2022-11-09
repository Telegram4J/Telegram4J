package telegram4j.mtproto;

import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import telegram4j.tl.mtproto.RpcError;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A {@link Retry} strategy which triggers on {@link RpcException}
 * with code 420 and message starting with FLOOD_WAIT_ to delay they retry signals.
 */
public class MTProtoRetrySpec extends Retry {

    private final Predicate<Duration> predicate;
    private final long maxAttempts;
    private final boolean inRow;

    private MTProtoRetrySpec(Predicate<Duration> predicate, long maxAttempts, boolean inRow) {
        this.predicate = Objects.requireNonNull(predicate);
        this.maxAttempts = maxAttempts;
        this.inRow = inRow;
    }

    /**
     * Creates a new retry strategy for flood wait errors.
     *
     * @param canRetry The predicate for flood wait delay. Resend with delay will be triggered if
     * predicate returns {@code true} for specified duration.
     * @param maxAttempts The maximum number of retry attempts.
     * @param inRow The maximum number of retry attempts to allow in a row, reset by successful onNext().
     * @return A new {@code MTProtoRetrySpec}.
     */
    public static MTProtoRetrySpec create(Predicate<Duration> canRetry,
                                          long maxAttempts, boolean inRow) {
        return new MTProtoRetrySpec(canRetry, maxAttempts, inRow);
    }

    public static MTProtoRetrySpec max(Predicate<Duration> canRetry, long maxAttempts) {
        return create(canRetry, maxAttempts, false);
    }

    public static MTProtoRetrySpec maxInRow(Predicate<Duration> canRetry, long maxAttempts) {
        return create(canRetry, maxAttempts, true);
    }


    public MTProtoRetrySpec withPredicate(Predicate<Duration> predicate) {
        if (predicate == this.predicate) return this;
        return new MTProtoRetrySpec(predicate, maxAttempts, inRow);
    }

    public MTProtoRetrySpec withMaxAttempts(long maxAttempts) {
        if (maxAttempts == this.maxAttempts) return this;
        return new MTProtoRetrySpec(predicate, maxAttempts, inRow);
    }

    public MTProtoRetrySpec withInRow(boolean inRow) {
        if (inRow == this.inRow) return this;
        return new MTProtoRetrySpec(predicate, maxAttempts, inRow);
    }

    @Override
    public Flux<Long> generateCompanion(Flux<RetrySignal> retrySignals) {
        return retrySignals.concatMap(retryWhenState -> {
            RetrySignal copy = retryWhenState.copy();
            Throwable currentFailure = copy.failure();
            long attempts = inRow ? copy.totalRetriesInARow() : copy.totalRetries();

            if (currentFailure == null) {
                return Mono.error(new IllegalStateException("Retry.RetrySignal#failure() not expected to be null"));
            }

            if (attempts >= maxAttempts) {
                return Mono.error(Exceptions.retryExhausted("Retries exhausted: " + (inRow
                        ? copy.totalRetriesInARow() + "/" + maxAttempts + " in a row (" + copy.totalRetries() + " total)"
                        : copy.totalRetries() + "/" + maxAttempts),
                        copy.failure()));
            }

            if (currentFailure instanceof RpcException) {
                var exc = (RpcException) currentFailure;
                if (exc.getError().errorCode() == 420 &&
                    exc.getError().errorMessage().startsWith("FLOOD_WAIT_")) {
                    Duration delay = parse(exc.getError());
                    if (predicate.test(delay)) {
                        return Mono.delay(delay);
                    }
                }
            }
            return Mono.error(currentFailure);
        });
    }

    static Duration parse(RpcError error) {
        String arg = error.errorMessage().substring(
                error.errorMessage().lastIndexOf('_') + 1);
        return Duration.ofSeconds(Integer.parseInt(arg));
    }
}
