package telegram4j.core.spec.media;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.BaseInputGeoPoint;
import telegram4j.tl.ImmutableInputMediaGeoPoint;
import telegram4j.tl.InputMediaGeoPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class InputMediaGeoPointSpec implements InputMediaSpec {
    private final double latitude;
    private final double longitude;
    private final Integer accuracyRadius;

    private InputMediaGeoPointSpec(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyRadius = null;
    }

    private InputMediaGeoPointSpec(double latitude, double longitude, @Nullable Integer accuracyRadius) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyRadius = accuracyRadius;
    }

    public double latitude() {
        return latitude;
    }

    public double longitude() {
        return longitude;
    }

    public Optional<Integer> accuracyRadius() {
        return Optional.ofNullable(accuracyRadius);
    }

    @Override
    public Mono<ImmutableInputMediaGeoPoint> resolve(MTProtoTelegramClient client) {
        return Mono.just(InputMediaGeoPoint.builder()
                .geoPoint(BaseInputGeoPoint.builder()
                        .lat(latitude())
                        .longitude(longitude())
                        .accuracyRadius(accuracyRadius().orElse(null))
                        .build())
                .build());
    }

    public InputMediaGeoPointSpec withLatitude(double value) {
        if (Double.doubleToLongBits(latitude) == Double.doubleToLongBits(value)) return this;
        return new InputMediaGeoPointSpec(value, longitude, accuracyRadius);
    }

    public InputMediaGeoPointSpec withLongitude(double value) {
        if (Double.doubleToLongBits(longitude) == Double.doubleToLongBits(value)) return this;
        return new InputMediaGeoPointSpec(latitude, value, accuracyRadius);
    }

    public InputMediaGeoPointSpec withAccuracyRadius(@Nullable Integer value) {
        if (Objects.equals(accuracyRadius, value)) return this;
        return new InputMediaGeoPointSpec(latitude, longitude, value);
    }

    public InputMediaGeoPointSpec withAccuracyRadius(Optional<Integer> opt) {
        return withAccuracyRadius(opt.orElse(null));
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof InputMediaGeoPointSpec that)) return false;
        return Double.doubleToLongBits(latitude) == Double.doubleToLongBits(that.latitude)
                && Double.doubleToLongBits(longitude) == Double.doubleToLongBits(that.longitude)
                && Objects.equals(accuracyRadius, that.accuracyRadius);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Double.hashCode(latitude);
        h += (h << 5) + Double.hashCode(longitude);
        h += (h << 5) + Objects.hashCode(accuracyRadius);
        return h;
    }

    @Override
    public String toString() {
        return "InputMediaGeoPointSpec{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", accuracyRadius=" + accuracyRadius +
                '}';
    }

    public static InputMediaGeoPointSpec of(double latitude, double longitude) {
        return new InputMediaGeoPointSpec(latitude, longitude);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private static final byte INIT_BIT_LATITUDE = 0x1;
        private static final byte INIT_BIT_LONGITUDE = 0x2;
        private byte initBits = 0x3;

        private double latitude;
        private double longitude;
        private Integer accuracyRadius;

        private Builder() {
        }

        public Builder from(InputMediaGeoPointSpec instance) {
            Objects.requireNonNull(instance);
            latitude(instance.latitude);
            longitude(instance.longitude);
            accuracyRadius(instance.accuracyRadius);
            return this;
        }

        public Builder latitude(double latitude) {
            this.latitude = latitude;
            initBits &= ~INIT_BIT_LATITUDE;
            return this;
        }

        public Builder longitude(double longitude) {
            this.longitude = longitude;
            initBits &= ~INIT_BIT_LONGITUDE;
            return this;
        }

        public Builder accuracyRadius(@Nullable Integer accuracyRadius) {
            this.accuracyRadius = accuracyRadius;
            return this;
        }

        public Builder accuracyRadius(Optional<Integer> accuracyRadius) {
            this.accuracyRadius = accuracyRadius.orElse(null);
            return this;
        }

        public InputMediaGeoPointSpec build() {
            if (initBits != 0) {
                throw incompleteInitialization();
            }
            return new InputMediaGeoPointSpec(latitude, longitude, accuracyRadius);
        }

        private IllegalStateException incompleteInitialization() {
            List<String> attributes = new ArrayList<>();
            if ((initBits & INIT_BIT_LATITUDE) != 0) attributes.add("latitude");
            if ((initBits & INIT_BIT_LONGITUDE) != 0) attributes.add("longitude");
            return new IllegalStateException("Cannot build InputMediaGeoPointSpec, some of required attributes are not set " + attributes);
        }
    }
}
