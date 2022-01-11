package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;

public class DocumentAttributeImageSize extends BaseDocumentAttribute {

    private final telegram4j.tl.DocumentAttributeImageSize data;

    public DocumentAttributeImageSize(MTProtoTelegramClient client, telegram4j.tl.DocumentAttributeImageSize data) {
        super(client, Type.IMAGE_SIZE);
        this.data = Objects.requireNonNull(data, "data");
    }

    public int getWight() {
        return data.w();
    }

    public int getHeight() {
        return data.h();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DocumentAttributeImageSize that = (DocumentAttributeImageSize) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), data);
    }

    @Override
    public String toString() {
        return "DocumentAttributeImageSize{" +
                "data=" + data +
                "} " + super.toString();
    }
}
