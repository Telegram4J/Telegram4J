package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.AnimationData;

import java.util.Objects;
import java.util.Optional;

public class Animation implements TelegramObject {

    private final TelegramClient client;
    private final AnimationData data;

    public Animation(TelegramClient client, AnimationData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public AnimationData getData() {
        return data;
    }

    public String getFileId() {
        return data.fileId();
    }

    public String getFileUniqueId() {
        return data.fileUniqueId();
    }

    public int getWidth() {
        return data.width();
    }

    public int getHeight() {
        return data.height();
    }

    public int getDuration() {
        return data.duration();
    }

    public Optional<PhotoSize> getThumb() {
        return data.thumb().map(data -> new PhotoSize(client, data));
    }

    public Optional<String> getFileName() {
        return data.fileName();
    }

    public Optional<String> getMimeType() {
        return data.mimeType();
    }

    public Optional<Integer> getFileSize() {
        return data.fileSize();
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Animation that = (Animation) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        return "Animation{data=" + data + '}';
    }
}
