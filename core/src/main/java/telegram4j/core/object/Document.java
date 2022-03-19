package telegram4j.core.object;

import io.netty.buffer.ByteBufUtil;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.media.PhotoSize;
import telegram4j.core.object.media.VideoSize;
import telegram4j.core.util.EntityFactory;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.BaseDocument;
import telegram4j.tl.InputPeer;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * General type of the documents.
 * All subtypes are inferred from {@link BaseDocument#attributes()} except {@link telegram4j.tl.DocumentAttributeFilename},
 * that pre-inferred to {@link #getFileName()} for this type.
 */
public class Document implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final BaseDocument data;
    @Nullable
    private final String fileName;

    private final String fileReferenceId;

    public Document(MTProtoTelegramClient client, BaseDocument data,
                    @Nullable String fileName, int messageId, InputPeer peer) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
        this.fileName = fileName;

        this.fileReferenceId = FileReferenceId.ofDocument(data, messageId, peer).serialize();
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets serialized file reference id for this document.
     *
     * @return The serialized file reference id.
     */
    public String getFileReferenceId() {
        return fileReferenceId;
    }

    /**
     * Gets id of the document. Mainly used in the methods.
     *
     * @return The id of the document.
     */
    public long getId() {
        return data.id();
    }

    /**
     * Gets access hash of the document. Mainly used in the methods.
     *
     * @return The access hash of the document.
     */
    public long getAccessHash() {
        return data.accessHash();
    }

    /**
     * Gets hex dump of the file reference. Mainly used in the methods.
     *
     * @return The hex dump of the file reference.
     */
    public String getFileReference() {
        return ByteBufUtil.hexDump(data.fileReference());
    }

    /**
     * Gets timestamp of the document creation.
     *
     * @return The {@link Instant} of the document creation.
     */
    public Instant getCreationTimestamp() {
        return Instant.ofEpochSecond(data.date());
    }

    /**
     * Gets mime-type of the document in the string.
     *
     * @return The mime-type string of the document.
     */
    public String getMimeType() {
        return data.mimeType();
    }

    /**
     * Gets size of document in the bytes.
     *
     * @return The size of document in the bytes.
     */
    public int getSize() {
        return data.size();
    }

    /**
     * Gets {@link List} of {@link PhotoSize thumbnails} for this document, if present.
     *
     * @return The l{@link List} of {@link PhotoSize thumbnails} for this document, if present.
     */
    public Optional<List<PhotoSize>> getThumbs() {
        return Optional.ofNullable(data.thumbs())
                .map(list -> list.stream()
                        .map(d -> EntityFactory.createPhotoSize(client, d))
                        .collect(Collectors.toList()));
    }

    /**
     * Gets {@link List} of {@link VideoSize video thumbnails} for this document, if present.
     *
     * @return The l{@link List} of {@link VideoSize video thumbnails} for this document, if present.
     */
    public Optional<List<VideoSize>> getVideoThumbs() {
        return Optional.ofNullable(data.videoThumbs())
                .map(l -> l.stream()
                        .map(d -> new VideoSize(client, d))
                        .collect(Collectors.toList()));
    }

    /**
     * Gets id of the DC, where document was stored.
     *
     * @return The id of the DC, where document was stored.
     */
    public int getDcId() {
        return data.dcId();
    }

    /**
     * Gets name of document, if present.
     *
     * @return The name of document, if present.
     */
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
