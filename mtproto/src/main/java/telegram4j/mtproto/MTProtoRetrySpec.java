package telegram4j.mtproto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * A {@link Retry} strategy which triggers on {@link RpcException}
 * with code 420 and message starting with FLOOD_WAIT_ to delay they retry signals.
 */
public class MTProtoRetrySpec extends Retry {
    private static final Retry instance = new MTProtoRetrySpec();

    private MTProtoRetrySpec() {}

    /**
     * Gets common instance of retry strategy.
     *
     * @return The common instance of mtproto retry strategy.
     */
    public static Retry instance() {
        return instance;
    }

    @Override
    public Flux<Long> generateCompanion(Flux<RetrySignal> retrySignals) {
        return retrySignals.concatMap(retryWhenState -> {
            RetrySignal copy = retryWhenState.copy();
            Throwable currentFailure = copy.failure();

            if (currentFailure == null) {
                return Mono.error(new IllegalStateException("Retry.RetrySignal#failure() not expected to be null"));
            }

            RpcException rpcExc;
            if (currentFailure instanceof RpcException &&
                    (rpcExc = (RpcException) currentFailure).getError().errorCode() == 420 &&
                    rpcExc.getError().errorMessage().startsWith("FLOOD_WAIT_")) {
                String arg = rpcExc.getError().errorMessage().substring(
                        rpcExc.getError().errorMessage().lastIndexOf('_') + 1);
                Duration delay = Duration.ofSeconds(Long.parseLong(arg));
                return Mono.delay(delay);
            }
            return Mono.error(currentFailure);
        });
    }
}
