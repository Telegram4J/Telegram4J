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

    public boolean isCustom() {
        return emoticon == null;
    }

    public Optional<Long> getDocumentId() {
        return isCustom() ? Optional.of(documentId) : Optional.empty();
    }

    public Optional<String> getEmoticon() {
        return Optional.ofNullable(emoticon);
    }
}
