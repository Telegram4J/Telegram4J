package telegram4j.core.object;

import reactor.util.annotation.Nullable;

import java.util.Objects;
import java.util.Optional;

/** Geo point representation. */
public final class GeoPoint {

    private final telegram4j.tl.BaseGeoPoint data;

    public GeoPoint(telegram4j.tl.BaseGeoPoint data) {
        this.data = Objects.requireNonNull(data);
    }

    /**
     * Gets longitude coordinates of geo point.
     *
     * @return The longitude coordinates of geo point.
     */
    public double getLongitude() {
        return data.longitude();
    }

    /**
     * Gets latitude coordinates of geo point.
     *
     * @return The latitude coordinates of geo point.
     */
    public double getLatitude() {
        return data.lat();
    }

    /**
     * Gets access hash for geo point.
     *
     * @return The access hash for geo point.
     */
    public long getAccessHash() {
        return data.accessHash();
    }

    /**
     * Gets estimated horizontal accuracy of the location, in meters; defined by the sender.
     *
     * @return The estimated horizontal accuracy of the location, in meters; defined by the sender.
     */
    public Optional<Integer> getAccuracyRadius() {
        return Optional.ofNullable(data.accuracyRadius());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeoPoint geoPoint = (GeoPoint) o;
        return data.equals(geoPoint.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "GeoPoint{" +
                "data=" + data +
                '}';
    }
}
