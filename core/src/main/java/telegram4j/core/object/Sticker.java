/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package telegram4j.core.object;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.auxiliary.AuxiliaryStickerSet;
import telegram4j.core.internal.MappingUtil;
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
public final class Sticker extends Document {

    private final Variant2<DocumentAttributeSticker, DocumentAttributeCustomEmoji> stickerData;
    private final Variant2<DocumentAttributeImageSize, DocumentAttributeVideo> optData;

    public Sticker(MTProtoTelegramClient client, BaseDocument data, @Nullable String fileName,
                   Context context, Variant2<DocumentAttributeSticker, DocumentAttributeCustomEmoji> stickerData,
                   Variant2<DocumentAttributeImageSize, DocumentAttributeVideo> optData) {
        super(client, data, fileName, context);
        this.stickerData = Objects.requireNonNull(stickerData);
        this.optData = Objects.requireNonNull(optData);
    }

    public Sticker(MTProtoTelegramClient client, WebDocument data, @Nullable String fileName,
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

    public boolean isTextColor() {
        return stickerData.map(d -> false, DocumentAttributeCustomEmoji::textColor);
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
        return optData.getT2().map(d -> MappingUtil.durationFromSeconds(d.duration()));
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
    public InputStickerSet getStickerSetId() {
        return stickerData.map(DocumentAttributeSticker::stickerset, DocumentAttributeCustomEmoji::stickerset);
    }

    /**
     * Requests to retrieve full sticker set of sticker.
     *
     * @return A {@link Mono} emitting on successful completion {@link AuxiliaryStickerSet full sticker set info}.
     */
    public Mono<AuxiliaryStickerSet> getStickerSet() {
        return client.getStickerSet(getStickerSetId());
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
            return switch (mimeType.toLowerCase(Locale.US)) {
                case "image/png", "image/webp" -> STATIC;
                case "video/webm" -> VIDEO;
                case "application/x-tgsticker" -> ANIMATED;
                default -> throw new IllegalStateException("Unexpected mime type: '" + mimeType + '\'');
            };
        }
    }
}
