package telegram4j.mtproto.client.impl;

import telegram4j.mtproto.client.ImmutableStats;
import telegram4j.mtproto.client.MTProtoClient;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.time.Instant;
import java.util.Optional;

final class ConcurrentStats implements MTProtoClient.Stats {
    static final VarHandle QUERIES_COUNT;

    static {
        try {
            var l = MethodHandles.lookup();
            QUERIES_COUNT = l.findVarHandle(ConcurrentStats.class, "queriesCount", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    volatile Instant lastQueryTimestamp;
    volatile int queriesCount;

    void addQueriesCount(int amount) {
        QUERIES_COUNT.getAndAdd(this, amount);
    }

    void incrementQueriesCount() {
        QUERIES_COUNT.getAndAdd(this, 1);
    }

    void decrementQueriesCount() {
        QUERIES_COUNT.getAndAdd(this, -1);
    }

    @Override
    public Optional<Instant> lastQueryTimestamp() {
        return Optional.ofNullable(lastQueryTimestamp);
    }

    @Override
    public int queriesCount() {
        return queriesCount;
    }

    @Override
    public MTProtoClient.Stats copy() {
        return new ImmutableStats(lastQueryTimestamp, queriesCount);
    }

    @Override
    public String toString() {
        return "Stats{" +
                "lastQueryTimestamp=" + lastQueryTimestamp +
                ", queriesCount=" + queriesCount +
                '}';
    }
}
