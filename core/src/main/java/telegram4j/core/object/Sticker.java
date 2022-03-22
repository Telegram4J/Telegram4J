package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.media.MaskCoordinates;
import telegram4j.core.spec.IdFields;
import telegram4j.tl.BaseDocumentFields;
import telegram4j.tl.DocumentAttributeImageSize;
import telegram4j.tl.DocumentAttributeSticker;
import telegram4j.tl.InputPeer;

import java.util.Optional;

public class Sticker extends Document {

    private final DocumentAttributeSticker stickerData;
    private final DocumentAttributeImageSize sizeData;

    public Sticker(MTProtoTelegramClient client, BaseDocumentFields data, @Nullable String fileName, int messageId,
                   InputPeer peer, DocumentAttributeSticker stickerData, DocumentAttributeImageSize sizeData) {
        super(client, data, fileName, messageId, peer);
        this.stickerData = stickerData;
        this.sizeData = sizeData;
    }

    public int getWeight() {
        return sizeData.w();
    }

    public int getHeight() {
        return sizeData.h();
    }

    public boolean isMask() {
        return stickerData.mask();
    }

    public String getAlternative() {
        return stickerData.alt();
    }

    public IdFields.StickerSetId getStickerSet() {
        return IdFields.StickerSetId.from(stickerData.stickerset());
    }

    public Optional<MaskCoordinates> getMaskCoordinates() {
        return Optional.ofNullable(stickerData.maskCoords())
                .map(d -> new MaskCoordinates(getClient(), d));
    }

    @Override
    public String toString() {
        return "Sticker{" +
                "stickerData=" + stickerData +
                ", sizeData=" + sizeData +
                "} " + super.toString();
    }
}
