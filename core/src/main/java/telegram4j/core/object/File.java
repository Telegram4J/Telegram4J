package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.FileData;

import java.util.Objects;
import java.util.Optional;

public class File implements Attachment {

    private final TelegramClient client;
    private final FileData data;

    public File(TelegramClient client, FileData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public FileData getData() {
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

    public Optional<Integer> getFileSize() {
        return data.fileSize();
    }

    public Optional<String> getFilePath() {
        return data.filePath();
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        File that = (File) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        return "File{data=" + data + '}';
    }
}
