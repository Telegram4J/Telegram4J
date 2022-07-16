package telegram4j.mtproto.file;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.base64.Base64Dialect;
import reactor.util.annotation.Nullable;
import telegram4j.tl.*;
import telegram4j.tl.api.TlEncodingUtil;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * File reference wrapper, which can be serialized to base 64 url string
 * via {@link #serialize()} and deserialized {@link #deserialize(String)}.
 * For compatibility with rpc methods can be mapped to {@code InputPhoto}/{@code InputDocument}, {@code InputFileLocation}.
 *
 * @apiNote This identifier can't be used across different accounts
 * due to the relativity of message identifiers in private chats
 * and groups, as well as other privacy factors.
 */
public class FileReferenceId {

    static final char PREFIX = 'x';
    static final byte MAX_RLE_SEQ = Byte.MAX_VALUE;

    static final int MESSAGE_ID_MASK = 1;
    static final int PEER_MASK = 1 << 1;
    static final int ACCESS_HASH_MASK = 1 << 2;
    static final int THUMB_SIZE_TYPE_MASK = 1 << 3;

    private final Type fileType;
    private final DocumentType documentType;
    private final PhotoSizeType sizeType;
    private final int dcId;
    private final long documentId;
    private final long accessHash;
    private final ByteBuf fileReference;
    private final String thumbSizeType;
    private final String url;
    private final InputStickerSet stickerSet;
    private final int thumbVersion;

    private final int messageId;
    private final InputPeer peer;

    FileReferenceId(Type fileType, DocumentType documentType, PhotoSizeType sizeType,
                    int dcId, long documentId, long accessHash,
                    ByteBuf fileReference, String thumbSizeType,
                    String url, InputStickerSet stickerSet, int thumbVersion,
                    int messageId, InputPeer peer) {
        this.fileType = Objects.requireNonNull(fileType, "fileType");
        this.documentType = Objects.requireNonNull(documentType, "documentType");
        this.sizeType = Objects.requireNonNull(sizeType, "sizeType");
        this.dcId = dcId;
        this.documentId = documentId;
        this.accessHash = accessHash;
        this.fileReference = Objects.requireNonNull(fileReference, "fileReference");
        this.url = Objects.requireNonNull(url, "url");
        this.thumbSizeType = Objects.requireNonNull(thumbSizeType, "thumbSize");
        this.stickerSet = stickerSet;
        this.thumbVersion = thumbVersion;

        this.messageId = messageId;
        this.peer = Objects.requireNonNull(peer, "peer");
    }

    /**
     * Creates new {@code FileReferenceId} object from given document and source context,
     * with <b>first</b> thumbnail, if applicable.
     *
     * @throws IllegalArgumentException If peer id is {@link InputPeerEmpty} or message id is negative.
     * @param document The document info.
     * @param messageId The source message id.
     * @param peer The message source peer.
     * @return The new {@code FileReferenceId} from given document and source context.
     */
    public static FileReferenceId ofDocument(BaseDocumentFields document, int messageId, InputPeer peer) {
        if (peer.identifier() == InputPeerEmpty.ID) {
            throw new IllegalArgumentException("Unexpected peer type.");
        }
        if (messageId < 0) {
            throw new IllegalArgumentException("Message id must be positive.");
        }

        long id = -1;
        long accessHash = -1;
        String firstThumbSize = "";
        int dcId = -1;
        ByteBuf fileReference = Unpooled.EMPTY_BUFFER;
        String url = "";
        Type type;
        DocumentType documentType = DocumentType.fromAttributes(document.attributes());
        switch (document.identifier()) {
            case BaseDocument.ID: {
                BaseDocument document0 = (BaseDocument) document;

                type = Type.DOCUMENT;
                id = document0.id();
                accessHash = document0.accessHash();
                var thumbs = document0.thumbs();
                firstThumbSize = thumbs != null && !thumbs.isEmpty() ? thumbs.get(0).type() : "";
                dcId = document0.dcId();
                fileReference = TlEncodingUtil.copyAsUnpooled(document0.fileReference());

                break;
            }
            case BaseWebDocument.ID: {
                BaseWebDocument document0 = (BaseWebDocument) document;

                type = Type.WEB_DOCUMENT;
                url = document0.url();
                accessHash = document0.accessHash();

                break;
            }
            case WebDocumentNoProxy.ID: {
                WebDocumentNoProxy document0 = (WebDocumentNoProxy) document;

                url = document0.url();
                type = Type.WEB_DOCUMENT;

                break;
            }
            default:
                throw new IllegalArgumentException("Unknown document type: " + document);
        }

        return new FileReferenceId(type, documentType, PhotoSizeType.UNKNOWN,
                dcId, id, accessHash, fileReference, firstThumbSize, url,
                InputStickerSetEmpty.instance(), -1, messageId, peer);
    }

    /**
     * Creates new {@code FileReferenceId} object from given <b>normal</b> photo and source context,
     * with <b>first</b> thumbnail.
     *
     * @throws IllegalArgumentException If peer id is {@link InputPeerEmpty}.
     * @param chatPhoto The chat photo info.
     * @param messageId The source message id, if photo from message, otherwise must be {@code -1}.
     * @param peer The chat/channel peer where photo was sent.
     * @return The new {@code FileReferenceId} from given <b>normal</b> photo and source context.
     */
    public static FileReferenceId ofChatPhoto(BasePhoto chatPhoto, int messageId, InputPeer peer) {
        if (peer.identifier() == InputPeerEmpty.ID) {
            throw new IllegalArgumentException("Unexpected peer type: " + peer);
        }
        var sizes = chatPhoto.sizes();
        String photoSizeType = sizes.isEmpty() ? "" : sizes.get(0).type();

        return new FileReferenceId(Type.CHAT_PHOTO, DocumentType.UNKNOWN, PhotoSizeType.UNKNOWN,
                chatPhoto.dcId(), chatPhoto.id(), chatPhoto.accessHash(),
                TlEncodingUtil.copyAsUnpooled(chatPhoto.fileReference()), photoSizeType,
                "", InputStickerSetEmpty.instance(), -1, messageId, peer);
    }

    /**
     * Creates new {@code FileReferenceId} object from given minimal chat photo and source context.
     *
     * @throws IllegalArgumentException If {@code sizeType} is {@link PhotoSizeType#UNKNOWN} or peer id is {@link InputPeerEmpty}.
     * @param chatPhoto The chat photo info.
     * @param sizeType The size type of photo, must be {@link PhotoSizeType#CHAT_PHOTO_SMALL} or {@link PhotoSizeType#CHAT_PHOTO_BIG}.
     * @param messageId The source message id, if photo from message, otherwise must be {@code -1}.
     * @param peer The peer that's have this photo.
     * @return The new {@code FileReferenceId} from given minimal chat photo and source context.
     */
    public static FileReferenceId ofChatPhoto(ChatPhotoFields chatPhoto, PhotoSizeType sizeType,
                                              int messageId, InputPeer peer) {
        if (peer.identifier() == InputPeerEmpty.ID) {
            throw new IllegalArgumentException("Unexpected peer type: " + peer);
        }
        if (sizeType == PhotoSizeType.UNKNOWN) {
            throw new IllegalArgumentException("Unexpected size type for chat photo: " + sizeType);
        }

        return new FileReferenceId(Type.CHAT_PHOTO, DocumentType.UNKNOWN, sizeType,
                chatPhoto.dcId(), chatPhoto.photoId(), -1,
                Unpooled.EMPTY_BUFFER, "", "",
                InputStickerSetEmpty.instance(), -1, messageId, peer);
    }

    /**
     * Creates new {@code FileReferenceId} object from given <b>message</b> photo and source context,
     * with <b>first</b> thumbnail.
     *
     * @throws IllegalArgumentException If peer id is {@link InputPeerEmpty} or message id is negative.
     * @param photo The photo object.
     * @param messageId The source message id, if photo from message, otherwise must be {@code -1}.
     * @param peer The message source peer.
     * @return The new {@code FileReferenceId} from given photo and source context.
     */
    public static FileReferenceId ofPhoto(BasePhoto photo, int messageId, InputPeer peer) {
        if (peer.identifier() == InputPeerEmpty.ID) {
            throw new IllegalArgumentException("Unexpected peer type.");
        }
        if (messageId < 0) {
            throw new IllegalArgumentException("Message id must be positive.");
        }
        var sizes = photo.sizes();
        String photoSizeType = sizes.isEmpty() ? "" : sizes.get(0).type();

        return new FileReferenceId(Type.PHOTO, DocumentType.UNKNOWN,
                PhotoSizeType.UNKNOWN, photo.dcId(), photo.id(), photo.accessHash(),
                TlEncodingUtil.copyAsUnpooled(photo.fileReference()), photoSizeType,
                "", InputStickerSetEmpty.instance(), -1, messageId, peer);
    }

    /**
     * Creates new {@code FileReferenceId} object with sticker set thumbnail from given their id.
     *
     * @throws IllegalArgumentException If sticker set id is {@link InputStickerSetEmpty}.
     * @param stickerSet The sticker set identifier.
     * @param version The id of sticker set thumbnail.
     * @return The new {@code FileReferenceId} with sticker set thumbnail from their id.
     */
    public static FileReferenceId ofStickerSet(InputStickerSet stickerSet, int version) {
        if (stickerSet.identifier() == InputStickerSetEmpty.ID) {
            throw new IllegalArgumentException("Unexpected stickerSet type.");
        }
        if (version < 0) {
            throw new IllegalArgumentException("Invalid sticker set thumbnail version.");
        }
        return new FileReferenceId(Type.STICKER_SET_THUMB, DocumentType.UNKNOWN,
                PhotoSizeType.UNKNOWN, -1, -1, -1,
                Unpooled.EMPTY_BUFFER, "", "", stickerSet,
                version, -1, InputPeerEmpty.instance());
    }

    /**
     * Deserializes reference to {@code FileReferenceId}
     * with decoded information about file.
     *
     * @throws IllegalArgumentException If it's not a file ref id.
     * @param str The base 64 url string.
     * @return The deserialized {@code FileReferenceId} with decoded information.
     */
    public static FileReferenceId deserialize(String str) {
        if (str.length() < 1 || str.charAt(0) != PREFIX) {
            throw new IllegalArgumentException("Incorrect file reference id format: '" + str + "'");
        }

        ByteBuf data = Unpooled.wrappedBuffer(str.getBytes(StandardCharsets.UTF_8));
        data.skipBytes(1); // PREFIX

        ByteBuf buf = Base64.decode(data, Base64Dialect.URL_SAFE);
        data.release();
        buf = decodeZeroRle(buf);

        Type fileType = Type.ALL[buf.readByte()];
        byte flags = buf.readByte();
        int messageId = -1;
        if ((flags & MESSAGE_ID_MASK) != 0) {
            messageId = buf.readIntLE();
        }

        InputPeer peer = InputPeerEmpty.instance();
        if ((flags & PEER_MASK) != 0) {
            peer = TlDeserializer.deserialize(buf);
        }

        String url = "";
        long accessHash = -1;
        int dcId = -1;
        long documentId = -1;
        DocumentType documentType = DocumentType.UNKNOWN;
        ByteBuf fileReference = Unpooled.EMPTY_BUFFER;
        String thumbSizeType = "";
        PhotoSizeType sizeType = PhotoSizeType.UNKNOWN;
        InputStickerSet stickerSet = InputStickerSetEmpty.instance();
        int thumbVersion = -1;

        switch (fileType) {
            case WEB_DOCUMENT:
                documentType = DocumentType.ALL[buf.readByte()];

                if ((flags & ACCESS_HASH_MASK) != 0) {
                    accessHash = buf.readLongLE();
                }

                url = TlSerialUtil.deserializeString(buf);

                break;
            case DOCUMENT:
                documentType = DocumentType.ALL[buf.readByte()];

            case PHOTO:
                dcId = buf.readByte();
                documentId = buf.readLongLE();
                accessHash = buf.readLongLE();

                fileReference = TlEncodingUtil.copyAsUnpooled(TlSerialUtil.deserializeBytes(buf));
                if ((flags & THUMB_SIZE_TYPE_MASK) != 0) {
                    thumbSizeType = Character.valueOf((char) buf.readByte()).toString();
                }

                break;
            case CHAT_PHOTO:
                sizeType = PhotoSizeType.ALL[buf.readByte()];
                dcId = buf.readByte();
                documentId = buf.readLongLE();

                if (sizeType == PhotoSizeType.UNKNOWN) {
                    accessHash = buf.readLongLE();
                    fileReference = TlEncodingUtil.copyAsUnpooled(TlSerialUtil.deserializeBytes(buf));
                    if ((flags & THUMB_SIZE_TYPE_MASK) != 0) {
                        thumbSizeType = Character.valueOf((char) buf.readByte()).toString();
                    }
                }

                break;
            case STICKER_SET_THUMB:
                thumbVersion = buf.readIntLE();
                stickerSet = TlDeserializer.deserialize(buf);

                break;
            default:
                buf.release();
                throw new IllegalStateException("Malformed file reference id.");
        }

        buf.release();
        return new FileReferenceId(fileType, documentType, sizeType, dcId, documentId, accessHash,
                fileReference, thumbSizeType, url, stickerSet,
                thumbVersion, messageId, peer);
    }

    static ByteBuf encodeZeroRle(ByteBuf data) {
        ByteBuf encoded = data.alloc().buffer(data.readableBytes());

        for (int i = 0, n = data.readableBytes(); i < n; i++) {
            byte b = data.getByte(i);
            encoded.writeByte(b);

            if (i + 1 < n && b == 0) {
                int c = 1;
                while (c < MAX_RLE_SEQ && i + c < n && data.getByte(i + c) == 0) {
                    c++;
                }

                encoded.writeByte(c);
                i += c - 1;
            }
        }

        data.release();
        return encoded;
    }

    static ByteBuf decodeZeroRle(ByteBuf data) {
        ByteBuf decoded = data.alloc().buffer(data.readableBytes());

        for (int i = 0, n = data.readableBytes(); i < n; i++) {
            byte b = data.getByte(i);
            if (i + 1 < n && b == 0) {
                decoded.writeZero(data.getByte(i + 1));
                i++;
                continue;
            }

            decoded.writeByte(b);
        }

        data.release();
        return decoded;
    }

    /**
     * Serializes reference to base 64 url string,
     * which can be deserialized via {@link #deserialize(String)}.
     *
     * @return The serialized base 64 url identifier string of file reference.
     */
    public String serialize() {
        UnpooledByteBufAllocator alloc = UnpooledByteBufAllocator.DEFAULT;
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte((byte) fileType.ordinal());

        byte flags = (byte) ((messageId != -1 ? MESSAGE_ID_MASK : 0) | (peer.identifier() != InputPeerEmpty.ID ? PEER_MASK : 0));
        if (fileType == Type.WEB_DOCUMENT && accessHash != -1) {
            flags |= ACCESS_HASH_MASK;
        }
        if ((fileType == Type.DOCUMENT || fileType == Type.PHOTO ||
                fileType == Type.CHAT_PHOTO && sizeType == PhotoSizeType.UNKNOWN) &&
                !thumbSizeType.isEmpty()) {
            flags |= THUMB_SIZE_TYPE_MASK;
        }

        buf.writeByte(flags);
        if (messageId != -1) {
            buf.writeIntLE(messageId);
        }
        if (peer.identifier() != InputPeerEmpty.ID) {
            ByteBuf peerIdBuf = TlSerializer.serialize(alloc, peer);
            buf.writeBytes(peerIdBuf);
            peerIdBuf.release();
        }

        switch (fileType) {
            case WEB_DOCUMENT:
                buf.writeByte((byte) documentType.ordinal());

                if (accessHash != -1) {
                    buf.writeLongLE(accessHash);
                }

                ByteBuf urlBuf = TlSerialUtil.serializeString(alloc, url);
                buf.writeBytes(urlBuf);
                urlBuf.release();

                break;
            case DOCUMENT:
                buf.writeByte((byte) documentType.ordinal());

            case PHOTO: {

                buf.writeByte((byte) dcId);
                buf.writeLongLE(documentId);
                buf.writeLongLE(accessHash);

                ByteBuf fileReferenceBuf = TlSerialUtil.serializeBytes(alloc, fileReference);
                buf.writeBytes(fileReferenceBuf);
                fileReferenceBuf.release();

                if (!thumbSizeType.isEmpty()) {
                    buf.writeByte(thumbSizeType.charAt(0));
                }

                break;
            }
            case CHAT_PHOTO:
                buf.writeByte((byte) sizeType.ordinal());
                buf.writeByte((byte) dcId);
                buf.writeLongLE(documentId);

                if (sizeType == PhotoSizeType.UNKNOWN) {
                    buf.writeLongLE(accessHash);

                    ByteBuf fileReferenceBuf = TlSerialUtil.serializeBytes(alloc, fileReference);
                    buf.writeBytes(fileReferenceBuf);
                    fileReferenceBuf.release();

                    if (!thumbSizeType.isEmpty()) {
                        buf.writeByte(thumbSizeType.charAt(0));
                    }
                }

                break;
            case STICKER_SET_THUMB:

                buf.writeIntLE(thumbVersion);

                ByteBuf stickerSetBuf = TlSerializer.serialize(alloc, stickerSet);
                buf.writeBytes(stickerSetBuf);
                stickerSetBuf.release();

                break;

            default:
                throw new IllegalStateException("Unexpected value: " + fileType);
        }

        buf = encodeZeroRle(buf);
        ByteBuf base64 = Base64.encode(buf, Base64Dialect.URL_SAFE);
        try {
            return PREFIX + base64.toString(StandardCharsets.UTF_8);
        } finally {
            base64.release();
        }
    }

    /**
     * Gets the type of file.
     *
     * @return The type of file.
     */
    public Type getFileType() {
        return fileType;
    }

    /**
     * Gets the size type of chat photo, if chat photo is it.
     *
     * @return The size type of chat photo, if applicable, otherwise {@link PhotoSizeType#UNKNOWN}.
     */
    public PhotoSizeType getSizeType() {
        return sizeType;
    }

    /**
     * Gets the id of media dc, through which can be downloaded file, if applicable.
     *
     * @return The id of media dc, if applicable, otherwise {@code -1}.
     */
    public int getDcId() {
        return dcId;
    }

    /**
     * Gets the id of file, if applicable.
     *
     * @return The id of file, if applicable, otherwise {@code -1}.
     */
    public long getDocumentId() {
        return documentId;
    }

    /**
     * Gets the access hash, if file has it.
     *
     * @return The access hash, if applicable, otherwise {@code -1}.
     */
    public long getAccessHash() {
        return accessHash;
    }

    /**
     * Gets <i>immutable</i> {@link ByteBuf} of file reference, if file has it.
     *
     * @return The <i>immutable</i> {@link ByteBuf} of file reference, if applicable, otherwise {@code Unpooled.EMPTY_BUFFER}.
     */
    public ByteBuf getFileReference() {
        return fileReference.duplicate();
    }

    /**
     * Gets the source peer id, if applicable.
     *
     * @return The source peer id, if applicable, otherwise {@link InputPeerEmpty}.
     */
    public InputPeer getPeer() {
        return peer;
    }

    /**
     * Gets the thumbnail transformation type, if file has it.
     *
     * @return The thumbnail transformation type, if applicable, otherwise {@code ""}.
     */
    public String getThumbSizeType() {
        return thumbSizeType;
    }

    /**
     * Gets the web document url, if file has it.
     *
     * @return The url of web document, if applicable, otherwise {@code ""}.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Gets the source message id, if file from message.
     *
     * @return The source message id, if applicable, otherwise {@code -1}.
     */
    public int getMessageId() {
        return messageId;
    }

    /**
     * Gets {@link DocumentType} of web or tg file, if file is document.
     *
     * @return The {@link DocumentType} of file, if applicable, otherwise {@code DocumentType.UNKNOWN}
     */
    public DocumentType getDocumentType() {
        return documentType;
    }

    /**
     * Creates a {@link InputWebFileLocation} from this reference,
     * which used in {@link telegram4j.tl.request.upload.GetWebFile} method, if access hash present, and it's web file
     *
     * @return The new {@link InputWebFileLocation} from this reference, if access hash present, and it's web file
     */
    public Optional<InputWebFileLocation> asWebLocation() {
        switch (fileType) {
            case WEB_DOCUMENT:
                if (accessHash == -1) {
                    return Optional.empty();
                }

                return Optional.of(ImmutableBaseInputWebFileLocation.of(url, accessHash));
            default:
                return Optional.empty();
        }
    }

    /**
     * Creates a {@link InputFileLocation} from this reference,
     * which used in {@link telegram4j.tl.request.upload.GetFile} method.
     * <p>
     * Web files would be ignored, use {@link #asWebLocation()}.
     *
     * @return The new {@link InputFileLocation} from this reference.
     */
    public Optional<InputFileLocation> asLocation() {
        switch (fileType) {
            case CHAT_PHOTO:
                if (fileReference != Unpooled.EMPTY_BUFFER) { // is full image
                    return Optional.of(InputPhotoFileLocation.builder()
                            .accessHash(accessHash)
                            .fileReference(fileReference)
                            .id(documentId)
                            .thumbSize(thumbSizeType)
                            .build());
                }

                return Optional.of(InputPeerPhotoFileLocation.builder()
                        .peer(peer)
                        .photoId(documentId)
                        .big(sizeType == PhotoSizeType.CHAT_PHOTO_BIG)
                        .build());
            case PHOTO:
                return Optional.of(InputPhotoFileLocation.builder()
                        .accessHash(accessHash)
                        .fileReference(fileReference)
                        .id(documentId)
                        .thumbSize(thumbSizeType)
                        .build());
            case STICKER_SET_THUMB:
                return Optional.of(InputStickerSetThumb.builder()
                        .stickerset(stickerSet)
                        .thumbVersion(thumbVersion)
                        .build());
            case DOCUMENT:
                return Optional.of(InputDocumentFileLocation.builder()
                        .accessHash(accessHash)
                        .fileReference(fileReference)
                        .id(documentId)
                        .thumbSize(thumbSizeType)
                        .build());
            default:
                return Optional.empty();
        }
    }

    /**
     * Creates a {@link ImmutableBaseInputDocument} from this reference if file type is {@link Type#DOCUMENT}.
     *
     * @throws IllegalStateException If tried to create input document from non-document reference.
     * @return The new {@link ImmutableBaseInputDocument} from this reference.
     */
    public ImmutableBaseInputDocument asInputDocument() {
        if (fileType != Type.DOCUMENT) {
            throw new IllegalStateException("Cant create input document from file reference id: " + this);
        }

        return ImmutableBaseInputDocument.of(documentId, accessHash)
                .withFileReference(fileReference);
    }

    /**
     * Creates a {@link ImmutableBaseInputPhoto} from this reference if file type is {@link Type#PHOTO}.
     *
     * @throws IllegalStateException If tried to create input photo from non-photo reference.
     * @return The new {@link ImmutableBaseInputPhoto} from this reference.
     */
    public ImmutableBaseInputPhoto asInputPhoto() {
        if (fileType != Type.PHOTO) {
            throw new IllegalStateException("Cant create input photo from file reference id: " + this);
        }

        return ImmutableBaseInputPhoto.of(documentId, accessHash)
                .withFileReference(fileReference);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileReferenceId that = (FileReferenceId) o;
        return fileType == that.fileType && dcId == that.dcId
                && documentId == that.documentId && accessHash == that.accessHash
                && thumbVersion == that.thumbVersion && sizeType == that.sizeType
                && fileReference.equals(that.fileReference)
                && thumbSizeType.equals(that.thumbSizeType) && url.equals(that.url)
                && stickerSet.equals(that.stickerSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileType, sizeType, dcId, documentId,
                accessHash, thumbSizeType, url, stickerSet, thumbVersion,
                fileReference);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("FileReferenceId{");
        builder.append("fileType=").append(fileType);

        switch (fileType) {
            case WEB_DOCUMENT:
                builder.append(", ").append("accessHash=").append(accessHash);
                builder.append(", ").append("url='").append(accessHash).append('\'');
                builder.append(", ").append("documentType=").append(documentType);
                builder.append(", ").append("messageId=").append(messageId);
                builder.append(", ").append("peer=").append(peer);
                break;
            case DOCUMENT:
                builder.append(", ").append("dcId=").append(dcId);
                builder.append(", ").append("documentId=").append(documentId);
                builder.append(", ").append("accessHash=").append(accessHash);
                builder.append(", ").append("documentType=").append(documentType);
                builder.append(", ").append("thumbSizeType=").append(thumbSizeType);
                builder.append(", ").append("fileReference='").append(ByteBufUtil.hexDump(fileReference)).append('\'');
                builder.append(", ").append("messageId=").append(messageId);
                builder.append(", ").append("peer=").append(peer);
                break;
            case STICKER_SET_THUMB:
                builder.append(", ").append("stickerSet=").append(stickerSet);
                builder.append(", ").append("thumbVersion=").append(thumbVersion);
                break;
            case CHAT_PHOTO:
                builder.append(", ").append("sizeType=").append(sizeType);
                builder.append(", ").append("dcId=").append(dcId);
                builder.append(", ").append("documentId=").append(documentId);

                if (sizeType == PhotoSizeType.UNKNOWN) {
                    builder.append(", ").append("accessHash=").append(accessHash);
                    builder.append(", ").append("fileReference='").append(ByteBufUtil.hexDump(fileReference)).append('\'');
                    builder.append(", ").append("thumbSizeType=").append(thumbSizeType);
                }

                builder.append(", ").append("messageId=").append(messageId);
                builder.append(", ").append("peer=").append(peer);
                break;
            case PHOTO:
                builder.append(", ").append("dcId=").append(dcId);
                builder.append(", ").append("documentId=").append(documentId);
                builder.append(", ").append("accessHash=").append(accessHash);
                builder.append(", ").append("fileReference='").append(ByteBufUtil.hexDump(fileReference)).append('\'');
                builder.append(", ").append("thumbSizeType=").append(thumbSizeType);
                builder.append(", ").append("messageId=").append(messageId);
                builder.append(", ").append("peer=").append(peer);
                break;
            default:
                throw new IllegalStateException();
        }

        builder.append('}');

        return builder.toString();
    }

    /**
     * Types of peer photo size, i.e. {@link Type#CHAT_PHOTO}.
     * For other file types must be {@code UNKNOWN}.
     */
    public enum PhotoSizeType {
        /** Unknown size type represents documents, photos etc. */
        UNKNOWN,

        /** Small chat photo size type, that's indicated {@link InputPeerPhotoFileLocation#big()} in {@code false} state. */
        CHAT_PHOTO_SMALL,

        /** Big chat photo size type, that's indicated {@link InputPeerPhotoFileLocation#big()} in {@code true} state. */
        CHAT_PHOTO_BIG;

        static final PhotoSizeType[] ALL = values();
    }

    /** Types of web or tg documents. */
    public enum DocumentType {
        /** Not a document type. */
        UNKNOWN,

        /** Default type for all other documents. */
        GENERAL,

        /** Represents document with {@link DocumentAttributeVideo} attribute. */
        VIDEO,

        /** Represents document with {@link DocumentAttributeAnimated} attribute. */
        GIF,

        /**
         * Represents document with {@link DocumentAttributeAudio} attribute
         * and {@link DocumentAttributeAudio#voice} flag in {@code true} state.
         */
        VOICE,

        /** Represents document with {@link DocumentAttributeAudio} attribute. */
        AUDIO,

        /** Represents document with {@link DocumentAttributeSticker} attribute. */
        STICKER;

        static final DocumentType[] ALL = values();

        public static DocumentType fromAttributes(List<DocumentAttribute> attributes) {
            DocumentType type = GENERAL;
            for (DocumentAttribute attribute : attributes) {
                switch (attribute.identifier()) {
                    case DocumentAttributeAnimated.ID: return GIF;
                    case DocumentAttributeAudio.ID: {
                        DocumentAttributeAudio d = (DocumentAttributeAudio) attribute;
                        return d.voice() ? VOICE : AUDIO;
                    }
                    case DocumentAttributeSticker.ID: return STICKER;
                    case DocumentAttributeVideo.ID:
                        type = VIDEO;
                        break;
                }
            }
            return type;
        }
    }

    /** Types of file. */
    public enum Type {
        /** Reserved unknown type. */
        UNKNOWN,

        /** File type, representing all documents. */
        DOCUMENT,

        /** Web file type, representing web documents. */
        WEB_DOCUMENT,

        /** Sticker set thumbnail type. */
        STICKER_SET_THUMB,

        /** Message photo type. */
        PHOTO,

        /** Type, representing chat/channel/user minimal or normal photo. */
        CHAT_PHOTO;

        static final Type[] ALL = values();
    }
}
