package telegram4j.core.spec.media;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.InputDocument;
import telegram4j.tl.InputMedia;
import telegram4j.tl.InputMediaDocument;
import telegram4j.tl.InputMediaDocumentExternal;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class InputMediaDocumentSpec implements InputMediaSpec {
    private final String document;
    private final String query;
    private final Duration autoDeleteDuration;

    private InputMediaDocumentSpec(String document) {
        this.document = Objects.requireNonNull(document);
        this.query = null;
        this.autoDeleteDuration = null;
    }

    private InputMediaDocumentSpec(String document, @Nullable String query, @Nullable Duration autoDeleteDuration) {
        this.document = document;
        this.query = query;
        this.autoDeleteDuration = autoDeleteDuration;
    }

    public String document() {
        return document;
    }

    public Optional<String> query() {
        return Optional.ofNullable(query);
    }

    public Optional<Duration> autoDeleteDuration() {
        return Optional.ofNullable(autoDeleteDuration);
    }

    @Override
    public Mono<InputMedia> asData(MTProtoTelegramClient client) {
        return Mono.fromCallable(() -> {
            Integer ttlSeconds = autoDeleteDuration()
                    .map(Duration::getSeconds)
                    .map(Math::toIntExact)
                    .orElse(null);

            try {
                InputDocument doc = FileReferenceId.deserialize(document()).asInputDocument();

                return InputMediaDocument.builder()
                        .id(doc)
                        .query(query().orElse(null))
                        .ttlSeconds(ttlSeconds)
                        .build();
            } catch (IllegalArgumentException t) {
                return InputMediaDocumentExternal.builder()
                        .url(document())
                        .ttlSeconds(ttlSeconds)
                        .build();
            }
        });
    }

    public InputMediaDocumentSpec withDocument(String value) {
        Objects.requireNonNull(value);
        if (this.document.equals(value)) return this;
        return new InputMediaDocumentSpec(value, this.query, this.autoDeleteDuration);
    }

    public InputMediaDocumentSpec withQuery(@Nullable String value) {
        if (Objects.equals(this.query, value)) return this;
        return new InputMediaDocumentSpec(this.document, value, this.autoDeleteDuration);
    }

    public InputMediaDocumentSpec withQuery(Optional<String> opt) {
        return withQuery(opt.orElse(null));
    }

    public InputMediaDocumentSpec withAutoDeleteDuration(@Nullable Duration value) {
        if (Objects.equals(this.autoDeleteDuration, value)) return this;
        return new InputMediaDocumentSpec(this.document, this.query, value);
    }

    public InputMediaDocumentSpec withAutoDeleteDuration(Optional<Duration> opt) {
        return withAutoDeleteDuration(opt.orElse(null));
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InputMediaDocumentSpec that = (InputMediaDocumentSpec) o;
        return document.equals(that.document) && Objects.equals(query, that.query) &&
                Objects.equals(autoDeleteDuration, that.autoDeleteDuration);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + document.hashCode();
        h += (h << 5) + Objects.hashCode(query);
        h += (h << 5) + Objects.hashCode(autoDeleteDuration);
        return h;
    }

    public static InputMediaDocumentSpec of(String document) {
        return new InputMediaDocumentSpec(document);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private static final byte INIT_BIT_DOCUMENT = 0x1;
        private byte initBits = 0x1;

        private String document;
        private String query;
        private Duration autoDeleteDuration;

        private Builder() {
        }

        public Builder from(InputMediaDocumentSpec instance) {
            Objects.requireNonNull(instance);
            document(instance.document);
            query(instance.query);
            autoDeleteDuration(instance.autoDeleteDuration);
            return this;
        }

        public Builder document(String document) {
            this.document = Objects.requireNonNull(document);
            initBits &= ~INIT_BIT_DOCUMENT;
            return this;
        }

        public Builder query(@Nullable String query) {
            this.query = query;
            return this;
        }

        public Builder query(Optional<String> query) {
            this.query = query.orElse(null);
            return this;
        }

        public Builder autoDeleteDuration(@Nullable Duration autoDeleteDuration) {
            this.autoDeleteDuration = autoDeleteDuration;
            return this;
        }

        public Builder autoDeleteDuration(Optional<Duration> autoDeleteDuration) {
            this.autoDeleteDuration = autoDeleteDuration.orElse(null);
            return this;
        }

        public InputMediaDocumentSpec build() {
            if (initBits != 0) {
                throw incompleteInitialization();
            }
            return new InputMediaDocumentSpec(document, query, autoDeleteDuration);
        }

        private IllegalStateException incompleteInitialization() {
            List<String> attributes = new ArrayList<>();
            if ((initBits & INIT_BIT_DOCUMENT) != 0) attributes.add("document");
            return new IllegalStateException("Cannot build InputMediaDocumentSpec, some of required attributes are not set " + attributes);
        }
    }
}
