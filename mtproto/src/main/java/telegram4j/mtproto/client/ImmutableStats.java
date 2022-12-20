package telegram4j.mtproto.client;

import reactor.util.annotation.Nullable;

import java.time.Instant;
import java.util.Optional;

/** Immutable implementation of {@code MTProtoClient.Stats}. */
public class ImmutableStats implements MTProtoClient.Stats {
    private final Instant lastQueryTimestamp;
    private final int queriesCount;

    public ImmutableStats(@Nullable Instant lastQueryTimestamp, int queriesCount) {
        this.lastQueryTimestamp = lastQueryTimestamp;
        this.queriesCount = queriesCount;
    }

    @Override
    public Optional<Instant> getLastQueryTimestamp() {
        return Optional.ofNullable(lastQueryTimestamp);
    }

    @Override
    public int getQueriesCount() {
        return queriesCount;
    }

    @Override
    public String toString() {
        return "ImmutableStats{" +
                "lastQueryTimestamp=" + lastQueryTimestamp +
                ", queriesCount=" + queriesCount +
                '}';
    }
}
