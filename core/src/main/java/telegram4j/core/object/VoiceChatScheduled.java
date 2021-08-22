package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.VoiceChatScheduledData;

import java.time.Instant;
import java.util.Objects;

public class VoiceChatScheduled implements TelegramObject {

    private final TelegramClient client;
    private final VoiceChatScheduledData data;

    public VoiceChatScheduled(TelegramClient client, VoiceChatScheduledData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public VoiceChatScheduledData getData() {
        return data;
    }

    public Instant getStartTimestamp() {
        return Instant.ofEpochSecond(data.startDate());
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VoiceChatScheduled that = (VoiceChatScheduled) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        return "VoiceChatScheduled{data=" + data + '}';
    }
}
