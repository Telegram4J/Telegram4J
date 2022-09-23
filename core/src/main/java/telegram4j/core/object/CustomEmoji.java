package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.util.Variant2;
import telegram4j.tl.*;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Inferred from {@link BaseDocumentFields#attributes()} type for all types of custom emoji documents.
 * The {@link #getFileName() file name} will always be available.
 */
public class CustomEmoji extends Document {
    // TODO: DocumentAttributeVideo#supportsStreaming is always true, but I need to re-check this

    private final DocumentAttributeCustomEmoji emojiData;
    private final Variant2<DocumentAttributeImageSize, DocumentAttributeVideo> optData;

    public CustomEmoji(MTProtoTelegramClient client, BaseDocumentFields data, @Nullable String fileName, int messageId,
                       InputPeer peer, DocumentAttributeCustomEmoji emojiData,
                       Variant2<DocumentAttributeImageSize, DocumentAttributeVideo> optData) {
        super(client, data, fileName, messageId, peer);
        this.emojiData = Objects.requireNonNull(emojiData);
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
     * Gets duration of video emoji, if {@link #getType()} is {@link Sticker.Type#VIDEO}.
     *
     * @return The duration of video emoji, if {@link #getType()} is {@link Sticker.Type#VIDEO}
     */
    public Optional<Duration> getDuration() {
        return optData.getT2().map(d -> Duration.ofSeconds(d.duration()));
    }

    public boolean isFree() {
        return emojiData.free();
    }

    /**
     * Gets alternative unicode emoji representation for emoji.
     *
     * @return The alternative unicode emoji representation.
     */
    public String getAlternative() {
        return emojiData.alt();
    }

    /**
     * Gets id of sticker set where this sticker is placed.
     *
     * @return The {@link InputStickerSet} id of sticker set.
     */
    public InputStickerSet getStickerSet() {
        return emojiData.stickerset();
    }

    @Override
    public String toString() {
        return "CustomEmoji{" +
                "data=" + data +
                ", fileReferenceId=" + fileReferenceId +
                '}';
    }
}
