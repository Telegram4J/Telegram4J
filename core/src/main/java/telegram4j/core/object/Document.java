package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.DocumentData;

import java.util.Objects;
import java.util.Optional;

public class Document implements Attachment {

    private final TelegramClient client;
    private final DocumentData data;

    public Document(TelegramClient client, DocumentData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public DocumentData getData() {
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
        Document that = (Document) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        return "Document{data=" + data + '}';
    }
}
