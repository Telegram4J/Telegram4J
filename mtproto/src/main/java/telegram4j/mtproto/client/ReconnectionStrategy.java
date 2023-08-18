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
package telegram4j.mtproto.client;

import reactor.util.annotation.Nullable;
import telegram4j.mtproto.internal.Preconditions;

import java.time.Duration;

/** Interface for controlling client reconnection behavior. */
public interface ReconnectionStrategy {

    /**
     * Creates new {@code ReconnectionStrategy} that always
     * allows reconnection with no backoff.
     *
     * @return A new {@code ReconnectionStrategy}.
     */
    static ReconnectionStrategy immediately() {
        return ctx -> Duration.ZERO;
    }

    /**
     * Creates new simple {@code ReconnectionStrategy} that always
     * retries reconnection with fixed backoff interval.
     *
     * @param interval The backoff value.
     * @throws IllegalArgumentException if {@code interval} is negative.
     * @return A new {@code ReconnectionStrategy}.
     */
    static ReconnectionStrategy fixedInterval(Duration interval) {
        return fixedInterval(interval, Integer.MAX_VALUE);
    }

    /**
     * Creates new simple {@code ReconnectionStrategy} that
     * retries reconnection with fixed backoff interval.
     *
     * @param interval The backoff value.
     * @param maxAttempts The number of max attempts. If exceeded, the client will be closed.
     * @throws IllegalArgumentException if {@code interval} or {@code maxAttempts} is negative.
     * @return A new {@code ReconnectionStrategy}.
     */
    static ReconnectionStrategy fixedInterval(Duration interval, int maxAttempts) {
        Preconditions.requireArgument(!interval.isNegative(), "Interval must be positive");
        Preconditions.requireArgument(maxAttempts > 0, "maxAttempts must be positive");

        return ctx -> {
            if (ctx.iteration() == maxAttempts) {
                return null;
            }
            return interval;
        };
    }

    /**
     * Computes backoff value for client reconnection.
     *
     * @param ctx The context of attempt.
     * @return The new backoff time or {@code null} for preventing reconnection.
     */
    @Nullable
    Duration computeBackoff(ReconnectionContext ctx);
}
