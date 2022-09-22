package telegram4j.core.object;

import io.netty.buffer.ByteBuf;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.media.PhotoSize;
import telegram4j.core.object.media.VideoSize;
import telegram4j.core.util.EntityFactory;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.BasePhoto;
import telegram4j.tl.InputPeer;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** Representation for message and profile photos in normal quality. */
public class Photo implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final BasePhoto data;

    private final FileReferenceId fileReferenceId;

    public Photo(MTProtoTelegramClient client, BasePhoto data, InputPeer chatPeer, int messageId) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);

        this.fileReferenceId = FileReferenceId.ofPhoto(data, '\0', messageId, chatPeer);
    }

    public Photo(MTProtoTelegramClient client, BasePhoto data, int messageId, InputPeer peer) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);

        this.fileReferenceId = FileReferenceId.ofChatPhoto(data, '\0', messageId, peer);
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets {@link FileReferenceId} for this photo.
     *
     * @return The {@link FileReferenceId} for this photo.
     */
    public FileReferenceId getFileReferenceId() {
        return fileReferenceId;
    }

    /**
     * Gets whether photo has mask stickers attached to it.
     *
     * @return {@code true} if photo has mask stickers attached to it.
     */
    public boolean hasStickers() {
        return data.hasStickers();
    }

    /**
     * Gets id of the photo.
     *
     * @return The id of the photo.
     */
    public long getId() {
        return data.id();
    }

    /**
     * Gets access hash of the photo.
     *
     * @return The access hash of the photo.
     */
    public long getAccessHash() {
        return data.accessHash();
    }

    /**
     * Gets <i>immutable</i> {@link ByteBuf} of the file reference.
     *
     * @return The <i>immutable</i> {@link ByteBuf} of the file reference.
     */
    public ByteBuf getFileReference() {
        return data.fileReference();
    }

    /**
     * Gets timestamp of the photo upload.
     *
     * @return The {@link Instant} of the photo upload.
     */
    public Instant getTimestamp() {
        return Instant.ofEpochSecond(data.date());
    }

    /**
     * Gets {@link List} of {@link PhotoSize thumbnails} for this photo.
     *
     * @return The {@link List} of {@link PhotoSize thumbnails} for this photo.
     */
    public List<PhotoSize> getSizes() {
        return data.sizes().stream()
                .map(EntityFactory::createPhotoSize)
                .collect(Collectors.toList());
    }

    /**
     * Gets {@link List} of {@link VideoSize video thumbnails} for this photo if it's animated, if present.
     *
     * @return The {@link List} of {@link VideoSize video thumbnails} for this photo, if present.
     */
    public Optional<List<VideoSize>> getVideoSizes() {
        return Optional.ofNullable(data.videoSizes())
                .map(l -> l.stream()
                        .map(d -> new VideoSize(client, d))
                        .collect(Collectors.toList()));
    }

    /**
     * Gets id of the DC, where photo was stored.
     *
     * @return The id of the DC, where photo was stored.
     */
    public int getDcId() {
        return data.dcId();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Photo photo = (Photo) o;
        return data.equals(photo.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "Photo{" +
                "data=" + data +
                ", fileReferenceId=" + fileReferenceId +
                '}';
    }
}
