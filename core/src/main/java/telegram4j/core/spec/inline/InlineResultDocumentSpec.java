package telegram4j.core.spec.inline;

import reactor.util.annotation.Nullable;
import telegram4j.mtproto.file.FileReferenceId.DocumentType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Spec for documents and photos inline results.
 *
 * <p>To create audio file inline result these fields must be present:
 * <ol>
 *   <li>{@link #type()} to {@link DocumentType#AUDIO} if it's web file</li>
 *   <li>{@link #title()} - the title of audio file</li>
 *   <li>{@link #performer()} - the performer of audio file</li>
 *   <li>{@link #duration()} - the duration of audio file</li>
 *   <li>{@link #thumb()} - the static thumbnail of audio file</li>
 * </ol>
 */
public final class InlineResultDocumentSpec implements InlineResultSpec {

    private final DocumentType type;
    @Nullable
    private final String title;
    @Nullable
    private final String performer;
    @Nullable
    private final String description;
    @Nullable
    private final Duration duration;
    @Nullable
    private final WebDocumentFields.Size size;
    private final String file;
    @Nullable
    private final String mimeType;
    @Nullable
    private final WebDocumentSpec thumb;
    @Nullable
    private final String filename;
    private final String id;
    private final InlineMessageSpec message;

    private InlineResultDocumentSpec(String file, String id, InlineMessageSpec message) {
        this.file = Objects.requireNonNull(file);
        this.id = Objects.requireNonNull(id);
        this.message = Objects.requireNonNull(message);
        this.type = null;
        this.title = null;
        this.performer = null;
        this.description = null;
        this.duration = null;
        this.size = null;
        this.mimeType = null;
        this.thumb = null;
        this.filename = null;
    }

    private InlineResultDocumentSpec(@Nullable DocumentType type, @Nullable String title,
                                     @Nullable String performer, @Nullable String description,
                                     @Nullable Duration duration, @Nullable WebDocumentFields.Size size,
                                     String file, @Nullable String mimeType, @Nullable WebDocumentSpec thumb,
                                     @Nullable String filename, String id, InlineMessageSpec message) {
        this.type = type;
        this.title = title;
        this.performer = performer;
        this.description = description;
        this.duration = duration;
        this.size = size;
        this.file = file;
        this.mimeType = mimeType;
        this.thumb = thumb;
        this.filename = filename;
        this.id = id;
        this.message = message;
    }

    /**
     * @return The type of web file, if absent {@link DocumentType#GENERAL}
     * would be used.
     */
    public Optional<DocumentType> type() {
        return Optional.ofNullable(type);
    }

    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    public Optional<String> performer() {
        return Optional.ofNullable(performer);
    }

    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    public Optional<Duration> duration() {
        return Optional.ofNullable(duration);
    }

    public Optional<WebDocumentFields.Size> size() {
        return Optional.ofNullable(size);
    }

    /**
     * @return The serialized {@link telegram4j.mtproto.file.FileReferenceId} of file or URL.
     */
    public String file() {
        return file;
    }

    /**
     * @return The mime type for web file. Must be <b>application/pdf</b> or <b>application/zip</b>
     */
    public Optional<String> mimeType() {
        return Optional.ofNullable(mimeType);
    }

    public Optional<WebDocumentSpec> thumb() {
        return Optional.ofNullable(thumb);
    }

    public Optional<String> filename() {
        return Optional.ofNullable(filename);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public InlineMessageSpec message() {
        return message;
    }

    public InlineResultDocumentSpec withType(@Nullable DocumentType value) {
        if (type == value) return this;
        return new InlineResultDocumentSpec(value, title, performer, description, duration, size, file, mimeType, thumb, filename, id, message);
    }

    public InlineResultDocumentSpec withType(Optional<DocumentType> opt) {
        return withType(opt.orElse(null));
    }

    public InlineResultDocumentSpec withTitle(@Nullable String value) {
        if (Objects.equals(title, value)) return this;
        return new InlineResultDocumentSpec(type, value, performer, description, duration, size, file, mimeType, thumb, filename, id, message);
    }

    public InlineResultDocumentSpec withTitle(Optional<String> opt) {
        return withTitle(opt.orElse(null));
    }

    public InlineResultDocumentSpec withPerformer(@Nullable String value) {
        if (Objects.equals(performer, value)) return this;
        return new InlineResultDocumentSpec(type, title, value, description, duration, size, file, mimeType, thumb, filename, id, message);
    }

    public InlineResultDocumentSpec withPerformer(Optional<String> opt) {
        return withPerformer(opt.orElse(null));
    }

    public InlineResultDocumentSpec withDescription(@Nullable String value) {
        if (Objects.equals(description, value)) return this;
        return new InlineResultDocumentSpec(type, title, performer, value, duration, size, file, mimeType, thumb, filename, id, message);
    }

    public InlineResultDocumentSpec withDescription(Optional<String> opt) {
        return withDescription(opt.orElse(null));
    }

    public InlineResultDocumentSpec withDuration(@Nullable Duration value) {
        if (duration == value) return this;
        return new InlineResultDocumentSpec(type, title, performer, description, value, size, file, mimeType, thumb, filename, id, message);
    }

    public InlineResultDocumentSpec withDuration(Optional<Duration> opt) {
        return withDuration(opt.orElse(null));
    }

    public InlineResultDocumentSpec withSize(@Nullable WebDocumentFields.Size value) {
        if (size == value) return this;
        return new InlineResultDocumentSpec(type, title, performer, description, duration, value, file, mimeType, thumb, filename, id, message);
    }

    public InlineResultDocumentSpec withSize(Optional<WebDocumentFields.Size> opt) {
        return withSize(opt.orElse(null));
    }

    public InlineResultDocumentSpec withFile(String value) {
        Objects.requireNonNull(value);
        if (file.equals(value)) return this;
        return new InlineResultDocumentSpec(type, title, performer, description, duration, size, value, mimeType, thumb, filename, id, message);
    }

    public InlineResultDocumentSpec withMimeType(@Nullable String value) {
        if (Objects.equals(mimeType, value)) return this;
        return new InlineResultDocumentSpec(type, title, performer, description, duration, size, file, value, thumb, filename, id, message);
    }

    public InlineResultDocumentSpec withMimeType(Optional<String> opt) {
        return withMimeType(opt.orElse(null));
    }

    public InlineResultDocumentSpec withThumb(@Nullable WebDocumentSpec value) {
        if (thumb == value) return this;
        return new InlineResultDocumentSpec(type, title, performer, description, duration, size, file, mimeType, value, filename, id, message);
    }

    public InlineResultDocumentSpec withThumb(Optional<WebDocumentSpec> opt) {
        return withThumb(opt.orElse(null));
    }

    public InlineResultDocumentSpec withId(String value) {
        Objects.requireNonNull(value);
        if (id.equals(value)) return this;
        return new InlineResultDocumentSpec(type, title, performer, description, duration, size,
                file, mimeType, thumb, filename, value, message);
    }

    public InlineResultDocumentSpec withMessage(InlineMessageSpec value) {
        Objects.requireNonNull(value);
        if (message == value) return this;
        return new InlineResultDocumentSpec(type, title, performer, description, duration, size,
                file, mimeType, thumb, filename, id, value);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InlineResultDocumentSpec that = (InlineResultDocumentSpec) o;
        return type == that.type && Objects.equals(title, that.title) && Objects.equals(performer, that.performer) &&
                Objects.equals(description, that.description) && Objects.equals(duration, that.duration) &&
                Objects.equals(size, that.size) && file.equals(that.file) && Objects.equals(mimeType, that.mimeType) &&
                Objects.equals(thumb, that.thumb) && id.equals(that.id) && message.equals(that.message);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(type);
        h += (h << 5) + Objects.hashCode(title);
        h += (h << 5) + Objects.hashCode(performer);
        h += (h << 5) + Objects.hashCode(description);
        h += (h << 5) + Objects.hashCode(duration);
        h += (h << 5) + Objects.hashCode(size);
        h += (h << 5) + file.hashCode();
        h += (h << 5) + Objects.hashCode(mimeType);
        h += (h << 5) + Objects.hashCode(thumb);
        h += (h << 5) + id.hashCode();
        h += (h << 5) + message.hashCode();
        return h;
    }

    @Override
    public String toString() {
        return "InlineResultDocumentSpec{" +
                "type=" + type +
                ", title='" + title + '\'' +
                ", performer='" + performer + '\'' +
                ", description='" + description + '\'' +
                ", duration=" + duration +
                ", size=" + size +
                ", file='" + file + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", thumb=" + thumb +
                ", id='" + id + '\'' +
                ", message=" + message +
                '}';
    }

    public static InlineResultDocumentSpec of(String file, String id, InlineMessageSpec message) {
        return new InlineResultDocumentSpec(file, id, message);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private static final byte INIT_BIT_FILE = 0x1;
        private static final byte INIT_BIT_ID = 0x2;
        private static final byte INIT_BIT_MESSAGE = 0x4;
        private byte initBits = 0x7;

        private DocumentType type;
        private String title;
        private String performer;
        private String description;
        private Duration duration;
        private WebDocumentFields.Size size;
        private String file;
        private String mimeType;
        private WebDocumentSpec thumb;
        private String filename;
        private String id;
        private InlineMessageSpec message;

        private Builder() {
        }

        public Builder from(InlineResultDocumentSpec instance) {
            return from((Object) instance);
        }

        public Builder from(InlineResultSpec instance) {
            return from((Object) instance);
        }

        private Builder from(Object object) {
            Objects.requireNonNull(object);
            if (object instanceof InlineResultDocumentSpec) {
                InlineResultDocumentSpec instance = (InlineResultDocumentSpec) object;
                duration(instance.duration);
                file(instance.file);
                size(instance.size);
                thumb(instance.thumb);
                filename(instance.filename);
                description(instance.description);
                mimeType(instance.mimeType);
                id(instance.id);
                type(instance.type);
                title(instance.title);
                performer(instance.performer);
                message(instance.message);
            } else if (object instanceof InlineResultSpec) {
                InlineResultSpec instance = (InlineResultSpec) object;
                message(instance.message());
                id(instance.id());
            }
            return this;
        }

        public Builder type(@Nullable DocumentType type) {
            this.type = type;
            return this;
        }

        public Builder type(Optional<DocumentType> type) {
            this.type = type.orElse(null);
            return this;
        }

        public Builder title(@Nullable String title) {
            this.title = title;
            return this;
        }

        public Builder title(Optional<String> title) {
            this.title = title.orElse(null);
            return this;
        }

        public Builder performer(@Nullable String performer) {
            this.performer = performer;
            return this;
        }

        public Builder performer(Optional<String> performer) {
            this.performer = performer.orElse(null);
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

        public Builder duration(@Nullable Duration duration) {
            this.duration = duration;
            return this;
        }

        public Builder duration(Optional<Duration> duration) {
            this.duration = duration.orElse(null);
            return this;
        }

        public Builder size(int size) {
            this.size = WebDocumentFields.Size.of(size);
            return this;
        }

        public Builder size(int width, int height) {
            this.size = WebDocumentFields.Size.of(width, height);
            return this;
        }

        public Builder size(@Nullable WebDocumentFields.Size size) {
            this.size = size;
            return this;
        }

        public Builder size(Optional<WebDocumentFields.Size> size) {
            this.size = size.orElse(null);
            return this;
        }

        public Builder file(String file) {
            this.file = Objects.requireNonNull(file);
            initBits &= ~INIT_BIT_FILE;
            return this;
        }

        public Builder mimeType(@Nullable String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder mimeType(Optional<String> mimeType) {
            this.mimeType = mimeType.orElse(null);
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

        public Builder filename(@Nullable String filename) {
            this.filename = filename;
            return this;
        }

        public Builder filename(Optional<String> filename) {
            this.filename = filename.orElse(null);
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

        public InlineResultDocumentSpec build() {
            if (initBits != 0) {
                throw incompleteInitialization();
            }
            return new InlineResultDocumentSpec(type, title, performer, description,
                    duration, size, file, mimeType, thumb, filename, id, message);
        }

        private IllegalStateException incompleteInitialization() {
            List<String> attributes = new ArrayList<>(Integer.bitCount(initBits));
            if ((initBits & INIT_BIT_FILE) != 0) attributes.add("file");
            if ((initBits & INIT_BIT_ID) != 0) attributes.add("id");
            if ((initBits & INIT_BIT_MESSAGE) != 0) attributes.add("message");
            return new IllegalStateException("Cannot build InlineResultDocumentSpec, some of required attributes are not set " + attributes);
        }
    }
}
