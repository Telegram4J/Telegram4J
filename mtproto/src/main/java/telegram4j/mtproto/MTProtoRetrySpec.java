package telegram4j.mtproto;

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
    private static final MTProtoRetrySpec instance = new MTProtoRetrySpec(d -> d.compareTo(Duration.ofSeconds(5)) <= 0);

    private final Predicate<Duration> canRetry;

    private MTProtoRetrySpec(Predicate<Duration> canRetry) {
        this.canRetry = Objects.requireNonNull(canRetry);
    }

    /**
     * Gets common instance of retry strategy which retries sequence
     * if flood wait delay is less than 5 minutes.
     *
     * @return The common instance of mtproto retry strategy.
     */
    public static MTProtoRetrySpec instance() {
        return instance;
    }

    /**
     * Creates a new retry strategy for flood wait errors.
     *
     * @param canRetry The predicate for flood wait delay. Resend with delay will be triggered if
     * predicate returns {@code true} for specified duration.
     * @return A new {@code MTProtoRetrySpec}.
     */
    public static MTProtoRetrySpec create(Predicate<Duration> canRetry) {
        return new MTProtoRetrySpec(canRetry);
    }

    @Override
    public Flux<Long> generateCompanion(Flux<RetrySignal> retrySignals) {
        return retrySignals.concatMap(retryWhenState -> {
            RetrySignal copy = retryWhenState.copy();
            Throwable currentFailure = copy.failure();

            if (currentFailure == null) {
                return Mono.error(new IllegalStateException("Retry.RetrySignal#failure() not expected to be null"));
            }

            if (currentFailure instanceof RpcException) {
                var exc = (RpcException) currentFailure;
                if (exc.getError().errorMessage().startsWith("FLOOD_WAIT_") &&
                    exc.getError().errorCode() == 420) {
                    Duration delay = parse(exc.getError());
                    if (canRetry.test(delay)) {
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
