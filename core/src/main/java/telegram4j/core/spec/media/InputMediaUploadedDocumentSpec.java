package telegram4j.core.spec.media;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.DocumentAttribute;
import telegram4j.tl.InputFile;
import telegram4j.tl.InputMedia;
import telegram4j.tl.InputMediaUploadedDocument;
import telegram4j.tl.api.TlEncodingUtil;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class InputMediaUploadedDocumentSpec implements InputMediaSpec {
    private final boolean noSoundVideo;
    private final boolean forceFile;
    private final InputFile file;
    private final InputFile thumb;
    private final String mimeType;
    private final List<DocumentAttribute> attributes;
    private final List<String> stickers;
    private final Duration autoDeleteDuration;

    private InputMediaUploadedDocumentSpec(InputFile file, String mimeType,
                                           Iterable<? extends DocumentAttribute> attributes) {
        this.file = Objects.requireNonNull(file);
        this.mimeType = Objects.requireNonNull(mimeType);
        this.thumb = null;
        this.attributes = TlEncodingUtil.copyList(attributes);
        this.stickers = null;
        this.autoDeleteDuration = null;
        this.noSoundVideo = false;
        this.forceFile = false;
    }

    private InputMediaUploadedDocumentSpec(Builder builder) {
        this.file = builder.file;
        this.thumb = builder.thumb;
        this.mimeType = builder.mimeType;
        this.attributes = List.copyOf(builder.attributes);
        this.stickers = builder.stickers != null ? List.copyOf(builder.stickers) : null;
        this.autoDeleteDuration = builder.autoDeleteDuration;
        this.noSoundVideo = builder.noSoundVideo;
        this.forceFile = builder.forceFile;
    }

    private InputMediaUploadedDocumentSpec(boolean noSoundVideo, boolean forceFile,
                                           InputFile file, @Nullable InputFile thumb, String mimeType,
                                           List<DocumentAttribute> attributes, @Nullable List<String> stickers,
                                           @Nullable Duration autoDeleteDuration) {
        this.noSoundVideo = noSoundVideo;
        this.forceFile = forceFile;
        this.file = file;
        this.thumb = thumb;
        this.mimeType = mimeType;
        this.attributes = attributes;
        this.stickers = stickers;
        this.autoDeleteDuration = autoDeleteDuration;
    }

    public boolean noSoundVideo() {
        return noSoundVideo;
    }

    public boolean forceFile() {
        return forceFile;
    }

    public InputFile file() {
        return file;
    }

    public Optional<InputFile> thumb() {
        return Optional.ofNullable(thumb);
    }

    public String mimeType() {
        return mimeType;
    }

    public List<DocumentAttribute> attributes() {
        return attributes;
    }

    public Optional<List<String>> stickers() {
        return Optional.ofNullable(stickers);
    }

    public Optional<Duration> autoDeleteDuration() {
        return Optional.ofNullable(autoDeleteDuration);
    }

    @Override
    public Mono<InputMedia> asData(MTProtoTelegramClient client) {
        return Mono.fromSupplier(() -> InputMediaUploadedDocument.builder()
                .nosoundVideo(noSoundVideo())
                .forceFile(forceFile())
                .file(file())
                .thumb(thumb().orElse(null))
                .mimeType(mimeType())
                .attributes(attributes())
                .stickers(stickers()
                        .map(list -> list.stream()
                                .map(s -> FileReferenceId.deserialize(s)
                                        .asInputDocument())
                                .collect(Collectors.toList()))
                        .orElse(null))
                .ttlSeconds(autoDeleteDuration()
                        .map(Duration::getSeconds)
                        .map(Math::toIntExact)
                        .orElse(null))
                .build());
    }

    public InputMediaUploadedDocumentSpec withNoSoundVideo(boolean value) {
        if (noSoundVideo == value) return this;
        return new InputMediaUploadedDocumentSpec(value, forceFile, file, thumb, mimeType,
                attributes, stickers, autoDeleteDuration);
    }

    public InputMediaUploadedDocumentSpec withForceFile(boolean value) {
        if (forceFile == value) return this;
        return new InputMediaUploadedDocumentSpec(noSoundVideo, value, file, thumb, mimeType,
                attributes, stickers, autoDeleteDuration);
    }

    public InputMediaUploadedDocumentSpec withFile(InputFile value) {
        Objects.requireNonNull(value);
        if (file == value) return this;
        return new InputMediaUploadedDocumentSpec(noSoundVideo, forceFile, value, thumb, mimeType,
                attributes, stickers, autoDeleteDuration);
    }

    public InputMediaUploadedDocumentSpec withThumb(@Nullable InputFile value) {
        if (thumb == value) return this;
        return new InputMediaUploadedDocumentSpec(noSoundVideo, forceFile, file, value, mimeType,
                attributes, stickers, autoDeleteDuration);
    }

    public InputMediaUploadedDocumentSpec withThumb(Optional<? extends InputFile> opt) {
        return withThumb(opt.orElse(null));
    }

    public InputMediaUploadedDocumentSpec withMimeType(String value) {
        Objects.requireNonNull(value);
        if (mimeType.equals(value)) return this;
        return new InputMediaUploadedDocumentSpec(noSoundVideo, forceFile, file, thumb,
                value, attributes, stickers, autoDeleteDuration);
    }

    public InputMediaUploadedDocumentSpec withAttributes(DocumentAttribute... elements) {
        Objects.requireNonNull(elements);
        var newValue = List.of(elements);
        if (attributes == newValue) return this;
        return new InputMediaUploadedDocumentSpec(noSoundVideo, forceFile, file, thumb, mimeType,
                newValue, stickers, autoDeleteDuration);
    }

    public InputMediaUploadedDocumentSpec withAttributes(Iterable<? extends DocumentAttribute> elements) {
        Objects.requireNonNull(elements);
        if (attributes == elements) return this;
        List<DocumentAttribute> newValue = TlEncodingUtil.copyList(elements);
        if (attributes == newValue) return this;
        return new InputMediaUploadedDocumentSpec(noSoundVideo, forceFile, file, thumb, mimeType,
                newValue, stickers, autoDeleteDuration);
    }

    public InputMediaUploadedDocumentSpec withStickers(@Nullable Iterable<String> value) {
        if (stickers == value) return this;
        List<String> newValue = value != null ? TlEncodingUtil.copyList(value) : null;
        if (stickers == newValue) return this;
        return new InputMediaUploadedDocumentSpec(noSoundVideo, forceFile, file, thumb, mimeType,
                attributes, newValue, autoDeleteDuration);
    }

    public InputMediaUploadedDocumentSpec withStickers(Optional<? extends List<String>> opt) {
        return withStickers(opt.orElse(null));
    }

    public InputMediaUploadedDocumentSpec withAutoDeleteDuration(@Nullable Duration value) {
        if (Objects.equals(autoDeleteDuration, value)) return this;
        return new InputMediaUploadedDocumentSpec(noSoundVideo, forceFile, file, thumb, mimeType,
                attributes, stickers, value);
    }

    public InputMediaUploadedDocumentSpec withAutoDeleteDuration(Optional<? extends Duration> opt) {
        return withAutoDeleteDuration(opt.orElse(null));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InputMediaUploadedDocumentSpec)) return false;
        InputMediaUploadedDocumentSpec that = (InputMediaUploadedDocumentSpec) o;
        return noSoundVideo == that.noSoundVideo
                && forceFile == that.forceFile
                && file.equals(that.file)
                && Objects.equals(thumb, that.thumb)
                && mimeType.equals(that.mimeType)
                && attributes.equals(that.attributes)
                && Objects.equals(stickers, that.stickers)
                && Objects.equals(autoDeleteDuration, that.autoDeleteDuration);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Boolean.hashCode(noSoundVideo);
        h += (h << 5) + Boolean.hashCode(forceFile);
        h += (h << 5) + file.hashCode();
        h += (h << 5) + Objects.hashCode(thumb);
        h += (h << 5) + mimeType.hashCode();
        h += (h << 5) + attributes.hashCode();
        h += (h << 5) + Objects.hashCode(stickers);
        h += (h << 5) + Objects.hashCode(autoDeleteDuration);
        return h;
    }

    @Override
    public String toString() {
        return "InputMediaUploadedDocumentSpec{" +
                "noSoundVideo=" + noSoundVideo +
                ", forceFile=" + forceFile +
                ", file=" + file +
                ", thumb=" + thumb +
                ", mimeType='" + mimeType + '\'' +
                ", attributes=" + attributes +
                ", stickers=" + stickers +
                ", autoDeleteDuration=" + autoDeleteDuration +
                '}';
    }

    public static InputMediaUploadedDocumentSpec of(InputFile file, String mimeType,
                                                    Iterable<? extends DocumentAttribute> attributes) {
        return new InputMediaUploadedDocumentSpec(file, mimeType, attributes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private static final byte INIT_BIT_FILE = 0x1;
        private static final byte INIT_BIT_MIME_TYPE = 0x2;
        private static final byte INIT_BIT_ATTRIBUTES = 0x4;
        private byte initBits = 0x7;

        private boolean noSoundVideo;
        private boolean forceFile;
        private InputFile file;
        private InputFile thumb;
        private String mimeType;
        private List<DocumentAttribute> attributes;
        private List<String> stickers;
        private Duration autoDeleteDuration;

        private Builder() {
        }

        public Builder from(InputMediaUploadedDocumentSpec instance) {
            Objects.requireNonNull(instance);
            noSoundVideo(instance.noSoundVideo);
            forceFile(instance.forceFile);
            file(instance.file);
            thumb(instance.thumb);
            mimeType(instance.mimeType);
            attributes(instance.attributes);
            stickers(instance.stickers);
            autoDeleteDuration(instance.autoDeleteDuration);
            return this;
        }

        public Builder noSoundVideo(boolean noSoundVideo) {
            this.noSoundVideo = noSoundVideo;
            return this;
        }

        public Builder forceFile(boolean forceFile) {
            this.forceFile = forceFile;
            return this;
        }

        public Builder file(InputFile file) {
            this.file = Objects.requireNonNull(file);
            initBits &= ~INIT_BIT_FILE;
            return this;
        }

        public Builder thumb(@Nullable InputFile thumb) {
            this.thumb = thumb;
            return this;
        }

        public Builder thumb(Optional<? extends InputFile> thumb) {
            this.thumb = thumb.orElse(null);
            return this;
        }

        public Builder mimeType(String mimeType) {
            this.mimeType = Objects.requireNonNull(mimeType);
            initBits &= ~INIT_BIT_MIME_TYPE;
            return this;
        }

        public Builder addAttribute(DocumentAttribute element) {
            Objects.requireNonNull(element);
            if (attributes == null) {
                attributes = new ArrayList<>();
                initBits &= ~INIT_BIT_ATTRIBUTES;
            }
            this.attributes.add(element);
            return this;
        }

        public Builder addAttributes(DocumentAttribute... elements) {
            var copy = Arrays.stream(elements)
                    .map(Objects::requireNonNull)
                    .collect(Collectors.toList());
            if (attributes == null) {
                attributes = copy;
                initBits &= ~INIT_BIT_ATTRIBUTES;
            } else {
                attributes.addAll(copy);
            }
            return this;
        }

        public Builder attributes(Iterable<? extends DocumentAttribute> elements) {
            attributes = StreamSupport.stream(elements.spliterator(), false)
                    .map(Objects::requireNonNull)
                    .collect(Collectors.toList());
            initBits &= ~INIT_BIT_ATTRIBUTES;
            return this;
        }

        public Builder addAllAttributes(Iterable<? extends DocumentAttribute> elements) {
            List<DocumentAttribute> copy = StreamSupport.stream(elements.spliterator(), false)
                    .map(Objects::requireNonNull)
                    .collect(Collectors.toList());
            if (attributes == null) {
                attributes = copy;
                initBits &= ~INIT_BIT_ATTRIBUTES;
            } else {
                attributes.addAll(copy);
            }
            return this;
        }

        public Builder stickers(@Nullable List<String> stickers) {
            this.stickers = stickers;
            return this;
        }

        public Builder stickers(Optional<? extends List<String>> stickers) {
            this.stickers = stickers.orElse(null);
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

        public InputMediaUploadedDocumentSpec build() {
            if (initBits != 0) {
                throw new IllegalStateException(formatRequiredAttributesMessage());
            }
            return new InputMediaUploadedDocumentSpec(this);
        }

        private String formatRequiredAttributesMessage() {
            List<String> attributes = new ArrayList<>();
            if ((initBits & INIT_BIT_FILE) != 0) attributes.add("file");
            if ((initBits & INIT_BIT_MIME_TYPE) != 0) attributes.add("mimeType");
            if ((initBits & INIT_BIT_ATTRIBUTES) != 0) attributes.add("attributes");
            return "Cannot build InputMediaUploadedDocumentSpec, some of required attributes are not set " + attributes;
        }
    }
}
