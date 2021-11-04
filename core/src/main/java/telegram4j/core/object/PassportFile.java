package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.json.PassportFileData;

import java.time.Instant;
import java.util.Objects;

public class PassportFile implements Attachment {

    private final TelegramClient client;
    private final PassportFileData data;

    public PassportFile(TelegramClient client, PassportFileData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public PassportFileData getData() {
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

    public int getFileSize() {
        return data.fileSize().orElseThrow(IllegalStateException::new); // always present
    }

    public Instant getFileTimestamp() {
        return Instant.ofEpochSecond(data.fileDate());
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PassportFile that = (PassportFile) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "PassportFile{data=" + data + '}';
    }
}
