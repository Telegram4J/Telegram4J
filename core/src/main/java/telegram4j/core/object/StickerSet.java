package telegram4j.core.object;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.auxiliary.AuxiliaryStickerSet;
import telegram4j.core.internal.EntityFactory;
import telegram4j.core.object.media.PhotoSize;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.ImmutableInputStickerSetID;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class StickerSet implements TelegramObject {
    private final MTProtoTelegramClient client;
    private final telegram4j.tl.StickerSet data;

    @Nullable
    private final FileReferenceId fileReferenceId;

    public StickerSet(MTProtoTelegramClient client, telegram4j.tl.StickerSet data) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);

        Integer ver = data.thumbVersion();
        this.fileReferenceId = ver != null ? FileReferenceId.ofStickerSet(
                ImmutableInputStickerSetID.of(data.id(), data.accessHash()), ver,
                Objects.requireNonNull(data.thumbDcId())) : null;
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets {@link FileReferenceId} for this sticker set, if {@link #getThumbVersion()} is present.
     *
     * @return The {@link FileReferenceId} for this sticker set, if {@link #getThumbVersion()} is present.
     */
    public Optional<FileReferenceId> getFileReferenceId() {
        return Optional.ofNullable(fileReferenceId);
    }

    /**
     * Gets type of sticker set.
     *
     * @return The {@link Type} of sticker set.
     */
    public Type getType() {
        return data.emojis() ? Type.CUSTOM_EMOJI :
                data.masks() ? Type.MASK : Type.REGULAR;
    }

    /**
     * Gets type of stickers contained in this sticker pack.
     *
     * @return The {@link Sticker.Type} of stickers.
     */
    public Sticker.Type getStickerType() {
        return data.animated() ? Sticker.Type.ANIMATED :
                data.videos() ? Sticker.Type.VIDEO : Sticker.Type.STATIC;
    }

    /**
     * Gets whether sticker set is archived.
     *
     * @return {@code true} if sticker set is archived.
     */
    public boolean isArchived() {
        return data.archived();
    }

    /**
     * Gets whether sticker set is created by Telegram.
     *
     * @return {@code true} if sticker set is created by Telegram.
     */
    public boolean isOfficial() {
        return data.official();
    }

    /**
     * Gets a timestamp when this sticker set was installed by the current user.
     *
     * @return The {@link Instant} when this sticker set was installed by the current user.
     */
    public Optional<Instant> getInstallTimestamp() {
        return Optional.ofNullable(data.installedDate()).map(Instant::ofEpochSecond);
    }

    /**
     * Gets id of sticker set.
     *
     * @return The id of sticker set.
     */
    public long getId() {
        return data.id();
    }

    /**
     * Gets access hash for sticker set.
     *
     * @return The access hash for sticker set.
     */
    public long getAccessHash() {
        return data.accessHash();
    }

    /**
     * Gets name of sticker set.
     *
     * @return The name of sticker set.
     */
    public String getTitle() {
        return data.title();
    }

    /**
     * Gets short name of sticker set.
     *
     * @return The short name of sticker set.
     */
    public String getShortName() {
        return data.shortName();
    }

    /**
     * Gets mutable list of sticker set thumbnails, if present.
     *
     * @return The mutable list of sticker set thumbnails, if present.
     */
    public Optional<List<PhotoSize>> getThumbs() {
        return Optional.ofNullable(data.thumbs())
                .map(l -> l.stream()
                        .map(EntityFactory::createPhotoSize)
                        .collect(Collectors.toList()));
    }

    /**
     * Gets id of datacenter which store thumbnail for this sticker set, if present.
     *
     * @return The id of datacenter which store thumbnail for this sticker set, if present.
     */
    public Optional<Integer> getThumbDcId() {
        return Optional.ofNullable(data.thumbDcId());
    }

    /**
     * Gets version of thumbnail for this sticker set, if present.
     *
     * @return The version of thumbnail for this sticker set, if present.
     */
    public Optional<Integer> getThumbVersion() {
        return Optional.ofNullable(data.thumbVersion());
    }

    /**
     * Gets id of {@link Sticker custom emoji} which used as sticker set thumbnail, if present.
     *
     * @return The id of {@link Sticker custom emoji} which used as sticker set thumbnail, if present.
     */
    public Optional<Long> getThumbDocumentId() {
        return Optional.ofNullable(data.thumbDocumentId());
    }

    /**
     * Gets count of stickers or emojis in the sticker set.
     *
     * @return The count of stickers or emojis.
     */
    public int getCount() {
        return data.count();
    }

    public int getHash() {
        return data.hash();
    }

    /**
     * Requests to retrieve full sticker set.
     *
     * @return A {@link Mono} emitting on successful completion {@link AuxiliaryStickerSet full sticker set info}.
     */
    public Mono<AuxiliaryStickerSet> getStickers() {
        return client.getStickerSet(ImmutableInputStickerSetID.of(data.id(), data.accessHash()), data.hash());
    }

    @Override
    public String toString() {
        return "StickerSet{" +
                "data=" + data +
                ", fileReferenceId='" + fileReferenceId + '\'' +
                '}';
    }

    /** Types of sticker sets. */
    public enum Type {
        /** Regular sticker set that can contains any type of {@link Sticker.Type stickers}. */
        REGULAR,

        /** Mask set with {@link StickerSet#getStickerType() element type} {@link Sticker.Type#STATIC}. */
        MASK,

        /** Custom emoji set that can contains any type of {@link Sticker.Type emojis}. */
        CUSTOM_EMOJI
    }
}
