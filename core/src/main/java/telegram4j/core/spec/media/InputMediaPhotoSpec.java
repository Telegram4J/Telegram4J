package telegram4j.core.spec.media;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.InputMedia;
import telegram4j.tl.InputMediaPhoto;
import telegram4j.tl.InputMediaPhotoExternal;
import telegram4j.tl.InputPhoto;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class InputMediaPhotoSpec implements InputMediaSpec {
    private final String photo;
    private final Duration autoDeleteDuration;

    private InputMediaPhotoSpec(String photo) {
        this.photo = Objects.requireNonNull(photo);
        this.autoDeleteDuration = null;
    }

    private InputMediaPhotoSpec(String photo, @Nullable Duration autoDeleteDuration) {
        this.photo = photo;
        this.autoDeleteDuration = autoDeleteDuration;
    }

    /**
     * @return The serialized {@link telegram4j.mtproto.file.FileReferenceId} or url to web file.
     */
    public String photo() {
        return photo;
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
                InputPhoto doc = FileReferenceId.deserialize(photo).asInputPhoto();

                return InputMediaPhoto.builder()
                        .id(doc)
                        .ttlSeconds(ttlSeconds)
                        .build();
            } catch (IllegalArgumentException t) {
                return InputMediaPhotoExternal.builder()
                        .url(photo)
                        .ttlSeconds(ttlSeconds)
                        .build();
            }
        });
    }

    public InputMediaPhotoSpec withPhoto(String value) {
        String newValue = Objects.requireNonNull(value, "photo");
        if (this.photo.equals(newValue)) return this;
        return new InputMediaPhotoSpec(newValue, this.autoDeleteDuration);
    }

    public InputMediaPhotoSpec withAutoDeleteDuration(@Nullable Duration value) {
        if (Objects.equals(this.autoDeleteDuration, value)) return this;
        return new InputMediaPhotoSpec(this.photo, value);
    }

    public InputMediaPhotoSpec withAutoDeleteDuration(Optional<Duration> optional) {
        return withAutoDeleteDuration(optional.orElse(null));
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InputMediaPhotoSpec that = (InputMediaPhotoSpec) o;
        return photo.equals(that.photo) && Objects.equals(autoDeleteDuration, that.autoDeleteDuration);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + photo.hashCode();
        h += (h << 5) + Objects.hashCode(autoDeleteDuration);
        return h;
    }

    @Override
    public String toString() {
        return "InputMediaPhotoSpec{" +
                "photo='" + photo + '\'' +
                ", autoDeleteDuration=" + autoDeleteDuration +
                '}';
    }

    public static InputMediaPhotoSpec of(String photo) {
        return new InputMediaPhotoSpec(photo);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private static final byte INIT_BIT_PHOTO = 0x1;
        private byte initBits = 0x1;

        private String photo;
        private Duration autoDeleteDuration;

        private Builder() {
        }

        public Builder from(InputMediaPhotoSpec instance) {
            Objects.requireNonNull(instance);
            photo(instance.photo);
            autoDeleteDuration(instance.autoDeleteDuration);
            return this;
        }

        public Builder photo(String photo) {
            this.photo = Objects.requireNonNull(photo);
            initBits &= ~INIT_BIT_PHOTO;
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
            if (initBits != 0) {
                throw incompleteInitialization();
            }
            return new InputMediaPhotoSpec(photo, autoDeleteDuration);
        }

        private IllegalStateException incompleteInitialization() {
            List<String> attributes = new ArrayList<>();
            if ((initBits & INIT_BIT_PHOTO) != 0) attributes.add("photo");
            return new IllegalStateException("Cannot build InputMediaPhotoSpec, some of required attributes are not set " + attributes);
        }
    }
}
