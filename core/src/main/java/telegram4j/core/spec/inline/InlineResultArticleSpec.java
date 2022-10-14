package telegram4j.core.spec.inline;

import reactor.util.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class InlineResultArticleSpec implements InlineResultSpec {
    private final String title;
    private final String description;
    private final String url;
    private final WebDocumentSpec thumb;
    private final String id;
    private final InlineMessageSpec message;

    private InlineResultArticleSpec(String title, String url, String id, InlineMessageSpec message) {
        this.title = Objects.requireNonNull(title);
        this.url = Objects.requireNonNull(url);
        this.id = Objects.requireNonNull(id);
        this.message = Objects.requireNonNull(message);
        this.description = null;
        this.thumb = null;
    }

    private InlineResultArticleSpec(String title, @Nullable String description, String url,
                                    @Nullable WebDocumentSpec thumb, String id, InlineMessageSpec message) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.thumb = thumb;
        this.id = id;
        this.message = message;
    }

    public String title() {
        return title;
    }

    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    public String url() {
        return url;
    }

    public Optional<WebDocumentSpec> thumb() {
        return Optional.ofNullable(thumb);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public InlineMessageSpec message() {
        return message;
    }

    public InlineResultArticleSpec withTitle(String value) {
        Objects.requireNonNull(value);
        if (title.equals(value)) return this;
        return new InlineResultArticleSpec(value, description, url, thumb, id, message);
    }

    public InlineResultArticleSpec withDescription(@Nullable String value) {
        if (Objects.equals(description, value)) return this;
        return new InlineResultArticleSpec(title, value, url, thumb, id, message);
    }

    public InlineResultArticleSpec withDescription(Optional<String> opt) {
        return withDescription(opt.orElse(null));
    }

    public InlineResultArticleSpec withUrl(String value) {
        Objects.requireNonNull(value);
        if (url.equals(value)) return this;
        return new InlineResultArticleSpec(title, description, value, thumb, id, message);
    }

    public InlineResultArticleSpec withThumb(@Nullable WebDocumentSpec value) {
        if (thumb == value) return this;
        return new InlineResultArticleSpec(title, description, url, value, id, message);
    }

    public InlineResultArticleSpec withThumb(Optional<WebDocumentSpec> opt) {
        return withThumb(opt.orElse(null));
    }

    public InlineResultArticleSpec withId(String value) {
        Objects.requireNonNull(value);
        if (id.equals(value)) return this;
        return new InlineResultArticleSpec(title, description, url, thumb, value, message);
    }

    public InlineResultArticleSpec withMessage(InlineMessageSpec value) {
        Objects.requireNonNull(value);
        if (message == value) return this;
        return new InlineResultArticleSpec(title, description, url, thumb, id, value);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InlineResultArticleSpec that = (InlineResultArticleSpec) o;
        return title.equals(that.title) && Objects.equals(description, that.description) &&
                url.equals(that.url) && Objects.equals(thumb, that.thumb) &&
                id.equals(that.id) && message.equals(that.message);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + title.hashCode();
        h += (h << 5) + Objects.hashCode(description);
        h += (h << 5) + url.hashCode();
        h += (h << 5) + Objects.hashCode(thumb);
        h += (h << 5) + id.hashCode();
        h += (h << 5) + message.hashCode();
        return h;
    }

    @Override
    public String toString() {
        return "InlineResultArticleSpec{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", url='" + url + '\'' +
                ", thumb=" + thumb +
                ", id='" + id + '\'' +
                ", message=" + message +
                '}';
    }

    public static InlineResultArticleSpec of(String title, String url, String id, InlineMessageSpec message) {
        return new InlineResultArticleSpec(title, url, id, message);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private static final byte INIT_BIT_TITLE = 0x1;
        private static final byte INIT_BIT_URL = 0x2;
        private static final byte INIT_BIT_ID = 0x4;
        private static final byte INIT_BIT_MESSAGE = 0x8;
        private byte initBits = 0xf;

        private String title;
        private String description;
        private String url;
        private WebDocumentSpec thumb;
        private String id;
        private InlineMessageSpec message;

        private Builder() {
        }

        public Builder from(InlineResultArticleSpec instance) {
            return from((Object) instance);
        }

        public Builder from(InlineResultSpec instance) {
            return from((Object) instance);
        }

        private Builder from(Object object) {
            Objects.requireNonNull(object);
            if (object instanceof InlineResultArticleSpec) {
                InlineResultArticleSpec instance = (InlineResultArticleSpec) object;
                description(instance.description);
                id(instance.id);
                title(instance.title);
                message(instance.message);
                thumb(instance.thumb);
                url(instance.url);
            } else if (object instanceof InlineResultSpec) {
                InlineResultSpec instance = (InlineResultSpec) object;
                message(instance.message());
                id(instance.id());
            }
            return this;
        }

        public Builder title(String title) {
            this.title = Objects.requireNonNull(title);
            initBits &= ~INIT_BIT_TITLE;
            return this;
        }

        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        public Builder description(Optional<String> description) {
            this.description = description.orElse(null);
            return this;
        }

        public Builder url(String url) {
            this.url = Objects.requireNonNull(url);
            initBits &= ~INIT_BIT_URL;
            return this;
        }

        public Builder thumb(@Nullable WebDocumentSpec thumb) {
            this.thumb = thumb;
            return this;
        }

        public Builder thumb(Optional<WebDocumentSpec> thumb) {
            this.thumb = thumb.orElse(null);
            return this;
        }

        public Builder id(String id) {
            this.id = Objects.requireNonNull(id);
            initBits &= ~INIT_BIT_ID;
            return this;
        }

        public Builder message(InlineMessageSpec message) {
            this.message = Objects.requireNonNull(message);
            initBits &= ~INIT_BIT_MESSAGE;
            return this;
        }

        public InlineResultArticleSpec build() {
            if (initBits != 0) {
                throw incompleteInitialization();
            }
            return new InlineResultArticleSpec(title, description, url, thumb, id, message);
        }

        private IllegalStateException incompleteInitialization() {
            List<String> attributes = new ArrayList<>(Integer.bitCount(initBits));
            if ((initBits & INIT_BIT_TITLE) != 0) attributes.add("title");
            if ((initBits & INIT_BIT_URL) != 0) attributes.add("url");
            if ((initBits & INIT_BIT_ID) != 0) attributes.add("id");
            if ((initBits & INIT_BIT_MESSAGE) != 0) attributes.add("message");
            return new IllegalStateException("Cannot build InlineResultArticleSpec, some of required attributes are not set " + attributes);
        }
    }
}
