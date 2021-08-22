package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.ProximityAlertTriggeredData;

import java.util.Objects;

public class ProximityAlertTriggered implements TelegramObject {

    private final TelegramClient client;
    private final ProximityAlertTriggeredData data;

    public ProximityAlertTriggered(TelegramClient client, ProximityAlertTriggeredData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public ProximityAlertTriggeredData getData() {
        return data;
    }

    public User getTraveler() {
        return new User(client, data.traveler());
    }

    public User getWatcher() {
        return new User(client, data.watcher());
    }

    public int getDistance() {
        return data.distance();
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProximityAlertTriggered that = (ProximityAlertTriggered) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        return "ProximityAlertTriggered{data=" + data + '}';
    }
}
