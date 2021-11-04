package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.json.VoiceChatEndedData;

import java.util.Objects;

public class VoiceChatEnded implements TelegramObject {

    private final TelegramClient client;
    private final VoiceChatEndedData data;

    public VoiceChatEnded(TelegramClient client, VoiceChatEndedData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public VoiceChatEndedData getData() {
        return data;
    }

    public int getDuration() {
        return data.duration();
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VoiceChatEnded that = (VoiceChatEnded) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "VoiceChatEnded{data=" + data + '}';
    }
}
