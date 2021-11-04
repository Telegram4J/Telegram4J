package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.json.PhotoSizeData;

import java.util.Objects;
import java.util.Optional;

public class PhotoSize implements Attachment {

    private final TelegramClient client;
    private final PhotoSizeData data;

    public PhotoSize(TelegramClient client, PhotoSizeData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public PhotoSizeData getData() {
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
        PhotoSize that = (PhotoSize) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "PhotoSize{data=" + data + '}';
    }
}
