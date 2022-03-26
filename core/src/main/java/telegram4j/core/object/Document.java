package telegram4j.core.object;

import io.netty.buffer.ByteBufUtil;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.media.PhotoSize;
import telegram4j.core.object.media.VideoSize;
import telegram4j.core.util.EntityFactory;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.*;

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
    private final BaseDocumentFields data;
    @Nullable
    private final String fileName;

    private final String fileReferenceId;

    public Document(MTProtoTelegramClient client, BaseDocumentFields data,
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
     * Gets whether document is web file.
     *
     * @return {@code true} if document is web, otherwise {@code false}.
     */
    public boolean isWeb() {
        return data.identifier() != BaseDocument.ID;
    }

    /**
     * Gets url of web file, if present.
     *
     * @return The url of the web file.
     */
    public Optional<String> getUrl() {
        return data instanceof WebDocument
                ? Optional.of(((WebDocument) data).url())
                : Optional.empty();
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
     * Gets id of the document, if document isn't web.
     *
     * @return The id of the document, if document isn't web.
     */
    public Optional<Long> getId() {
        return data.identifier() == BaseDocument.ID
                ? Optional.of(((BaseDocument) data).id())
                : Optional.empty();
    }

    /**
     * Gets access hash of the document, if document has telegram proxying.
     *
     * @return The access hash of the document, if document has telegram proxying.
     */
    public Optional<Long> getAccessHash() {
        switch (data.identifier()) {
            case BaseWebDocument.ID: return Optional.of(((BaseWebDocument) data).accessHash());
            case BaseDocument.ID: return Optional.of(((BaseDocument) data).accessHash());
            default: return Optional.empty();
        }
    }

    /**
     * Gets hex dump of the file reference, if document isn't web.
     *
     * @return The hex dump of the file reference, if document isn't web.
     */
    public Optional<String> getFileReference() {
        return data.identifier() == BaseDocument.ID
                ? Optional.of(ByteBufUtil.hexDump(((BaseDocument) data).fileReference()))
                : Optional.empty();
    }

    /**
     * Gets timestamp of the document creation, if document isn't web.
     *
     * @return The {@link Instant} of the document creation, if document isn't web.
     */
    public Optional<Instant> getCreationTimestamp() {
        return data.identifier() == BaseDocument.ID
                ? Optional.ofNullable(Instant.ofEpochSecond(((BaseDocument) data).date()))
                : Optional.empty();
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
     * @return The {@link List} of {@link PhotoSize thumbnails} for this document, if present.
     */
    public Optional<List<PhotoSize>> getThumbs() {
        return data.identifier() == BaseDocument.ID
                ? Optional.ofNullable(((BaseDocument) data).thumbs())
                .map(list -> list.stream()
                        .map(d -> EntityFactory.createPhotoSize(client, d))
                        .collect(Collectors.toList()))
                : Optional.empty();
    }

    /**
     * Gets {@link List} of {@link VideoSize video thumbnails} for this document, if present.
     *
     * @return The {@link List} of {@link VideoSize video thumbnails} for this document, if present.
     */
    public Optional<List<VideoSize>> getVideoThumbs() {
        return data.identifier() == BaseDocument.ID
                ? Optional.ofNullable(((BaseDocument) data).videoThumbs())
                .map(l -> l.stream()
                        .map(d -> new VideoSize(client, d))
                        .collect(Collectors.toList()))
                : Optional.empty();
    }

    /**
     * Gets id of the DC, where document was stored, if document isn't web.
     *
     * @return The id of the DC, where document was stored, if document isn't web.
     */
    public Optional<Integer> getDcId() {
        return data.identifier() == BaseDocument.ID
                ? Optional.of(((BaseDocument) data).dcId())
                : Optional.empty();
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
