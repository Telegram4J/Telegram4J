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

import java.time.Duration;

import static telegram4j.mtproto.internal.Preconditions.requireArgument;

/**
 * Implementation of {@code ReconnectionStrategy} that
 * has two patterns of behavior.
 * <p>
 * The first one is optimistic, until {@code noBackoffAttempts} threshold
 * client will reconnect immediately. Thereafter, used geometric progression
 * with ration value 2 and limited series by {@code maxIncrease}.
 */
public class DefaultReconnectionStrategy implements ReconnectionStrategy {
    private final int noBackoffAttempts;
    private final int maxIncrease;
    private final Duration baseBackoff;

    private DefaultReconnectionStrategy(int noBackoffAttempts, int maxIncrease, Duration baseBackoff) {
        this.noBackoffAttempts = noBackoffAttempts;
        this.maxIncrease = maxIncrease;
        this.baseBackoff = baseBackoff;
    }

    /**
     * Creates new {@code DefaultReconnectionStrategy} with specified parameters.
     *
     * @throws IllegalArgumentException if {@code noBackoffAttempts} is less than 0 or {@code maxIncrease} is negative,
     * and or {@code baseBackoff} is negative.
     * @param noBackoffAttempts The count of attempts that will be processed without backoff.
     * Pass {@code 0} to disable functionality.
     * @param maxIncrease The max increase of base backoff. For details see docs of class.
     * @param baseBackoff The base backoff.
     * @return A new {@code DefaultReconnectionStrategy}.
     */
    public static DefaultReconnectionStrategy create(int noBackoffAttempts, int maxIncrease, Duration baseBackoff) {
        requireArgument(noBackoffAttempts >= 0, "noBackoffAttempts must be positive or zero");
        requireArgument(maxIncrease > 0, "maxIncrease must be positive");
        requireArgument(!baseBackoff.isNegative(), "baseBackoff must be positive or zero");

        return new DefaultReconnectionStrategy(noBackoffAttempts, maxIncrease, baseBackoff);
    }

    @Nullable
    @Override
    public Duration computeBackoff(ReconnectionContext ctx) {
        int iter = ctx.iteration();
        if (iter <= noBackoffAttempts) {
            return Duration.ZERO;
        }

        int i = Math.min(iter - noBackoffAttempts, maxIncrease);
        int n = 1 << i - 1;
        return baseBackoff.multipliedBy(n);
    }
}
