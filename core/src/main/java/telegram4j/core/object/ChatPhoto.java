package telegram4j.core.object;

import io.netty.buffer.ByteBuf;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.ChatPhotoFields;
import telegram4j.tl.InputPeer;

import java.util.Objects;
import java.util.Optional;

/**
 * Low-quality chat profile photo.
 * There are <b>2</b> versions available for download: small ({@link ChatPhoto#getSmallFileReferenceId()}) and big ({@link ChatPhoto#getBigFileReferenceId()}).
 */
public class ChatPhoto implements TelegramObject {
    private final MTProtoTelegramClient client;
    private final ChatPhotoFields data;

    private final String smallFileReferenceId;
    private final String bigFileReferenceId;

    public ChatPhoto(MTProtoTelegramClient client, ChatPhotoFields data, InputPeer peer, int messageId) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");

        this.smallFileReferenceId = FileReferenceId.ofChatPhoto(data,
                FileReferenceId.PhotoSizeType.CHAT_PHOTO_SMALL, messageId, peer).serialize();
        this.bigFileReferenceId = FileReferenceId.ofChatPhoto(data,
                FileReferenceId.PhotoSizeType.CHAT_PHOTO_BIG, messageId, peer).serialize();
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    public String getSmallFileReferenceId() {
        return smallFileReferenceId;
    }

    public String getBigFileReferenceId() {
        return bigFileReferenceId;
    }

    public boolean hasVideo() {
        return data.hasVideo();
    }

    public long getPhotoId() {
        return data.photoId();
    }

    public Optional<ByteBuf> getExpandedStrippedThumb() {
        return Optional.ofNullable(data.strippedThumb()).map(TlEntityUtil::expandInlineThumb);
    }

    public Optional<byte[]> getStrippedThumb() {
        return Optional.ofNullable(data.strippedThumb());
    }

    public int getDcId() {
        return data.dcId();
    }
}
