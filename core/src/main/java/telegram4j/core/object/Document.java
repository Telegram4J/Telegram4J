package telegram4j.core.object;

import io.netty.buffer.ByteBuf;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.internal.EntityFactory;
import telegram4j.core.object.media.AnimatedThumbnail;
import telegram4j.core.object.media.Thumbnail;
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
public sealed class Document implements TelegramObject
        permits Video, Photo, Sticker, Audio {

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

    public Document(MTProtoTelegramClient client, BaseDocument data,
                    @Nullable String fileName, Context context) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);
        this.fileName = fileName;
        this.fileReferenceId = FileReferenceId.ofDocument(data, context);
    }

    public Document(MTProtoTelegramClient client, WebDocument data,
                    @Nullable String fileName, Context context) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);
        this.fileName = fileName;
        this.fileReferenceId = FileReferenceId.ofDocument(data, context);
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
        return data instanceof WebDocument;
    }

    /**
     * Gets url of web file, if document is web file.
     *
     * @return The url of the web file, if present.
     */
    public Optional<String> getUrl() {
        return data instanceof WebDocument w
                ? Optional.of(w.url())
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
        return switch (data.identifier()) {
            case BaseDocument.ID -> Optional.of(((BaseDocument) data).id());
            case BasePhoto.ID -> Optional.of(((BasePhoto) data).id());
            default -> Optional.empty();
        };
    }

    /**
     * Gets access hash of the document, if document has telegram proxying.
     *
     * @return The access hash of the document, if document has telegram proxying.
     */
    public Optional<Long> getAccessHash() {
        return switch (data.identifier()) {
            case BasePhoto.ID -> Optional.of(((BasePhoto) data).accessHash());
            case BaseWebDocument.ID -> Optional.of(((BaseWebDocument) data).accessHash());
            case BaseDocument.ID -> Optional.of(((BaseDocument) data).accessHash());
            default -> Optional.empty();
        };
    }

    /**
     * Gets <i>immutable</i> {@link ByteBuf} of the file reference, if document isn't web.
     *
     * @return The <i>immutable</i> {@link ByteBuf} of the file reference, if document isn't web.
     */
    public Optional<ByteBuf> getFileReference() {
        return switch (data.identifier()) {
            case BaseDocument.ID -> Optional.of(((BaseDocument) data).fileReference());
            case BasePhoto.ID -> Optional.of(((BasePhoto) data).fileReference());
            default -> Optional.empty();
        };
    }

    /**
     * Gets timestamp of the document creation, if document isn't web.
     *
     * @return The {@link Instant} of the document creation, if document isn't web.
     */
    public Optional<Instant> getCreationTimestamp() {
        return switch (data.identifier()) {
            case BaseDocument.ID -> Optional.of(Instant.ofEpochSecond(((BaseDocument) data).date()));
            case BasePhoto.ID -> Optional.of(Instant.ofEpochSecond(((BasePhoto) data).date()));
            default -> Optional.empty();
        };
    }

    /**
     * Gets mime-type of the document in the string.
     *
     * @return The mime-type string of the document.
     */
    public String getMimeType() {
        return switch (data.identifier()) {
            case BasePhoto.ID -> "image/jpeg";
            case BaseWebDocument.ID, WebDocumentNoProxy.ID -> ((WebDocument) data).mimeType();
            case BaseDocument.ID -> ((BaseDocument) data).mimeType();
            default -> throw new IllegalStateException();
        };
    }

    /**
     * Gets size of document in the bytes, if present.
     * {@link Photo} files may have several size variants.
     *
     * @return The size of document in the bytes, if present.
     */
    public Optional<Long> getSize() {
        return switch (data.identifier()) {
            case BaseWebDocument.ID, WebDocumentNoProxy.ID -> Optional.of((long) ((WebDocument) data).size());
            case BaseDocument.ID -> Optional.of(((BaseDocument) data).size());
            default -> Optional.empty();
        };
    }

    /**
     * Gets mutable list of {@link Thumbnail thumbnails} for this document, if present.
     *
     * @return The mutable list of {@link Thumbnail thumbnails} for this document, if present.
     */
    public Optional<List<Thumbnail>> getThumbs() {
        return switch (data.identifier()) {
            case BaseDocument.ID -> Optional.ofNullable(((BaseDocument) data).thumbs()).map(l -> l.stream()
                    .map(EntityFactory::createThumbnail)
                    .collect(Collectors.toList()));
            case BasePhoto.ID -> Optional.of(((BasePhoto) data).sizes()).map(l -> l.stream()
                    .map(EntityFactory::createThumbnail)
                    .collect(Collectors.toList()));
            default -> Optional.empty();
        };
    }

    /**
     * Gets mutable list of {@link AnimatedThumbnail video thumbnails} for this document, if present.
     *
     * @return The mutable list of {@link AnimatedThumbnail video thumbnails} for this document, if present.
     */
    public Optional<List<AnimatedThumbnail>> getAnimatedThumbs() {
        return switch (data.identifier()) {
            case BaseDocument.ID -> Optional.ofNullable(((BaseDocument) data).videoThumbs()).map(l -> l.stream()
                    .map(d -> EntityFactory.createAnimatedThumbnail(client, d))
                    .collect(Collectors.toList()));
            case BasePhoto.ID -> Optional.ofNullable(((BasePhoto) data).videoSizes()).map(l -> l.stream()
                    .map(d -> EntityFactory.createAnimatedThumbnail(client, d))
                    .collect(Collectors.toList()));
            default -> Optional.empty();
        };
    }

    /**
     * Gets id of the DC, where document was stored, if document isn't web.
     *
     * @return The id of the DC, where document was stored, if document isn't web.
     */
    public Optional<Integer> getDcId() {
        return switch (data.identifier()) {
            case BaseDocument.ID -> Optional.of(((BaseDocument) data).dcId());
            case BasePhoto.ID -> Optional.of(((BasePhoto) data).dcId());
            default -> Optional.empty();
        };
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
        if (!(o instanceof Document d)) return false;
        return fileReferenceId.equals(d.fileReferenceId);
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
