package telegram4j.core.object;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Representation of premium user status with custom emoji. */
public final class EmojiStatus implements TelegramObject {
    private final MTProtoTelegramClient client;
    private final long emojiId;
    @Nullable
    private final Instant untilTimestamp;

    public EmojiStatus(MTProtoTelegramClient client, long emojiId, @Nullable Instant untilTimestamp) {
        this.client = Objects.requireNonNull(client);
        this.emojiId = emojiId;
        this.untilTimestamp = untilTimestamp;
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Requests to retrieve {@link Sticker custom emoji} represented by this status.
     *
     * @return A {@link Mono} emitting on successful completion {@link Sticker custom emoji}.
     */
    public Mono<Sticker> getCustomEmoji() {
        return client.getCustomEmoji(emojiId);
    }

    /**
     * Gets id of {@link Sticker custom emoji}.
     *
     * @return id of {@link Sticker custom emoji}.
     */
    public long getEmojiId() {
        return emojiId;
    }

    /**
     * Requests to retrieve {@link Sticker custom emoji} by {@link #getEmojiId()}.
     *
     * @return A {@link Mono} emitting on successful completion {@link Sticker custom emoji}.
     */
    public Mono<Sticker> getEmoji() {
        return client.getCustomEmoji(emojiId);
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
                "emojiId=" + emojiId +
                ", untilTimestamp=" + untilTimestamp +
                '}';
    }
}
