package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.media.PhotoSize;
import telegram4j.core.util.EntityFactory;
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
                ImmutableInputStickerSetID.of(data.id(), data.accessHash()), ver) : null;
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

    public Optional<Instant> getInstallTimestamp() {
        return Optional.ofNullable(data.installedDate()).map(Instant::ofEpochSecond);
    }

    public long getId() {
        return data.id();
    }

    public long getAccessHash() {
        return data.accessHash();
    }

    public String getTitle() {
        return data.title();
    }

    public String getShortName() {
        return data.shortName();
    }

    public Optional<List<PhotoSize>> getThumbs() {
        return Optional.ofNullable(data.thumbs())
                .map(l -> l.stream()
                        .map(EntityFactory::createPhotoSize)
                        .collect(Collectors.toList()));
    }

    public Optional<Integer> getThumbDcId() {
        return Optional.ofNullable(data.thumbDcId());
    }

    public Optional<Integer> getThumbVersion() {
        return Optional.ofNullable(data.thumbVersion());
    }

    public Optional<Long> getThumbDocumentId() {
        return Optional.ofNullable(data.thumbDocumentId());
    }

    public int getCount() {
        return data.count();
    }

    public int getHash() {
        return data.hash();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StickerSet that = (StickerSet) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
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
