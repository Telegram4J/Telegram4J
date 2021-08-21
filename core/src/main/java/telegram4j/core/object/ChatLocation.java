package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.ChatLocationData;

import java.util.Objects;

public class ChatLocation implements TelegramObject {

    private final TelegramClient client;
    private final ChatLocationData data;

    public ChatLocation(TelegramClient client, ChatLocationData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public Location getLocation() {
        return new Location(client, data.location());
    }

    public String getAddress() {
        return data.address();
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatLocation that = (ChatLocation) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        return "ChatLocation{data=" + data + '}';
    }
}
