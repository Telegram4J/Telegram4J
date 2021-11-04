package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.json.VoiceData;

import java.util.Objects;
import java.util.Optional;

public class Voice implements Attachment {

    private final TelegramClient client;
    private final VoiceData data;

    public Voice(TelegramClient client, VoiceData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public VoiceData getData() {
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
        Voice that = (Voice) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "Voice{data=" + data + '}';
    }
}
