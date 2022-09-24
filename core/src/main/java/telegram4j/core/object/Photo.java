package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.BaseDocumentFields;
import telegram4j.tl.BasePhoto;
import telegram4j.tl.DocumentAttributeImageSize;
import telegram4j.tl.InputPeer;

import java.util.Objects;
import java.util.Optional;

/** Representation for message and profile photos in normal quality. */
public class Photo extends Document {
    @Nullable
    private final DocumentAttributeImageSize sizeData;

    public Photo(MTProtoTelegramClient client, BaseDocumentFields data,
                 @Nullable String fileName, int messageId, InputPeer peer,
                 DocumentAttributeImageSize sizeData) {
        super(client, data, fileName, messageId, peer);
        this.sizeData = Objects.requireNonNull(sizeData);
    }

    public Photo(MTProtoTelegramClient client, BasePhoto data, InputPeer chatPeer, int messageId) {
        super(client, data, FileReferenceId.ofPhoto(data, messageId, chatPeer), null);

        sizeData = null;
    }

    public Photo(MTProtoTelegramClient client, BasePhoto data, int messageId, InputPeer peer) {
        super(client, data, FileReferenceId.ofChatPhoto(data, messageId, peer), null);

        sizeData = null;
    }

    /**
     * Gets original width of video document, if photo uploaded as document.
     *
     * @return The original width of video document, if photo uploaded as document
     */
    public Optional<Integer> getWidth() {
        return Optional.ofNullable(sizeData).map(DocumentAttributeImageSize::w);
    }

    /**
     * Gets original height of video document height, if photo uploaded as document
     *
     * @return The original height of video document height, if photo uploaded as document
     */
    public Optional<Integer> getHeight() {
        return Optional.ofNullable(sizeData).map(DocumentAttributeImageSize::h);
    }

    /**
     * Gets whether photo has mask stickers attached to it, otherwise {@code false}.
     *
     * @return {@code true} if photo has mask stickers attached to it, otherwise {@code false}.
     */
    public boolean hasStickers() {
        return data.identifier() == BasePhoto.ID && ((BasePhoto) data).hasStickers();
    }

    @Override
    public String toString() {
        return "Photo{" +
                "data=" + data +
                ", fileReferenceId=" + fileReferenceId +
                '}';
    }
}
