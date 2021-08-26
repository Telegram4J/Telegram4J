package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.StickerData;

import java.util.Objects;
import java.util.Optional;

public class Sticker implements Attachment {

    private final TelegramClient client;
    private final StickerData data;

    public Sticker(TelegramClient client, StickerData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public StickerData getData() {
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

    public boolean isAnimated() {
        return data.isAnimated();
    }

    public Optional<PhotoSize> getThumb() {
        return data.thumb().map(data -> new PhotoSize(client, data));
    }

    public Optional<String> getEmoji() {
        return data.emoji();
    }

    public Optional<String> getSetName() {
        return data.setName();
    }

    public Optional<String> getMaskPosition() {
        return data.maskPosition();
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
        Sticker that = (Sticker) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        return "Sticker{data=" + data + '}';
    }
}
