package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.json.VideoData;

import java.util.Objects;
import java.util.Optional;

public class Video implements Attachment {

    private final TelegramClient client;
    private final VideoData data;

    public Video(TelegramClient client, VideoData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public VideoData getData() {
        return data;
    }

    @Override
    public String getFileId() {
        return data.fileId();
    }

    @Override
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
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Video that = (Video) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "Video{data=" + data + '}';
    }
}
