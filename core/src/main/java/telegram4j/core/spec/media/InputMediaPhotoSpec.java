package telegram4j.core.spec.media;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.util.Variant2;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.InputMedia;
import telegram4j.tl.InputMediaPhoto;
import telegram4j.tl.InputMediaPhotoExternal;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public final class InputMediaPhotoSpec implements InputMediaSpec {
    @Nullable
    private final FileReferenceId photoFri;
    @Nullable
    private final String photoUrl;
    private final Duration autoDeleteDuration;

    private InputMediaPhotoSpec(FileReferenceId photoFri) {
        this.photoFri = Objects.requireNonNull(photoFri);
        this.photoUrl = null;
        this.autoDeleteDuration = null;
    }

    private InputMediaPhotoSpec(String photoUrl) {
        this.photoUrl = Objects.requireNonNull(photoUrl);
        this.autoDeleteDuration = null;
        this.photoFri = null;
    }

    private InputMediaPhotoSpec(@Nullable FileReferenceId photoFri, @Nullable String photoUrl,
                                @Nullable Duration autoDeleteDuration) {
        this.photoFri = photoFri;
        this.photoUrl = photoUrl;
        this.autoDeleteDuration = autoDeleteDuration;
    }

    public Variant2<String, FileReferenceId> photo() {
        return Variant2.of(photoUrl, photoFri);
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

            if (photoFri != null) {
                return InputMediaPhoto.builder()
                        .id(photoFri.asInputPhoto())
                        .ttlSeconds(ttlSeconds)
                        .build();
            }
            return InputMediaPhotoExternal.builder()
                    .url(Objects.requireNonNull(photoUrl))
                    .ttlSeconds(ttlSeconds)
                    .build();
        });
    }

    public InputMediaPhotoSpec withPhoto(String photoUrl) {
        if (photoUrl.equals(this.photoUrl)) return this;
        return new InputMediaPhotoSpec(null, photoUrl, this.autoDeleteDuration);
    }

    public InputMediaPhotoSpec withPhoto(FileReferenceId photoFri) {
        if (photoFri.equals(this.photoFri)) return this;
        return new InputMediaPhotoSpec(photoFri, null, this.autoDeleteDuration);
    }

    public InputMediaPhotoSpec withAutoDeleteDuration(@Nullable Duration value) {
        if (Objects.equals(this.autoDeleteDuration, value)) return this;
        return new InputMediaPhotoSpec(photoFri, photoUrl, value);
    }

    public InputMediaPhotoSpec withAutoDeleteDuration(Optional<Duration> optional) {
        return withAutoDeleteDuration(optional.orElse(null));
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof InputMediaPhotoSpec)) return false;
        InputMediaPhotoSpec that = (InputMediaPhotoSpec) o;
        return Objects.equals(photoFri, that.photoFri) &&
                Objects.equals(photoUrl, that.photoUrl) &&
                Objects.equals(autoDeleteDuration, that.autoDeleteDuration);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(photoFri);
        h += (h << 5) + Objects.hashCode(photoUrl);
        h += (h << 5) + Objects.hashCode(autoDeleteDuration);
        return h;
    }

    @Override
    public String toString() {
        return "InputMediaPhotoSpec{" +
                "photo=" + (photoFri != null ? photoFri : photoUrl) +
                ", autoDeleteDuration=" + autoDeleteDuration +
                '}';
    }

    public static InputMediaPhotoSpec of(FileReferenceId photoFri) {
        return new InputMediaPhotoSpec(photoFri);
    }

    public static InputMediaPhotoSpec of(String photoUrl) {
        return new InputMediaPhotoSpec(photoUrl);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private FileReferenceId photoFri;
        private String photoUrl;
        private Duration autoDeleteDuration;

        private Builder() {
        }

        public Builder from(InputMediaPhotoSpec instance) {
            photoUrl = instance.photoUrl;
            photoFri = instance.photoFri;
            autoDeleteDuration = instance.autoDeleteDuration;
            return this;
        }

        public Builder photo(FileReferenceId photoFri) {
            this.photoFri = Objects.requireNonNull(photoFri);
            this.photoUrl = null;
            return this;
        }

        public Builder photo(String photoUrl) {
            this.photoUrl = Objects.requireNonNull(photoUrl);
            this.photoFri = null;
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

        public InputMediaPhotoSpec build() {
            if (photoFri == null && photoUrl == null) {
                throw new IllegalStateException("Cannot build InputMediaPhotoSpec, 'photo' attribute is not set");
            }
            return new InputMediaPhotoSpec(photoFri, photoUrl, autoDeleteDuration);
        }
    }
}
