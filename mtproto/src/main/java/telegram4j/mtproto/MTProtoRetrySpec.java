/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package telegram4j.mtproto;

import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
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

    private final Scheduler delayScheduler;
    private final Predicate<Duration> predicate;
    private final long maxAttempts;
    private final boolean inRow;

    private MTProtoRetrySpec(Scheduler delayScheduler, Predicate<Duration> predicate, long maxAttempts, boolean inRow) {
        this.delayScheduler = Objects.requireNonNull(delayScheduler);
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
    public static MTProtoRetrySpec create(Scheduler delayScheduler, Predicate<Duration> canRetry,
                                          long maxAttempts, boolean inRow) {
        return new MTProtoRetrySpec(delayScheduler, canRetry, maxAttempts, inRow);
    }

    public static MTProtoRetrySpec max(Predicate<Duration> canRetry, long maxAttempts) {
        return max(Schedulers.single(), canRetry, maxAttempts);
    }

    public static MTProtoRetrySpec max(Scheduler delayScheduler, Predicate<Duration> canRetry, long maxAttempts) {
        return create(delayScheduler, canRetry, maxAttempts, false);
    }

    public static MTProtoRetrySpec maxInRow(Predicate<Duration> canRetry, long maxAttempts) {
        return maxInRow(Schedulers.single(), canRetry, maxAttempts);
    }

    public static MTProtoRetrySpec maxInRow(Scheduler delayScheduler, Predicate<Duration> canRetry, long maxAttempts) {
        return create(delayScheduler, canRetry, maxAttempts, true);
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

            if (currentFailure instanceof RpcException exc) {
                if (exc.getError().errorCode() == 420 &&
                    exc.getError().errorMessage().startsWith("FLOOD_WAIT_")) {
                    Duration delay = parse(exc.getError());
                    if (predicate.test(delay)) {
                        return Mono.delay(delay, delayScheduler);
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
