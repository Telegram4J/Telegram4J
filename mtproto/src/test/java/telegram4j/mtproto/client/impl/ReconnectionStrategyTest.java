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
