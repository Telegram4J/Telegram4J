package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.media.MaskCoordinates;
import telegram4j.core.spec.IdFields;
import telegram4j.tl.*;

import java.util.Objects;
import java.util.Optional;

/** Inferred from {@link BaseDocumentFields#attributes()} type for all types of sticker documents. */
public class Sticker extends Document {

    private final DocumentAttributeSticker stickerData;
    @Nullable
    private final DocumentAttributeImageSize sizeData;
    @Nullable
    private final DocumentAttributeVideo videoData;

    public Sticker(MTProtoTelegramClient client, BaseDocumentFields data, @Nullable String fileName, int messageId,
                   InputPeer peer, DocumentAttributeSticker stickerData, @Nullable DocumentAttributeImageSize sizeData,
                   @Nullable DocumentAttributeVideo videoData) {
        super(client, data, fileName, messageId, peer);
        this.stickerData = Objects.requireNonNull(stickerData, "stickerData");
        this.sizeData = sizeData;
        this.videoData = videoData;
    }

    /**
     * Gets width of sticker.
     *
     * @return The width of sticker.
     */
    public int getWidth() {
        if (sizeData != null) {
            return sizeData.w();
        }

        return Objects.requireNonNull(videoData).w();
    }

    /**
     * Gets height of sticker.
     *
     * @return The height of sticker.
     */
    public int getHeight() {
        if (sizeData != null) {
            return sizeData.w();
        }

        return Objects.requireNonNull(videoData).h();
    }

    /**
     * Gets whether this is a mask sticker
     *
     * @return {@code true} if this is a mask sticker.
     */
    public boolean isMask() {
        return stickerData.mask();
    }

    /**
     * Gets alternative unicode emoji representation for sticker.
     *
     * @return The alternative unicode emoji representation.
     */
    public String getAlternative() {
        return stickerData.alt();
    }

    /**
     * Gets id of sticker set where this sticker is placed.
     *
     * @return The {@link IdFields.StickerSetId} id of sticker set.
     */
    public IdFields.StickerSetId getStickerSet() {
        return IdFields.StickerSetId.from(stickerData.stickerset());
    }

    /**
     * Gets mask coordinates, if {@link #isMask()} is {@code true}.
     *
     * @return The mask coordinates, if {@link #isMask()} is {@code true}.
     */
    public Optional<MaskCoordinates> getMaskCoordinates() {
        return Optional.ofNullable(stickerData.maskCoords())
                .map(d -> new MaskCoordinates(getClient(), d));
    }

    @Override
    public String toString() {
        return "Sticker{" +
                "stickerData=" + stickerData +
                ", sizeData=" + sizeData +
                ", videoData=" + videoData +
                "} " + super.toString();
    }
}
