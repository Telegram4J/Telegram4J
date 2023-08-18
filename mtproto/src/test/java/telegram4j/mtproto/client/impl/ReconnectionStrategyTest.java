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
package telegram4j.mtproto.client.impl;

import org.junit.jupiter.api.Test;
import telegram4j.mtproto.client.DefaultReconnectionStrategy;
import telegram4j.mtproto.client.ReconnectionStrategy;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReconnectionStrategyTest {
    @Test
    void testDefaultImpl() {
        final Duration base = Duration.ofSeconds(1);
        final int maxIncrease = 5;
        final int noBackoffAttempts = 3;

        var ctx = new ReconnectionContextImpl();
        var impl = DefaultReconnectionStrategy.create(noBackoffAttempts, maxIncrease, base);

        for (int i = 0; i < noBackoffAttempts; i++) {
            assertEquals(Duration.ZERO, next(ctx, impl));
        }

        for (int i = 0; i < maxIncrease; i++) {
            int act = ctx.iteration() - noBackoffAttempts;
            Duration expected = base.multipliedBy(1L << act);
            assertEquals(expected, next(ctx, impl), ctx::toString);
        }
        assertEquals(ctx.lastBackoff().orElseThrow(), next(ctx, impl), ctx::toString);
    }

    Duration next(ReconnectionContextImpl ctx, ReconnectionStrategy strategy) {
        ctx.increment();
        ctx.setException(null);

        Duration backoff = strategy.computeBackoff(ctx);
        if (backoff != null) {
            ctx.setLastBackoff(backoff);
        }

        return backoff;
    }
}
