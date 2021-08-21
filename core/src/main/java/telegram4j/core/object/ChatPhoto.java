package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.TelegramClient;
import telegram4j.json.ChatPhotoData;

import java.util.Objects;

public class ChatPhoto implements TelegramObject {

    private final TelegramClient client;
    private final ChatPhotoData data;

    public ChatPhoto(TelegramClient client, ChatPhotoData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public String getSmallFileId() {
        return data.smallFileId();
    }

    public String getSmallFileUniqueId() {
        return data.smallFileUniqueId();
    }

    public String getBigFileId() {
        return data.bigFileId();
    }

    public String getBigFileUniqueId() {
        return data.bigFileUniqueId();
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatPhoto that = (ChatPhoto) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        return "ChatPhoto{data=" + data + '}';
    }
}
