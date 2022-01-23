package telegram4j.core.object;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.media.PhotoSize;
import telegram4j.core.object.media.VideoSize;
import telegram4j.core.util.EntityFactory;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.BasePhoto;
import telegram4j.tl.InputPeer;
import telegram4j.tl.InputPeerEmpty;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class Photo implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final BasePhoto data;

    private final String fileReferenceId;

    public Photo(MTProtoTelegramClient client, BasePhoto data, int messageId) {
        this(client, data, InputPeerEmpty.instance(), messageId);
    }

    public Photo(MTProtoTelegramClient client, BasePhoto data, InputPeer peer, int messageId) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");

        if (peer.identifier() != InputPeerEmpty.ID) {
            this.fileReferenceId = FileReferenceId.ofChatPhoto(data, -1, peer)
                    .serialize(ByteBufAllocator.DEFAULT);
        } else {
            this.fileReferenceId = FileReferenceId.ofPhoto(data, messageId, peer)
                    .serialize(ByteBufAllocator.DEFAULT);
        }
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    public String getFileReferenceId() {
        return fileReferenceId;
    }

    public boolean isHasStickers() {
        return data.hasStickers();
    }

    public long getId() {
        return data.id();
    }

    public long getAccessHash() {
        return data.accessHash();
    }

    public String getFileReference() {
        return ByteBufUtil.hexDump(data.fileReference());
    }

    public Instant getTimestamp() {
        return Instant.ofEpochSecond(data.date());
    }

    public List<PhotoSize> getSizes() {
        return data.sizes().stream()
                .map(d -> EntityFactory.createPhotoSize(client, d))
                .collect(Collectors.toList());
    }

    public Optional<List<VideoSize>> getVideoSizes() {
        return Optional.ofNullable(data.videoSizes())
                .map(l -> l.stream()
                        .map(d -> new VideoSize(client, d))
                        .collect(Collectors.toList()));
    }

    public int getDcId() {
        return data.dcId();
    }
}
