package telegram4j.core.object;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.media.PhotoSize;
import telegram4j.core.object.media.VideoSize;
import telegram4j.core.util.EntityFactory;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.BaseDocument;
import telegram4j.tl.InputPeerEmpty;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class Document implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final BaseDocument data;
    @Nullable
    private final String fileName;

    private final String fileReferenceId;

    public Document(MTProtoTelegramClient client, BaseDocument data,
                    @Nullable String fileName, int messageId) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
        this.fileName = fileName;

        this.fileReferenceId = FileReferenceId.ofDocument(data, messageId, InputPeerEmpty.instance())
                .serialize(ByteBufAllocator.DEFAULT);
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    public String getFileReferenceId() {
        return fileReferenceId;
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

    public int getDate() {
        return data.date();
    }

    public String getMimeType() {
        return data.mimeType();
    }

    public int getSize() {
        return data.size();
    }

    public Optional<List<PhotoSize>> getThumbs() {
        return Optional.ofNullable(data.thumbs())
                .map(list -> list.stream()
                        .map(d -> EntityFactory.createPhotoSize(client, d))
                        .collect(Collectors.toList()));
    }

    public Optional<List<VideoSize>> getVideoThumbs() {
        return Optional.ofNullable(data.videoThumbs())
                .map(l -> l.stream()
                        .map(d -> new VideoSize(client, d))
                        .collect(Collectors.toList()));
    }

    public int getDcId() {
        return data.dcId();
    }

    public Optional<String> getFileName() {
        return Optional.ofNullable(fileName);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return data.equals(document.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "Document{" +
                "data=" + data +
                ", fileName='" + fileName + '\'' +
                ", fileReferenceId='" + fileReferenceId + '\'' +
                '}';
    }
}
