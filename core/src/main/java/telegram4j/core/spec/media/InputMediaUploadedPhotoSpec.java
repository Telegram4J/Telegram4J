package telegram4j.core.spec.media;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.InputFile;
import telegram4j.tl.InputMedia;
import telegram4j.tl.InputMediaUploadedPhoto;
import telegram4j.tl.api.TlEncodingUtil;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class InputMediaUploadedPhotoSpec implements InputMediaSpec {
    private final InputFile file;
    private final List<FileReferenceId> stickers;
    private final Duration autoDeleteDuration;

    private InputMediaUploadedPhotoSpec(InputFile file) {
        this.file = Objects.requireNonNull(file);
        this.stickers = null;
        this.autoDeleteDuration = null;
    }

    private InputMediaUploadedPhotoSpec(InputFile file, @Nullable Iterable<FileReferenceId> stickers, @Nullable Duration autoDeleteDuration) {
        this.file = file;
        this.stickers = stickers != null ? TlEncodingUtil.copyList(stickers) : null;
        this.autoDeleteDuration = autoDeleteDuration;
    }

    public InputFile file() {
        return file;
    }

    public Optional<List<FileReferenceId>> stickers() {
        return Optional.ofNullable(stickers);
    }

    public Optional<Duration> autoDeleteDuration() {
        return Optional.ofNullable(autoDeleteDuration);
    }

    @Override
    public Mono<InputMedia> asData(MTProtoTelegramClient client) {
        return Mono.fromSupplier(() -> InputMediaUploadedPhoto.builder()
                .file(file)
                .stickers(stickers()
                        .map(list -> list.stream()
                                .map(FileReferenceId::asInputDocument)
                                .collect(Collectors.toUnmodifiableList()))
                        .orElse(null))
                .ttlSeconds(autoDeleteDuration()
                        .map(Duration::getSeconds)
                        .map(Math::toIntExact)
                        .orElse(null))
                .build());
    }

    public InputMediaUploadedPhotoSpec withFile(InputFile value) {
        Objects.requireNonNull(value);
        if (file == value) return this;
        return new InputMediaUploadedPhotoSpec(value, stickers, autoDeleteDuration);
    }

    public InputMediaUploadedPhotoSpec withStickers(@Nullable Iterable<FileReferenceId> value) {
        if (stickers == value) return this;
        var newStickers = value != null ? TlEncodingUtil.copyList(value) : null;
        if (stickers == newStickers) return this;
        return new InputMediaUploadedPhotoSpec(file, newStickers, autoDeleteDuration);
    }

    public InputMediaUploadedPhotoSpec withStickers(Optional<? extends Iterable<FileReferenceId>> opt) {
        return withStickers(opt.orElse(null));
    }

    public InputMediaUploadedPhotoSpec withAutoDeleteDuration(@Nullable Duration value) {
        if (Objects.equals(autoDeleteDuration, value)) return this;
        return new InputMediaUploadedPhotoSpec(file, stickers, value);
    }

    public InputMediaUploadedPhotoSpec withAutoDeleteDuration(Optional<? extends Duration> opt) {
        return withAutoDeleteDuration(opt.orElse(null));
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InputMediaUploadedPhotoSpec that = (InputMediaUploadedPhotoSpec) o;
        return file.equals(that.file) && Objects.equals(stickers, that.stickers) &&
                Objects.equals(autoDeleteDuration, that.autoDeleteDuration);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + file.hashCode();
        h += (h << 5) + Objects.hashCode(stickers);
        h += (h << 5) + Objects.hashCode(autoDeleteDuration);
        return h;
    }

    @Override
    public String toString() {
        return "InputMediaUploadedPhotoSpec{" +
                "file=" + file +
                ", stickers=" + stickers +
                ", autoDeleteDuration=" + autoDeleteDuration +
                '}';
    }

    public static InputMediaUploadedPhotoSpec of(InputFile file) {
        return new InputMediaUploadedPhotoSpec(file);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private static final long INIT_BIT_FILE = 0x1L;
        private long initBits = 0x1L;

        private InputFile file;
        private List<FileReferenceId> stickers;
        private Duration autoDeleteDuration;

        private Builder() {
        }

        /**
         * Fill a builder with attribute values from the provided {@code InputMediaUploadedPhotoSpec} instance.
         * Regular attribute values will be replaced with those from the given instance.
         * Absent optional values will not replace present values.
         *
         * @param instance The instance from which to copy values
         * @return {@code this} builder for use in a chained invocation
         */
        public Builder from(InputMediaUploadedPhotoSpec instance) {
            file(instance.file);
            stickers(instance.stickers);
            autoDeleteDuration(instance.autoDeleteDuration);
            return this;
        }

        public Builder file(InputFile file) {
            this.file = Objects.requireNonNull(file);
            initBits &= ~INIT_BIT_FILE;
            return this;
        }

        public Builder stickers(@Nullable Iterable<FileReferenceId> stickers) {
            this.stickers = stickers != null ? TlEncodingUtil.copyList(stickers) : null;
            return this;
        }

        public Builder stickers(Optional<? extends Iterable<FileReferenceId>> stickers) {
            this.stickers = stickers.map(TlEncodingUtil::copyList).orElse(null);
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

        /**
         * Builds a new {@link InputMediaUploadedPhotoSpec InputMediaUploadedPhotoSpec}.
         *
         * @return An immutable instance of InputMediaUploadedPhotoSpec
         * @throws IllegalStateException if any required attributes are missing
         */
        public InputMediaUploadedPhotoSpec build() {
            if (initBits != 0) {
                throw new IllegalStateException(formatRequiredAttributesMessage());
            }
            return new InputMediaUploadedPhotoSpec(file, stickers, autoDeleteDuration);
        }

        private String formatRequiredAttributesMessage() {
            List<String> attributes = new ArrayList<>();
            if ((initBits & INIT_BIT_FILE) != 0) attributes.add("file");
            return "Cannot build InputMediaUploadedPhotoSpec, some of required attributes are not set " + attributes;
        }
    }
}
