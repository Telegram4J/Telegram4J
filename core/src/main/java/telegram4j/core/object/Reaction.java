package telegram4j.core.object;

import reactor.util.annotation.Nullable;

import java.util.Objects;
import java.util.Optional;

public class Reaction {
    private final long documentId;
    @Nullable
    private final String emoticon;

    public Reaction(long documentId) {
        this.documentId = documentId;
        this.emoticon = null;
    }

    public Reaction(String emoticon) {
        this.emoticon = Objects.requireNonNull(emoticon);
        this.documentId = -1;
    }

    /**
     * Gets id of custom emoji of this reaction, if its custom emoji.
     *
     * @return The id of custom emoji of this reaction, if its custom emoji.
     */
    public Optional<Long> getDocumentId() {
        return emoticon == null ? Optional.of(documentId) : Optional.empty();
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
        if (o == null || getClass() != o.getClass()) return false;
        Reaction reaction = (Reaction) o;
        return documentId == reaction.documentId && Objects.equals(emoticon, reaction.emoticon);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(documentId) + Objects.hashCode(emoticon);
    }

    @Override
    public String toString() {
        return "Reaction{" + (emoticon != null ? "emoticon='" + emoticon + '\'' : "documentId=" + documentId) + '}';
    }
}
