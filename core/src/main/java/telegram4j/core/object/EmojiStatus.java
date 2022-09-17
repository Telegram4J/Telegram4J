package telegram4j.core.object;

import reactor.util.annotation.Nullable;

import java.time.Instant;
import java.util.Optional;

public class EmojiStatus {
    private final long documentId;
    @Nullable
    private final Instant untilTimestamp;

    public EmojiStatus(long documentId, @Nullable Instant untilTimestamp) {
        this.documentId = documentId;
        this.untilTimestamp = untilTimestamp;
    }

    public long getDocumentId() {
        return documentId;
    }

    public Optional<Instant> getUntilTimestamp() {
        return Optional.ofNullable(untilTimestamp);
    }

    @Override
    public String toString() {
        return "EmojiStatus{" +
                "documentId=" + documentId +
                ", untilTimestamp=" + untilTimestamp +
                '}';
    }
}
