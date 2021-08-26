package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.json.AudioData;

import java.util.Objects;
import java.util.Optional;

public class Audio implements Attachment {

    private final TelegramClient client;
    private final AudioData data;

    public Audio(TelegramClient client, AudioData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public AudioData getData() {
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

    public int getDuration() {
        return data.duration();
    }

    public Optional<String> getPerformer() {
        return data.performer();
    }

    public Optional<String> getTitle() {
        return data.title();
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

    public Optional<PhotoSize> getThumb() {
        return data.thumb().map(data -> new PhotoSize(client, data));
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Audio that = (Audio) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        return "Audio{data=" + data + '}';
    }
}
