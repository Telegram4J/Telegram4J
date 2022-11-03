package telegram4j.core.object;

import io.netty.buffer.ByteBuf;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.internal.EntityFactory;
import telegram4j.core.object.media.PhotoSize;
import telegram4j.core.object.media.VideoSize;
import telegram4j.mtproto.file.Context;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.*;
import telegram4j.tl.api.TlObject;

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

    protected final MTProtoTelegramClient client;
    protected final TlObject data; // WebDocument/BaseDocument/BasePhoto
    @Nullable
    protected final String fileName;

    protected final FileReferenceId fileReferenceId;

    protected Document(MTProtoTelegramClient client, TlObject data,
                       FileReferenceId fileRefId, @Nullable String fileName) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);
        this.fileReferenceId = Objects.requireNonNull(fileRefId);
        this.fileName = fileName;
    }

    public Document(MTProtoTelegramClient client, BaseDocumentFields data,
                    @Nullable String fileName, Context context) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);
        this.fileName = fileName;

        this.fileReferenceId = data instanceof BaseDocument
                ? FileReferenceId.ofDocument((BaseDocument) data, context)
                : FileReferenceId.ofDocument((WebDocument) data, context);
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
     * @return The url of the web file, if present.
     */
    public Optional<String> getUrl() {
        return data instanceof WebDocument
                ? Optional.of(((WebDocument) data).url())
                : Optional.empty();
    }

    /**
     * Gets {@link FileReferenceId} for this document.
     *
     * @return The {@link FileReferenceId} for this document.
     */
    public FileReferenceId getFileReferenceId() {
        return fileReferenceId;
    }

    /**
     * Gets id of the document, if document isn't web.
     *
     * @return The id of the document, if document isn't web.
     */
    public Optional<Long> getId() {
        switch (data.identifier()) {
            case BaseDocument.ID: return Optional.of(((BaseDocument) data).id());
            case BasePhoto.ID: return Optional.of(((BasePhoto) data).id());
            default: return Optional.empty();
        }
    }

    /**
     * Gets access hash of the document, if document has telegram proxying.
     *
     * @return The access hash of the document, if document has telegram proxying.
     */
    public Optional<Long> getAccessHash() {
        switch (data.identifier()) {
            case BasePhoto.ID: return Optional.of(((BasePhoto) data).accessHash());
            case BaseWebDocument.ID: return Optional.of(((BaseWebDocument) data).accessHash());
            case BaseDocument.ID: return Optional.of(((BaseDocument) data).accessHash());
            default: return Optional.empty();
        }
    }

    /**
     * Gets <i>immutable</i> {@link ByteBuf} of the file reference, if document isn't web.
     *
     * @return The <i>immutable</i> {@link ByteBuf} of the file reference, if document isn't web.
     */
    public Optional<ByteBuf> getFileReference() {
        switch (data.identifier()) {
            case BaseDocument.ID: return Optional.of(((BaseDocument) data).fileReference());
            case BasePhoto.ID: return Optional.of(((BasePhoto) data).fileReference());
            default: return Optional.empty();
        }
    }

    /**
     * Gets timestamp of the document creation, if document isn't web.
     *
     * @return The {@link Instant} of the document creation, if document isn't web.
     */
    public Optional<Instant> getCreationTimestamp() {
        switch (data.identifier()) {
            case BaseDocument.ID: return Optional.of(Instant.ofEpochSecond(((BaseDocument) data).date()));
            case BasePhoto.ID: return Optional.of(Instant.ofEpochSecond(((BasePhoto) data).date()));
            default: return Optional.empty();
        }
    }

    /**
     * Gets mime-type of the document in the string.
     *
     * @return The mime-type string of the document.
     */
    public String getMimeType() {
        return data instanceof BaseDocumentFields
                ? ((BaseDocumentFields) data).mimeType()
                : "image/jpeg";
    }

    /**
     * Gets size of document in the bytes, if present.
     * {@link Photo} files may have several size variants.
     *
     * @return The size of document in the bytes, if present.
     */
    public Optional<Long> getSize() {
        if (data instanceof WebDocument) {
            return Optional.of((long) ((WebDocument) data).size());
        } else if (data.identifier() == BaseDocument.ID) {
            return Optional.of(((BaseDocument) data).size());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Gets mutable list of {@link PhotoSize thumbnails} for this document, if present.
     *
     * @return The mutable list of {@link PhotoSize thumbnails} for this document, if present.
     */
    public Optional<List<PhotoSize>> getThumbs() {
        switch (data.identifier()) {
            case BaseDocument.ID: return Optional.ofNullable(((BaseDocument) data).thumbs()).map(l -> l.stream()
                    .map(EntityFactory::createPhotoSize)
                    .collect(Collectors.toList()));
            case BasePhoto.ID: return Optional.of(((BasePhoto) data).sizes()).map(l -> l.stream()
                    .map(EntityFactory::createPhotoSize)
                    .collect(Collectors.toList()));
            default: return Optional.empty();
        }
    }

    /**
     * Gets mutable list of {@link VideoSize video thumbnails} for this document, if present.
     *
     * @return The mutable list of {@link VideoSize video thumbnails} for this document, if present.
     */
    public Optional<List<VideoSize>> getVideoThumbs() {
        switch (data.identifier()) {
            case BaseDocument.ID: return Optional.ofNullable(((BaseDocument) data).videoThumbs()).map(l -> l.stream()
                    .map(d -> new VideoSize(client, d))
                    .collect(Collectors.toList()));
            case BasePhoto.ID: return Optional.ofNullable(((BasePhoto) data).videoSizes()).map(l -> l.stream()
                    .map(d -> new VideoSize(client, d))
                    .collect(Collectors.toList()));
            default: return Optional.empty();
        }
    }

    /**
     * Gets id of the DC, where document was stored, if document isn't web.
     *
     * @return The id of the DC, where document was stored, if document isn't web.
     */
    public Optional<Integer> getDcId() {
        switch (data.identifier()) {
            case BaseDocument.ID: return Optional.of(((BaseDocument) data).dcId());
            case BasePhoto.ID: return Optional.of(((BasePhoto) data).dcId());
            default: return Optional.empty();
        }
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
    public final boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return fileReferenceId.equals(document.fileReferenceId);
    }

    @Override
    public final int hashCode() {
        return fileReferenceId.hashCode();
    }

    @Override
    public String toString() {
        return "Document{" +
                "data=" + data +
                ", fileReferenceId=" + fileReferenceId +
                '}';
    }
}
