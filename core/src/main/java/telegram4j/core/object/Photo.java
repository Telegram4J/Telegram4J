package telegram4j.core.object;

import io.netty.buffer.ByteBufAllocator;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.media.PhotoSize;
import telegram4j.core.object.media.VideoSize;
import telegram4j.core.util.EntityFactory;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.BasePhoto;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class Photo implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final BasePhoto data;

    private final String smallFileReferenceId;
    private final String bigFileReferenceId;

    public Photo(MTProtoTelegramClient client, BasePhoto data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");

        this.smallFileReferenceId = FileReferenceId.ofPhoto(data)
                .serialize(ByteBufAllocator.DEFAULT);
        this.bigFileReferenceId = FileReferenceId.ofPhoto(data)
                .serialize(ByteBufAllocator.DEFAULT);
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

    public boolean isHasStickers() {
        return data.hasStickers();
    }

    public long getId() {
        return data.id();
    }

    public long getAccessHash() {
        return data.accessHash();
    }

    public byte[] getFileReference() {
        return data.fileReference();
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
