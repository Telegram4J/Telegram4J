package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;

public class DocumentAttributeFilename extends BaseDocumentAttribute {

    private final telegram4j.tl.DocumentAttributeFilename data;

    public DocumentAttributeFilename(MTProtoTelegramClient client, telegram4j.tl.DocumentAttributeFilename data) {
        super(client, Type.FILENAME);
        this.data = Objects.requireNonNull(data, "data");
    }

    public String getFileName() {
        return data.fileName();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentAttributeFilename that = (DocumentAttributeFilename) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "DocumentAttributeFilename{" +
                "data=" + data +
                '}';
    }
}
