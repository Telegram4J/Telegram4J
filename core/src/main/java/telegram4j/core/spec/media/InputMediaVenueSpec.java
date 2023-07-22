package telegram4j.core.spec.media;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.BaseInputGeoPoint;
import telegram4j.tl.ImmutableInputMediaVenue;
import telegram4j.tl.InputMedia;
import telegram4j.tl.InputMediaVenue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class InputMediaVenueSpec implements InputMediaSpec {
    private final double latitude;
    private final double longitude;
    private final Integer accuracyRadius;
    private final String title;
    private final String address;
    private final String provider;
    private final String venueId;
    private final String venueType;

    private InputMediaVenueSpec(double latitude, double longitude, String title,
                                String address, String provider, String venueId, String venueType) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.title = Objects.requireNonNull(title);
        this.address = Objects.requireNonNull(address);
        this.provider = Objects.requireNonNull(provider);
        this.venueId = Objects.requireNonNull(venueId);
        this.venueType = Objects.requireNonNull(venueType);
        this.accuracyRadius = null;
    }

    private InputMediaVenueSpec(double latitude, double longitude,
                                @Nullable Integer accuracyRadius, String title, String address,
                                String provider, String venueId, String venueType) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyRadius = accuracyRadius;
        this.title = title;
        this.address = address;
        this.provider = provider;
        this.venueId = venueId;
        this.venueType = venueType;
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

    public String title() {
        return title;
    }

    public String address() {
        return address;
    }

    public String provider() {
        return provider;
    }

    public String venueId() {
        return venueId;
    }

    public String venueType() {
        return venueType;
    }

    @Override
    public Mono<ImmutableInputMediaVenue> resolve(MTProtoTelegramClient client) {
        return Mono.just(InputMediaVenue.builder()
                .geoPoint(BaseInputGeoPoint.builder()
                        .lat(latitude)
                        .longitude(longitude)
                        .accuracyRadius(accuracyRadius)
                        .build())
                .title(title)
                .address(address)
                .provider(provider)
                .venueId(venueId)
                .venueType(venueType)
                .build());
    }

    public InputMediaVenueSpec withLatitude(double value) {
        if (Double.doubleToLongBits(latitude) == Double.doubleToLongBits(value)) return this;
        return new InputMediaVenueSpec(value, longitude, accuracyRadius, title, address, provider, venueId, venueType);
    }

    public InputMediaVenueSpec withLongitude(double value) {
        if (Double.doubleToLongBits(longitude) == Double.doubleToLongBits(value)) return this;
        return new InputMediaVenueSpec(latitude, value, accuracyRadius, title, address, provider, venueId, venueType);
    }

    public InputMediaVenueSpec withAccuracyRadius(@Nullable Integer value) {
        if (Objects.equals(accuracyRadius, value)) return this;
        return new InputMediaVenueSpec(latitude, longitude, value, title, address, provider, venueId, venueType);
    }

    public InputMediaVenueSpec withAccuracyRadius(Optional<Integer> opt) {
        return withAccuracyRadius(opt.orElse(null));
    }

    public InputMediaVenueSpec withTitle(String value) {
        Objects.requireNonNull(value);
        if (title.equals(value)) return this;
        return new InputMediaVenueSpec(latitude, longitude, accuracyRadius, value, address, provider, venueId, venueType);
    }

    public InputMediaVenueSpec withAddress(String value) {
        Objects.requireNonNull(value);
        if (address.equals(value)) return this;
        return new InputMediaVenueSpec(latitude, longitude, accuracyRadius, title, value, provider, venueId, venueType);
    }

    public InputMediaVenueSpec withProvider(String value) {
        Objects.requireNonNull(value);
        if (provider.equals(value)) return this;
        return new InputMediaVenueSpec(latitude, longitude, accuracyRadius, title, address, value, venueId, venueType);
    }

    public InputMediaVenueSpec withVenueId(String value) {
        Objects.requireNonNull(value);
        if (venueId.equals(value)) return this;
        return new InputMediaVenueSpec(latitude, longitude, accuracyRadius, title, address, provider, value, venueType);
    }

    public InputMediaVenueSpec withVenueType(String value) {
        Objects.requireNonNull(value);
        if (venueType.equals(value)) return this;
        return new InputMediaVenueSpec(latitude, longitude, accuracyRadius, title, address, provider, venueId, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InputMediaVenueSpec that)) return false;
        return Double.doubleToLongBits(latitude) == Double.doubleToLongBits(that.latitude)
                && Double.doubleToLongBits(longitude) == Double.doubleToLongBits(that.longitude)
                && Objects.equals(accuracyRadius, that.accuracyRadius)
                && title.equals(that.title)
                && address.equals(that.address)
                && provider.equals(that.provider)
                && venueId.equals(that.venueId)
                && venueType.equals(that.venueType);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Double.hashCode(latitude);
        h += (h << 5) + Double.hashCode(longitude);
        h += (h << 5) + Objects.hashCode(accuracyRadius);
        h += (h << 5) + title.hashCode();
        h += (h << 5) + address.hashCode();
        h += (h << 5) + provider.hashCode();
        h += (h << 5) + venueId.hashCode();
        h += (h << 5) + venueType.hashCode();
        return h;
    }

    @Override
    public String toString() {
        return "InputMediaVenueSpec{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", accuracyRadius=" + accuracyRadius +
                ", title='" + title + '\'' +
                ", address='" + address + '\'' +
                ", provider='" + provider + '\'' +
                ", venueId='" + venueId + '\'' +
                ", venueType='" + venueType + '\'' +
                '}';
    }

    public static InputMediaVenueSpec of(double latitude, double longitude, String title,
                                         String address, String provider, String venueId, String venueType) {
        return new InputMediaVenueSpec(latitude, longitude, title, address, provider, venueId, venueType);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private static final byte INIT_BIT_LATITUDE = 0x1;
        private static final byte INIT_BIT_LONGITUDE = 0x2;
        private static final byte INIT_BIT_TITLE = 0x4;
        private static final byte INIT_BIT_ADDRESS = 0x8;
        private static final byte INIT_BIT_PROVIDER = 0x10;
        private static final byte INIT_BIT_VENUE_ID = 0x20;
        private static final byte INIT_BIT_VENUE_TYPE = 0x40;
        private byte initBits = 0b1111111;

        private double latitude;
        private double longitude;
        private Integer accuracyRadius;
        private String title;
        private String address;
        private String provider;
        private String venueId;
        private String venueType;

        private Builder() {
        }

        public Builder from(InputMediaVenueSpec instance) {
            Objects.requireNonNull(instance);
            latitude(instance.latitude);
            longitude(instance.longitude);
            accuracyRadius(instance.accuracyRadius);
            title(instance.title);
            address(instance.address);
            provider(instance.provider);
            venueId(instance.venueId);
            venueType(instance.venueType);
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

        public Builder title(String title) {
            this.title = Objects.requireNonNull(title);
            initBits &= ~INIT_BIT_TITLE;
            return this;
        }

        public Builder address(String address) {
            this.address = Objects.requireNonNull(address);
            initBits &= ~INIT_BIT_ADDRESS;
            return this;
        }

        public Builder provider(String provider) {
            this.provider = Objects.requireNonNull(provider);
            initBits &= ~INIT_BIT_PROVIDER;
            return this;
        }

        public Builder venueId(String venueId) {
            this.venueId = Objects.requireNonNull(venueId);
            initBits &= ~INIT_BIT_VENUE_ID;
            return this;
        }

        public Builder venueType(String venueType) {
            this.venueType = Objects.requireNonNull(venueType);
            initBits &= ~INIT_BIT_VENUE_TYPE;
            return this;
        }

        public InputMediaVenueSpec build() {
            if (initBits != 0) {
                throw incompleteInitialization();
            }
            return new InputMediaVenueSpec(latitude, longitude, accuracyRadius, title, address, provider, venueId, venueType);
        }

        private IllegalStateException incompleteInitialization() {
            List<String> attributes = new ArrayList<>();
            if ((initBits & INIT_BIT_LATITUDE) != 0) attributes.add("latitude");
            if ((initBits & INIT_BIT_LONGITUDE) != 0) attributes.add("longitude");
            if ((initBits & INIT_BIT_TITLE) != 0) attributes.add("title");
            if ((initBits & INIT_BIT_ADDRESS) != 0) attributes.add("address");
            if ((initBits & INIT_BIT_PROVIDER) != 0) attributes.add("provider");
            if ((initBits & INIT_BIT_VENUE_ID) != 0) attributes.add("venueId");
            if ((initBits & INIT_BIT_VENUE_TYPE) != 0) attributes.add("venueType");
            return new IllegalStateException("Cannot build InputMediaVenueSpec, some of required attributes are not set " + attributes);
        }
    }
}
