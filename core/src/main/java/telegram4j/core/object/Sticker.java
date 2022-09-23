package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.media.MaskCoordinates;
import telegram4j.core.util.Variant2;
import telegram4j.tl.*;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Inferred from {@link BaseDocumentFields#attributes()} type for all types of sticker documents.
 * The {@link #getFileName() file name} will always be available.
 */
public class Sticker extends Document {

    private final DocumentAttributeSticker stickerData;
    private final Variant2<DocumentAttributeImageSize, DocumentAttributeVideo> optData;

    public Sticker(MTProtoTelegramClient client, BaseDocumentFields data, @Nullable String fileName, int messageId,
                   InputPeer peer, DocumentAttributeSticker stickerData,
                   Variant2<DocumentAttributeImageSize, DocumentAttributeVideo> optData) {
        super(client, data, fileName, messageId, peer);
        this.stickerData = Objects.requireNonNull(stickerData);
        this.optData = Objects.requireNonNull(optData);
    }

    /**
     * Gets type of emoji.
     *
     * @return The {@link Sticker.Type} of emoji.
     */
    public Sticker.Type getType() {
        return Sticker.Type.fromMimeType(getMimeType());
    }

    /**
     * Gets width of sticker.
     *
     * @return The width of sticker.
     */
    public int getWidth() {
        return optData.map(DocumentAttributeImageSize::w, DocumentAttributeVideo::w);
    }

    /**
     * Gets height of sticker.
     *
     * @return The height of sticker.
     */
    public int getHeight() {
        return optData.map(DocumentAttributeImageSize::h, DocumentAttributeVideo::h);
    }

    /**
     * Gets duration of video sticker, if {@link #getType()} is {@link Sticker.Type#VIDEO}.
     *
     * @return The duration of video sticker, if {@link #getType()} is {@link Sticker.Type#VIDEO}
     */
    public Optional<Duration> getDuration() {
        return optData.getT2().map(d -> Duration.ofSeconds(d.duration()));
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
     * @return The {@link InputStickerSet} id of sticker set.
     */
    public InputStickerSet getStickerSet() {
        return stickerData.stickerset();
    }

    /**
     * Gets mask coordinates, if {@link #isMask()} is {@code true}.
     *
     * @return The mask coordinates, if {@link #isMask()} is {@code true}.
     */
    public Optional<MaskCoordinates> getMaskCoordinates() {
        return Optional.ofNullable(stickerData.maskCoords()).map(MaskCoordinates::new);
    }

    @Override
    public String toString() {
        return "Sticker{" +
                "data=" + data +
                ", fileReferenceId=" + fileReferenceId +
                '}';
    }

    /** Types of sticker set elements. */
    public enum Type {
        /** Represents static image sticker and emoji. */
        STATIC,

        /** Represents vector-animated sticker and emoji. */
        ANIMATED,

        /** Represents video sticker and emoji. */
        VIDEO;

        public static Type fromMimeType(String mimeType) {
            switch (mimeType.toLowerCase(Locale.US)) {
                case "image/png":
                case "image/webp": return STATIC;
                case "video/webm": return VIDEO;
                case "application/x-tgsticker": return ANIMATED;
                default: throw new IllegalStateException("Unexpected mime type: '" + mimeType + '\'');
            }
        }
    }
}
