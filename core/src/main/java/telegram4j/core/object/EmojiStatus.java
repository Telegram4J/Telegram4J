package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.time.Instant;
import java.util.Optional;

/** Representation of premium user status with custom emoji. */
public class EmojiStatus {
    private final long documentId;
    @Nullable
    private final Instant untilTimestamp;

    public EmojiStatus(long documentId, @Nullable Instant untilTimestamp) {
        this.documentId = documentId;
        this.untilTimestamp = untilTimestamp;
    }

    /**
     * Gets id of {@link Sticker custom emoji}.
     *
     * @see MTProtoTelegramClient#getCustomEmoji(long)
     * @return id of {@link Sticker custom emoji}.
     */
    public long getDocumentId() {
        return documentId;
    }

    /**
     * Gets timestamp until which this status is valid, if present, otherwise will be available forever.
     *
     * @return The timestamp until which this status is valid, if present, otherwise will be available forever.
     */
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
