package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;
import java.util.Optional;

public class GeoPoint implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.BaseGeoPoint data;

    public GeoPoint(MTProtoTelegramClient client, telegram4j.tl.BaseGeoPoint data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public double getLongitude() {
        return data.longState();
    }

    public double getLatitude() {
        return data.lat();
    }

    public long getAccessHash() {
        return data.accessHash();
    }

    public Optional<Integer> getAccuracyRadius() {
        return Optional.ofNullable(data.accuracyRadius());
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
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
