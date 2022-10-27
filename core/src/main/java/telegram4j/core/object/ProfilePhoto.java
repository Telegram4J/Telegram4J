package telegram4j.core.object;

import io.netty.buffer.ByteBuf;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.ChatPhotoFields;
import telegram4j.tl.InputPeer;

import java.util.Objects;
import java.util.Optional;

/**
 * Low-quality chat profile photo.
 *
 * <p>There are 2 versions available for download: small ({@link ProfilePhoto#getSmallFileReferenceId()})
 * and big ({@link ProfilePhoto#getBigFileReferenceId()}).
 */
public class ProfilePhoto implements TelegramObject {
    private final MTProtoTelegramClient client;
    private final ChatPhotoFields data;

    private final FileReferenceId smallFileReferenceId;
    private final FileReferenceId bigFileReferenceId;

    public ProfilePhoto(MTProtoTelegramClient client, ChatPhotoFields data, InputPeer peer, int messageId) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);

        this.smallFileReferenceId = FileReferenceId.ofChatPhoto(data, false, messageId, peer);
        this.bigFileReferenceId = FileReferenceId.ofChatPhoto(data, true, messageId, peer);
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets {@link FileReferenceId} of <b>small</b> chat photo.
     *
     * @return The {@link FileReferenceId} of <b>small</b> chat photo.
     */
    public FileReferenceId getSmallFileReferenceId() {
        return smallFileReferenceId;
    }

    /**
     * Gets serialized {@link FileReferenceId} of <b>big</b> chat photo.
     *
     * @return The serialized {@link FileReferenceId} of <b>big</b> chat photo.
     */
    public FileReferenceId getBigFileReferenceId() {
        return bigFileReferenceId;
    }

    /**
     * Gets whether chat has animated photo.
     *
     * @return {@code true} if chat has animated photo.
     */
    public boolean hasVideo() {
        return data.hasVideo();
    }

    /**
     * Gets id of chat photo.
     *
     * @return The id of chat photo.
     */
    public long getId() {
        return data.photoId();
    }

    /**
     * Gets new {@link ByteBuf} with expanded stripped thumbnail for photo, if present.
     *
     * @return The new {@link ByteBuf} with expanded stripped thumbnail for photo, if present.
     */
    public Optional<ByteBuf> getThumb() {
        return Optional.ofNullable(data.strippedThumb()).map(TlEntityUtil::expandInlineThumb);
    }

    /**
     * Gets raw stripped thumbnail for photo, if present.
     *
     * @return The raw stripped thumbnail for photo, if present.
     */
    public Optional<ByteBuf> getStrippedThumb() {
        return Optional.ofNullable(data.strippedThumb());
    }

    /**
     * Gets id of DC that can be used for downloading this photo.
     *
     * @return The id of DC that can be used for downloading this photo.
     */
    public int getDcId() {
        return data.dcId();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProfilePhoto profilePhoto = (ProfilePhoto) o;
        return data.equals(profilePhoto.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "ChatPhoto{" +
                "data=" + data +
                ", smallFileReferenceId=" + smallFileReferenceId +
                ", bigFileReferenceId=" + bigFileReferenceId +
                '}';
    }
}
