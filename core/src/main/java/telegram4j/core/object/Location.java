package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.json.LocationData;

import java.util.Objects;
import java.util.Optional;

public class Location implements TelegramObject {

    private final TelegramClient client;
    private final LocationData data;

    public Location(TelegramClient client, LocationData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public LocationData getData() {
        return data;
    }

    public float getLongitude() {
        return data.longitude();
    }

    public float getLatitude() {
        return data.latitude();
    }

    public Optional<Float> getHorizontalAccuracy() {
        return data.horizontalAccuracy();
    }

    public Optional<Integer> getLivePeriod() {
        return data.livePeriod();
    }

    public Optional<Integer> getHeading() {
        return data.heading();
    }

    public Optional<Integer> getProximityAlertRadius() {
        return data.proximityAlertRadius();
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location that = (Location) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "Location{data=" + data + '}';
    }
}
