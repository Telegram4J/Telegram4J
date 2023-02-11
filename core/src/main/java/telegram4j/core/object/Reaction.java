package telegram4j.core.object;

import reactor.util.annotation.Nullable;

import java.util.Objects;
import java.util.Optional;

public final class Reaction {
    private final long emojiId;
    @Nullable
    private final String emoticon;

    public Reaction(long emojiId) {
        this.emojiId = emojiId;
        this.emoticon = null;
    }

    public Reaction(String emoticon) {
        this.emoticon = Objects.requireNonNull(emoticon);
        this.emojiId = 0;
    }

    /**
     * Gets id of custom emoji of this reaction, if its custom emoji.
     *
     * @return The id of custom emoji of this reaction, if its custom emoji.
     */
    public Optional<Long> getEmojiId() {
        return emoticon == null ? Optional.of(emojiId) : Optional.empty();
    }

    /**
     * Gets emoticon of this reaction, if it's not custom emoji.
     *
     * @return The emoticon of this reaction, if it's not custom emoji.
     */
    public Optional<String> getEmoticon() {
        return Optional.ofNullable(emoticon);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof Reaction r)) return false;
        return emojiId == r.emojiId && Objects.equals(emoticon, r.emoticon);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(emojiId) + Objects.hashCode(emoticon);
    }

    @Override
    public String toString() {
        return "Reaction{" + (emoticon != null ? "emoticon='" + emoticon + '\'' : "emojiId=" + emojiId) + '}';
    }
}
