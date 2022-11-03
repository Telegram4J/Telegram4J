package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.media.MaskCoordinates;
import telegram4j.core.util.Variant2;
import telegram4j.mtproto.file.Context;
import telegram4j.tl.*;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Representation of all types of stickers and custom emojis.
 * The {@link #getFileName() file name} will always be available.
 */
public class Sticker extends Document {

    private final Variant2<DocumentAttributeSticker, DocumentAttributeCustomEmoji> stickerData;
    private final Variant2<DocumentAttributeImageSize, DocumentAttributeVideo> optData;

    public Sticker(MTProtoTelegramClient client, BaseDocumentFields data, @Nullable String fileName,
                   Context context, Variant2<DocumentAttributeSticker, DocumentAttributeCustomEmoji> stickerData,
                   Variant2<DocumentAttributeImageSize, DocumentAttributeVideo> optData) {
        super(client, data, fileName, context);
        this.stickerData = Objects.requireNonNull(stickerData);
        this.optData = Objects.requireNonNull(optData);
    }

    /**
     * Gets type of set which contains this sticker.
     *
     * @return The type of sticker set.
     */
    public StickerSet.Type getSetType() {
        return stickerData.map(d -> d.mask() ? StickerSet.Type.MASK
                : StickerSet.Type.REGULAR, d -> StickerSet.Type.CUSTOM_EMOJI);
    }

    /**
     * Gets type of emoji.
     *
     * @return The {@link Type} of emoji.
     */
    public Type getType() {
        return Type.fromMimeType(getMimeType());
    }

    // ???
    public boolean isFree() {
        return stickerData.map(d -> false, DocumentAttributeCustomEmoji::free);
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
     * Gets alternative unicode emoji representation for sticker.
     *
     * @return The alternative unicode emoji representation.
     */
    public String getAlternative() {
        return stickerData.map(DocumentAttributeSticker::alt, DocumentAttributeCustomEmoji::alt);
    }

    /**
     * Gets id of sticker set where this sticker is placed.
     *
     * @return The {@link InputStickerSet} id of sticker set.
     */
    public InputStickerSet getStickerSet() {
        return stickerData.map(DocumentAttributeSticker::stickerset, DocumentAttributeCustomEmoji::stickerset);
    }

    /**
     * Gets mask coordinates, if {@link #getSetType()} is {@link StickerSet.Type#MASK}.
     *
     * @return The mask coordinates, if {@link #getSetType()} is {@link StickerSet.Type#MASK}.
     */
    public Optional<MaskCoordinates> getMaskCoordinates() {
        return stickerData.getT1()
                .map(DocumentAttributeSticker::maskCoords)
                .map(MaskCoordinates::new);
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
        /** Represents static image sticker and emoji or mask. */
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
