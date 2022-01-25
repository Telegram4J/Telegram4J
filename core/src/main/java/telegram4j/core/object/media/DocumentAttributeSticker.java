package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.InputStickerSet;

import java.util.Objects;
import java.util.Optional;

public class DocumentAttributeSticker extends BaseDocumentAttribute {

    private final telegram4j.tl.DocumentAttributeSticker data;

    public DocumentAttributeSticker(MTProtoTelegramClient client, telegram4j.tl.DocumentAttributeSticker data) {
        super(client, Type.STICKER);
        this.data = Objects.requireNonNull(data, "data");
    }

    public boolean isMask() {
        return data.mask();
    }

    public String getAlternative() {
        return data.alt();
    }

    // TODO: mapping for this object
    public InputStickerSet getStickerSet() {
        return data.stickerset();
    }

    public Optional<MaskCoordinates> getMaskCoordinates() {
        return Optional.ofNullable(data.maskCoords()).map(d -> new MaskCoordinates(client, d));
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DocumentAttributeSticker that = (DocumentAttributeSticker) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), data);
    }

    @Override
    public String toString() {
        return "DocumentAttributeSticker{" +
                "data=" + data +
                "} " + super.toString();
    }
}
