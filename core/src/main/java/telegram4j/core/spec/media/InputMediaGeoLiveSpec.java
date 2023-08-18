/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package telegram4j.core.spec.media;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.BaseInputGeoPoint;
import telegram4j.tl.ImmutableInputMediaGeoLive;
import telegram4j.tl.InputMedia;
import telegram4j.tl.InputMediaGeoLive;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class InputMediaGeoLiveSpec implements InputMediaSpec {
    private final boolean stopped;
    private final double latitude;
    private final double longitude;
    private final Integer accuracyRadius;
    private final Integer heading;
    private final Integer period;
    private final Integer proximityNotificationRadius;

    private InputMediaGeoLiveSpec(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyRadius = null;
        this.heading = null;
        this.period = null;
        this.proximityNotificationRadius = null;
        this.stopped = false;
    }

    private InputMediaGeoLiveSpec(Builder builder) {
        this.latitude = builder.latitude;
        this.longitude = builder.longitude;
        this.accuracyRadius = builder.accuracyRadius;
        this.heading = builder.heading;
        this.period = builder.period;
        this.proximityNotificationRadius = builder.proximityNotificationRadius;
        this.stopped = builder.stopped;
    }

    private InputMediaGeoLiveSpec(boolean stopped, double latitude, double longitude,
                                  @Nullable Integer accuracyRadius, @Nullable Integer heading,
                                  @Nullable Integer period, @Nullable Integer proximityNotificationRadius) {
        this.stopped = stopped;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyRadius = accuracyRadius;
        this.heading = heading;
        this.period = period;
        this.proximityNotificationRadius = proximityNotificationRadius;
    }

    public boolean stopped() {
        return stopped;
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

    public Optional<Integer> heading() {
        return Optional.ofNullable(heading);
    }

    public Optional<Integer> period() {
        return Optional.ofNullable(period);
    }

    public Optional<Integer> proximityNotificationRadius() {
        return Optional.ofNullable(proximityNotificationRadius);
    }

    @Override
    public Mono<ImmutableInputMediaGeoLive> resolve(MTProtoTelegramClient client) {
        return Mono.just(InputMediaGeoLive.builder()
                .geoPoint(BaseInputGeoPoint.builder()
                        .lat(latitude)
                        .longitude(longitude)
                        .accuracyRadius(accuracyRadius)
                        .build())
                .heading(heading)
                .heading(period)
                .heading(proximityNotificationRadius)
                .build());
    }

    public InputMediaGeoLiveSpec withStopped(boolean value) {
        if (stopped == value) return this;
        return new InputMediaGeoLiveSpec(value, latitude, longitude, accuracyRadius, heading, period, proximityNotificationRadius);
    }

    public InputMediaGeoLiveSpec withLatitude(double value) {
        if (Double.doubleToLongBits(latitude) == Double.doubleToLongBits(value)) return this;
        return new InputMediaGeoLiveSpec(stopped, value, longitude, accuracyRadius, heading, period, proximityNotificationRadius);
    }

    public InputMediaGeoLiveSpec withLongitude(double value) {
        if (Double.doubleToLongBits(longitude) == Double.doubleToLongBits(value)) return this;
        return new InputMediaGeoLiveSpec(stopped, latitude, value, accuracyRadius, heading, period, proximityNotificationRadius);
    }

    public InputMediaGeoLiveSpec withAccuracyRadius(@Nullable Integer value) {
        if (Objects.equals(accuracyRadius, value)) return this;
        return new InputMediaGeoLiveSpec(stopped, latitude, longitude, value, heading, period, proximityNotificationRadius);
    }

    public InputMediaGeoLiveSpec withAccuracyRadius(Optional<Integer> optional) {
        return withAccuracyRadius(optional.orElse(null));
    }

    public InputMediaGeoLiveSpec withHeading(@Nullable Integer value) {
        if (Objects.equals(heading, value)) return this;
        return new InputMediaGeoLiveSpec(stopped, latitude, longitude, accuracyRadius, value, period, proximityNotificationRadius);
    }

    public InputMediaGeoLiveSpec withHeading(Optional<Integer> opt) {
        return withHeading(opt.orElse(null));
    }

    public InputMediaGeoLiveSpec withPeriod(@Nullable Integer value) {
        if (Objects.equals(period, value)) return this;
        return new InputMediaGeoLiveSpec(stopped, latitude, longitude, accuracyRadius, heading, value, proximityNotificationRadius);
    }

    public InputMediaGeoLiveSpec withPeriod(Optional<Integer> opt) {
        return withPeriod(opt.orElse(null));
    }

    public InputMediaGeoLiveSpec withProximityNotificationRadius(@Nullable Integer value) {
        if (Objects.equals(proximityNotificationRadius, value)) return this;
        return new InputMediaGeoLiveSpec(stopped, latitude, longitude, accuracyRadius, heading, period, value);
    }

    public InputMediaGeoLiveSpec withProximityNotificationRadius(Optional<Integer> opt) {
        return withProximityNotificationRadius(opt.orElse(null));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InputMediaGeoLiveSpec that)) return false;
        return stopped == that.stopped
                && Double.doubleToLongBits(latitude) == Double.doubleToLongBits(that.latitude)
                && Double.doubleToLongBits(longitude) == Double.doubleToLongBits(that.longitude)
                && Objects.equals(accuracyRadius, that.accuracyRadius)
                && Objects.equals(heading, that.heading)
                && Objects.equals(period, that.period)
                && Objects.equals(proximityNotificationRadius, that.proximityNotificationRadius);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Boolean.hashCode(stopped);
        h += (h << 5) + Double.hashCode(latitude);
        h += (h << 5) + Double.hashCode(longitude);
        h += (h << 5) + Objects.hashCode(accuracyRadius);
        h += (h << 5) + Objects.hashCode(heading);
        h += (h << 5) + Objects.hashCode(period);
        h += (h << 5) + Objects.hashCode(proximityNotificationRadius);
        return h;
    }

    @Override
    public String toString() {
        return "InputMediaGeoLiveSpec{" +
                "stopped=" + stopped +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", accuracyRadius=" + accuracyRadius +
                ", heading=" + heading +
                ", period=" + period +
                ", proximityNotificationRadius=" + proximityNotificationRadius +
                '}';
    }

    public static InputMediaGeoLiveSpec of(double latitude, double longitude) {
        return new InputMediaGeoLiveSpec(latitude, longitude);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private static final byte INIT_BIT_LATITUDE = 0x1;
        private static final byte INIT_BIT_LONGITUDE = 0x2;
        private byte initBits = 0x3;

        private boolean stopped;
        private double latitude;
        private double longitude;
        private Integer accuracyRadius;
        private Integer heading;
        private Integer period;
        private Integer proximityNotificationRadius;

        private Builder() {
        }

        public Builder from(InputMediaGeoLiveSpec instance) {
            Objects.requireNonNull(instance);
            stopped(instance.stopped);
            latitude(instance.latitude);
            longitude(instance.longitude);
            accuracyRadius(instance.accuracyRadius);
            heading(instance.heading);
            period(instance.period);
            proximityNotificationRadius(instance.proximityNotificationRadius);
            return this;
        }

        public Builder stopped(boolean stopped) {
            this.stopped = stopped;
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

        public Builder heading(@Nullable Integer heading) {
            this.heading = heading;
            return this;
        }

        public Builder heading(Optional<Integer> heading) {
            this.heading = heading.orElse(null);
            return this;
        }

        public Builder period(@Nullable Integer period) {
            this.period = period;
            return this;
        }

        public Builder period(Optional<Integer> period) {
            this.period = period.orElse(null);
            return this;
        }

        public Builder proximityNotificationRadius(@Nullable Integer proximityNotificationRadius) {
            this.proximityNotificationRadius = proximityNotificationRadius;
            return this;
        }

        public Builder proximityNotificationRadius(Optional<Integer> proximityNotificationRadius) {
            this.proximityNotificationRadius = proximityNotificationRadius.orElse(null);
            return this;
        }

        public InputMediaGeoLiveSpec build() {
            if (initBits != 0) {
                throw incompleteInitialization();
            }
            return new InputMediaGeoLiveSpec(this);
        }

        private IllegalStateException incompleteInitialization() {
            List<String> attributes = new ArrayList<>();
            if ((initBits & INIT_BIT_LATITUDE) != 0) attributes.add("latitude");
            if ((initBits & INIT_BIT_LONGITUDE) != 0) attributes.add("longitude");
            return new IllegalStateException("Cannot build InputMediaGeoLiveSpec, some of required attributes are not set " + attributes);
        }
    }
}
