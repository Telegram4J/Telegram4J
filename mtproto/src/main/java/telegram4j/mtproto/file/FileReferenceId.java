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
 * File reference wrapper, which can be serialized to {@code base64url} string
 * via {@link #serialize()} and deserialized via {@link #deserialize(String)}.
 * For compatibility with rpc methods can be mapped to {@link InputPhoto}/{@link InputDocument},
 * {@link InputFileLocation}, {@link InputWebFileLocation}.
 *
 * @apiNote This identifier can't be used across different accounts
 * due to the relativity of message identifiers in private chats
 * and groups, as well as other privacy factors.
 */
public class FileReferenceId {

    static final String PREFIX = "x74JF1D"; // xT4JFID
    static final byte MAX_RLE_SEQ = Byte.MAX_VALUE;

    static final byte SIZE_TYPE_ABSENT = -1;
    static final byte SIZE_TYPE_SMALL = 4;
    static final byte SIZE_TYPE_BIG = 5;

    static final int MESSAGE_ID_MASK = 1;
    static final int PEER_MASK = 1 << 1;
    static final int ACCESS_HASH_MASK = 1 << 2;
    static final int THUMB_SIZE_TYPE_MASK = 1 << 3;
    static final int SIZE_TYPE_SMALL_MASK = 1 << 4;
    static final int SIZE_TYPE_BIG_MASK = 1 << 5;

    private final Type fileType;
    @Nullable
    private final DocumentType documentType;
    private final byte sizeType;
    private final int dcId;
    private final long documentId;
    private final long accessHash;
    @Nullable
    private final ByteBuf fileReference;
    private final char thumbSizeType;
    @Nullable
    private final String url;
    private final InputStickerSet stickerSet;
    private final int thumbVersion;

    private final int messageId;
    @Nullable
    private final InputPeer peer;

    FileReferenceId(Type fileType, @Nullable DocumentType documentType, byte sizeType,
                    int dcId, long documentId, long accessHash,
                    @Nullable ByteBuf fileReference, char thumbSizeType,
                    @Nullable String url, @Nullable InputStickerSet stickerSet, int thumbVersion,
                    int messageId, @Nullable InputPeer peer) {
        this.fileType = Objects.requireNonNull(fileType);
        this.documentType = documentType;
        this.sizeType = sizeType;
        this.dcId = dcId;
        this.documentId = documentId;
        this.accessHash = accessHash;
        this.fileReference = fileReference;
        this.url = url;
        this.thumbSizeType = thumbSizeType;
        this.stickerSet = stickerSet;
        this.thumbVersion = thumbVersion;

        this.messageId = messageId;
        this.peer = peer;
    }

    private static char asChar(String type) {
        if (type.length() != 1) {
            throw new IllegalArgumentException("unknown format of the photo size type: '" + type + "'");
        }
        return type.charAt(0);
    }

    /**
     * Creates new {@code FileReferenceId} object from given web document and source context.
     *
     * @throws IllegalArgumentException If peer id is {@link InputPeerEmpty} or message id is negative.
     * @param document The document info.
     * @param messageId The source message id.
     * @param peer The message source peer.
     * @return The new {@code FileReferenceId} from given web document and source context.
     */
    public static FileReferenceId ofDocument(WebDocument document, int messageId, InputPeer peer) {
        if (peer.identifier() == InputPeerEmpty.ID) {
            throw new IllegalArgumentException("Unexpected peer type.");
        }
        if (messageId < 0) {
            throw new IllegalArgumentException("Message id must be positive.");
        }

        long accessHash = document.identifier() == BaseWebDocument.ID
                ? ((BaseWebDocument) document).accessHash()
                : -1;
        DocumentType documentType = DocumentType.fromAttributes(document.attributes());

        return new FileReferenceId(Type.WEB_DOCUMENT, documentType, SIZE_TYPE_ABSENT,
                -1, -1, accessHash, null, '\0', document.url(),
                null, -1, messageId, peer);
    }

    /**
     * Creates new {@code FileReferenceId} object from given document and source context,
     * with <b>first</b> video or static thumbnail.
     *
     * @throws IllegalArgumentException If peer id is {@link InputPeerEmpty} or message id is negative.
     * @param document The document info.
     * @param messageId The source message id.
     * @param peer The message source peer.
     * @return The new {@code FileReferenceId} from given document and source context.
     */
    public static FileReferenceId ofDocument(BaseDocument document, int messageId, InputPeer peer) {
        char thumbSizeType = Optional.ofNullable(document.videoThumbs())
                .map(d -> asChar(d.get(0).type()))
                .or(() -> Optional.ofNullable(document.thumbs())
                        .map(d -> asChar(d.get(0).type())))
                .orElseThrow();

        return ofDocument(document, thumbSizeType, messageId, peer);
    }

    /**
     * Creates new {@code FileReferenceId} object from given document and source context.
     *
     * @throws IllegalArgumentException If peer id is {@link InputPeerEmpty} or message id is negative.
     * @param document The document info.
     * @param thumbSizeType The type of thumbnail used for downloading.
     * @param messageId The source message id.
     * @param peer The message source peer.
     * @return The new {@code FileReferenceId} from given document and source context.
     */
    public static FileReferenceId ofDocument(BaseDocument document, char thumbSizeType, int messageId, InputPeer peer) {
        if (peer.identifier() == InputPeerEmpty.ID) {
            throw new IllegalArgumentException("Unexpected peer type.");
        }
        if (messageId < 0) {
            throw new IllegalArgumentException("Message id must be positive.");
        }

        DocumentType documentType = DocumentType.fromAttributes(document.attributes());
        ByteBuf fileReference = TlEncodingUtil.copyAsUnpooled(document.fileReference());
        return new FileReferenceId(Type.DOCUMENT, documentType, SIZE_TYPE_ABSENT,
                document.dcId(), document.id(), document.accessHash(), fileReference, thumbSizeType, null,
                null, -1, messageId, peer);
    }

    /**
     * Creates new {@code FileReferenceId} object from given <b>normal</b> photo and source context,
     * with <b>first</b> video or static thumbnail.
     *
     * @throws IllegalArgumentException If peer id is {@link InputPeerEmpty}.
     * @param chatPhoto The chat photo info.
     * @param messageId The source message id, if photo from message, otherwise must be {@code -1}.
     * @param peer The chat/channel peer where photo was sent.
     * @return The new {@code FileReferenceId} from given <b>normal</b> photo and source context.
     */
    public static FileReferenceId ofChatPhoto(BasePhoto chatPhoto, int messageId, InputPeer peer) {
        char thumbSizeType = Optional.ofNullable(chatPhoto.videoSizes())
                .map(d -> asChar(d.get(0).type()))
                .orElseGet(() -> asChar(chatPhoto.sizes().get(0).type()));

        return ofChatPhoto(chatPhoto, thumbSizeType, messageId, peer);
    }

    /**
     * Creates new {@code FileReferenceId} object from given <b>normal</b> photo and source context.
     *
     * @throws IllegalArgumentException If peer id is {@link InputPeerEmpty}.
     * @param chatPhoto The chat photo info.
     * @param thumbSizeType The type of thumbnail used for downloading.
     * @param messageId The source message id, if photo from message, otherwise must be {@code -1}.
     * @param peer The chat/channel peer where photo was sent.
     * @return The new {@code FileReferenceId} from given <b>normal</b> photo and source context.
     */
    public static FileReferenceId ofChatPhoto(BasePhoto chatPhoto, char thumbSizeType, int messageId, InputPeer peer) {
        if (peer.identifier() == InputPeerEmpty.ID) {
            throw new IllegalArgumentException("Unexpected peer type: " + peer);
        }

        ByteBuf fileReference = TlEncodingUtil.copyAsUnpooled(chatPhoto.fileReference());
        return new FileReferenceId(Type.CHAT_PHOTO, null, SIZE_TYPE_ABSENT,
                chatPhoto.dcId(), chatPhoto.id(), chatPhoto.accessHash(),
                fileReference, thumbSizeType, null, null, -1, messageId, peer);
    }

    /**
     * Creates new {@code FileReferenceId} object from given minimal chat photo and source context.
     *
     * @param chatPhoto The chat photo info.
     * @param big When chat photo is big.
     * @param messageId The source message id, if photo from message, otherwise must be {@code -1}.
     * @param peer The peer that's have this photo.
     * @return The new {@code FileReferenceId} from given minimal chat photo and source context.
     */
    public static FileReferenceId ofChatPhoto(ChatPhotoFields chatPhoto, boolean big, int messageId, InputPeer peer) {
        if (peer.identifier() == InputPeerEmpty.ID) {
            throw new IllegalArgumentException("Unexpected peer type: " + peer);
        }

        byte sizeType = big ? SIZE_TYPE_BIG : SIZE_TYPE_SMALL;
        return new FileReferenceId(Type.CHAT_PHOTO, null, sizeType,
                chatPhoto.dcId(), chatPhoto.photoId(), -1,
                null, '\0', null,
                null, -1, messageId, peer);
    }

    /**
     * Creates new {@code FileReferenceId} object from given <b>message</b> photo and source context,
     * with <b>first</b> video or static thumbnail.
     *
     * @throws IllegalArgumentException If peer id is {@link InputPeerEmpty} or message id is negative.
     * @param photo The photo object.
     * @param messageId The source message id, if photo from message, otherwise must be {@code -1}.
     * @param peer The message source peer.
     * @return The new {@code FileReferenceId} from given photo and source context.
     */
    public static FileReferenceId ofPhoto(BasePhoto photo, int messageId, InputPeer peer) {
        char thumbSizeType = Optional.ofNullable(photo.videoSizes())
                .map(d -> asChar(d.get(0).type()))
                .orElseGet(() -> asChar(photo.sizes().get(0).type()));

        return ofPhoto(photo, thumbSizeType, messageId, peer);
    }

    /**
     * Creates new {@code FileReferenceId} object from given <b>message</b> photo and source context.
     *
     * @throws IllegalArgumentException If peer id is {@link InputPeerEmpty} or message id is negative.
     * @param photo The photo object.
     * @param thumbSizeType The type of thumbnail used for downloading.
     * @param messageId The source message id, if photo from message, otherwise must be {@code -1}.
     * @param peer The message source peer.
     * @return The new {@code FileReferenceId} from given photo and source context.
     */
    public static FileReferenceId ofPhoto(BasePhoto photo, char thumbSizeType, int messageId, InputPeer peer) {
        if (peer.identifier() == InputPeerEmpty.ID) {
            throw new IllegalArgumentException("Unexpected peer type.");
        }
        if (messageId < 0) {
            throw new IllegalArgumentException("Message id must be positive.");
        }

        ByteBuf fileReference = TlEncodingUtil.copyAsUnpooled(photo.fileReference());
        return new FileReferenceId(Type.PHOTO, null,
                SIZE_TYPE_ABSENT, photo.dcId(), photo.id(), photo.accessHash(),
                fileReference, thumbSizeType,
                null, null, -1, messageId, peer);
    }

    /**
     * Creates new {@code FileReferenceId} object with sticker set thumbnail from given their id.
     *
     * @throws IllegalArgumentException If sticker set id is {@link InputStickerSetEmpty} or {@code version} is negative.
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

        return new FileReferenceId(Type.STICKER_SET_THUMB, null,
                SIZE_TYPE_ABSENT, -1, -1, -1,
                null, '\0', null, stickerSet,
                version, -1, null);
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
        if (!str.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Incorrect file reference id format: '" + str + "'");
        }

        ByteBuf data = Unpooled.wrappedBuffer(str.getBytes(StandardCharsets.UTF_8))
                .skipBytes(PREFIX.length());

        ByteBuf buf = Base64.decode(data, Base64Dialect.URL_SAFE);
        data.release();
        buf = decodeZeroRle(buf);

        Type fileType = Type.ALL[buf.readByte()];
        byte flags = buf.readByte();
        int messageId = -1;
        if ((flags & MESSAGE_ID_MASK) != 0) {
            messageId = buf.readIntLE();
        }

        InputPeer peer = null;
        if ((flags & PEER_MASK) != 0) {
            peer = TlDeserializer.deserialize(buf);
        }

        byte sizeType = (flags & SIZE_TYPE_BIG_MASK) != 0 ? SIZE_TYPE_BIG :
                (flags & SIZE_TYPE_SMALL_MASK) != 0 ? SIZE_TYPE_SMALL : SIZE_TYPE_ABSENT;

        String url = null;
        long accessHash = -1;
        int dcId = -1;
        long documentId = -1;
        DocumentType documentType = null;
        ByteBuf fileReference = null;
        char thumbSizeType = '\0';
        InputStickerSet stickerSet = null;
        int thumbVersion = -1;

        switch (fileType) {
            case WEB_DOCUMENT:
                documentType = DocumentType.ALL[buf.readByte()];

                url = TlSerialUtil.deserializeString(buf);

                if ((flags & ACCESS_HASH_MASK) != 0) {
                    accessHash = buf.readLongLE();
                }
                break;
            case DOCUMENT:
                documentType = DocumentType.ALL[buf.readByte()];

            case PHOTO:
                dcId = buf.readUnsignedByte();
                documentId = buf.readLongLE();
                accessHash = buf.readLongLE();

                fileReference = TlEncodingUtil.copyAsUnpooled(TlSerialUtil.deserializeBytes(buf));
                if ((flags & THUMB_SIZE_TYPE_MASK) != 0) {
                    thumbSizeType = (char) buf.readByte();
                }

                break;
            case CHAT_PHOTO:
                dcId = buf.readUnsignedByte();
                documentId = buf.readLongLE();

                if (sizeType == SIZE_TYPE_ABSENT) {
                    accessHash = buf.readLongLE();
                    fileReference = TlEncodingUtil.copyAsUnpooled(TlSerialUtil.deserializeBytes(buf));
                    if ((flags & THUMB_SIZE_TYPE_MASK) != 0) {
                        thumbSizeType = (char) buf.readByte();
                    }
                }

                break;
            case STICKER_SET_THUMB:
                thumbVersion = buf.readIntLE();
                stickerSet = TlDeserializer.deserialize(buf);

                break;
        }

        if (buf.isReadable()) {
            buf.release();
            throw new IllegalArgumentException("Malformed file reference id: " + str);
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
                byte c = 1;
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

        byte flags = 0;
        if (messageId != -1) {
            flags |= MESSAGE_ID_MASK;
        }
        if (peer != null) {
            flags |= PEER_MASK;
        }
        if (fileType == Type.WEB_DOCUMENT && accessHash != -1) {
            flags |= ACCESS_HASH_MASK;
        }
        if (fileType == Type.CHAT_PHOTO && sizeType != SIZE_TYPE_ABSENT) {
            flags |= 1 << sizeType;
        }
        if ((fileType == Type.DOCUMENT || fileType == Type.PHOTO ||
                fileType == Type.CHAT_PHOTO && sizeType == SIZE_TYPE_ABSENT) &&
                thumbSizeType != '\0') {
            flags |= THUMB_SIZE_TYPE_MASK;
        }

        buf.writeByte(flags);
        if ((flags & MESSAGE_ID_MASK) != 0) {
            buf.writeIntLE(messageId);
        }

        if ((flags & PEER_MASK) != 0) {
            Objects.requireNonNull(peer);
            ByteBuf peerIdBuf = TlSerializer.serialize(alloc, peer);
            buf.writeBytes(peerIdBuf);
            peerIdBuf.release();
        }

        switch (fileType) {
            case WEB_DOCUMENT:
                Objects.requireNonNull(documentType);
                buf.writeByte((byte) documentType.ordinal());

                Objects.requireNonNull(url);
                ByteBuf urlBuf = TlSerialUtil.serializeString(alloc, url);
                buf.writeBytes(urlBuf);
                urlBuf.release();

                if ((flags & ACCESS_HASH_MASK) != 0) {
                    buf.writeLongLE(accessHash);
                }
                break;
            case DOCUMENT:
                Objects.requireNonNull(documentType);
                buf.writeByte((byte) documentType.ordinal());

            case PHOTO: {
                buf.writeByte((byte) dcId);
                buf.writeLongLE(documentId);
                buf.writeLongLE(accessHash);

                Objects.requireNonNull(fileReference);
                ByteBuf fileReferenceBuf = TlSerialUtil.serializeBytes(alloc, fileReference);
                buf.writeBytes(fileReferenceBuf);
                fileReferenceBuf.release();

                if ((flags & THUMB_SIZE_TYPE_MASK) != 0) {
                    buf.writeByte(thumbSizeType);
                }

                break;
            }
            case CHAT_PHOTO:
                buf.writeByte((byte) dcId);
                buf.writeLongLE(documentId);

                if (sizeType == SIZE_TYPE_ABSENT) {
                    buf.writeLongLE(accessHash);

                    Objects.requireNonNull(fileReference);
                    ByteBuf fileReferenceBuf = TlSerialUtil.serializeBytes(alloc, fileReference);
                    buf.writeBytes(fileReferenceBuf);
                    fileReferenceBuf.release();

                    if ((flags & THUMB_SIZE_TYPE_MASK) != 0) {
                        buf.writeByte(thumbSizeType);
                    }
                }

                break;
            case STICKER_SET_THUMB:

                buf.writeIntLE(thumbVersion);

                Objects.requireNonNull(stickerSet);
                ByteBuf stickerSetBuf = TlSerializer.serialize(alloc, stickerSet);
                buf.writeBytes(stickerSetBuf);
                stickerSetBuf.release();

                break;
            default: throw new IllegalStateException();
        }

        buf = encodeZeroRle(buf);
        ByteBuf base64 = Base64.encode(buf, Base64Dialect.URL_SAFE);
        buf.release();
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
     * Gets whether chat photo is big, if chat photo is it.
     *
     * @return {@code true} if chat photo is big.
     */
    public boolean isBig() {
        return sizeType == SIZE_TYPE_BIG;
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
     * @return The <i>immutable</i> {@link ByteBuf} of file reference, if applicable.
     */
    public Optional<ByteBuf> getFileReference() {
        return Optional.ofNullable(fileReference).map(ByteBuf::duplicate);
    }

    /**
     * Gets the source peer id, if applicable.
     *
     * @return The source peer id, if applicable.
     */
    public Optional<InputPeer> getPeer() {
        return Optional.ofNullable(peer);
    }

    /**
     * Gets the thumbnail transformation type, if file has it.
     *
     * @return The thumbnail transformation type, if applicable, otherwise {@code '\0'}.
     */
    public char getThumbSizeType() {
        return thumbSizeType;
    }

    /**
     * Gets the web document url, if file has it.
     *
     * @return The url of web document, if applicable.
     */
    public Optional<String> getUrl() {
        return Optional.ofNullable(url);
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
     * @return The {@link DocumentType} of file, if applicable
     */
    public Optional<DocumentType> getDocumentType() {
        return Optional.ofNullable(documentType);
    }

    /**
     * Creates a {@link InputWebFileLocation} from this reference,
     * which used in {@link telegram4j.tl.request.upload.GetWebFile} method, if access hash present, and it's web file
     *
     * @return The new {@link InputWebFileLocation} from this reference, if access hash present, and it's web file
     */
    public Optional<InputWebFileLocation> asWebLocation() {
        if (fileType != Type.WEB_DOCUMENT || accessHash == -1) {
            return Optional.empty();
        }
        Objects.requireNonNull(url);
        return Optional.of(ImmutableBaseInputWebFileLocation.of(url, accessHash));
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
                if (sizeType == SIZE_TYPE_ABSENT) { // is full image
                    Objects.requireNonNull(fileReference);
                    return Optional.of(InputPhotoFileLocation.builder()
                            .accessHash(accessHash)
                            .fileReference(fileReference)
                            .id(documentId)
                            .thumbSize(String.valueOf(thumbSizeType))
                            .build());
                }

                Objects.requireNonNull(peer);
                return Optional.of(InputPeerPhotoFileLocation.builder()
                        .peer(peer)
                        .photoId(documentId)
                        .big(sizeType == SIZE_TYPE_BIG)
                        .build());
            case PHOTO:
                Objects.requireNonNull(fileReference);
                return Optional.of(InputPhotoFileLocation.builder()
                        .accessHash(accessHash)
                        .fileReference(fileReference)
                        .id(documentId)
                        .thumbSize(String.valueOf(thumbSizeType))
                        .build());
            case STICKER_SET_THUMB:
                Objects.requireNonNull(stickerSet);
                return Optional.of(InputStickerSetThumb.builder()
                        .stickerset(stickerSet)
                        .thumbVersion(thumbVersion)
                        .build());
            case DOCUMENT:
                Objects.requireNonNull(fileReference);
                return Optional.of(InputDocumentFileLocation.builder()
                        .accessHash(accessHash)
                        .fileReference(fileReference)
                        .id(documentId)
                        .thumbSize(String.valueOf(thumbSizeType))
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

        Objects.requireNonNull(fileReference);
        return ImmutableBaseInputDocument.of(documentId, accessHash, fileReference);
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

        Objects.requireNonNull(fileReference);
        return ImmutableBaseInputPhoto.of(documentId, accessHash, fileReference);
    }

    public FileReferenceId withThumbSizeType(char thumbSizeType) {
        if (fileType != Type.DOCUMENT && fileType != Type.PHOTO && (fileType != Type.CHAT_PHOTO || sizeType != SIZE_TYPE_ABSENT)) {
            throw new IllegalStateException("Thumb size type can't be set for file ref id: " + this);
        }
        if (thumbSizeType == this.thumbSizeType) return this;
        return new FileReferenceId(fileType, documentType, sizeType, dcId, documentId, accessHash,
                fileReference, thumbSizeType, url, stickerSet, thumbVersion, messageId, peer);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileReferenceId that = (FileReferenceId) o;
        return sizeType == that.sizeType && dcId == that.dcId &&
                documentId == that.documentId && accessHash == that.accessHash &&
                thumbSizeType == that.thumbSizeType && thumbVersion == that.thumbVersion &&
                messageId == that.messageId && fileType == that.fileType &&
                documentType == that.documentType &&
                Objects.equals(fileReference, that.fileReference) &&
                Objects.equals(url, that.url) &&
                Objects.equals(stickerSet, that.stickerSet) &&
                Objects.equals(peer, that.peer);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + fileType.hashCode();
        h += (h << 5) + Objects.hashCode(documentType);
        h += (h << 5) + sizeType;
        h += (h << 5) + dcId;
        h += (h << 5) + Long.hashCode(documentId);
        h += (h << 5) + Long.hashCode(accessHash);
        h += (h << 5) + Objects.hashCode(fileReference);
        h += (h << 5) + Character.hashCode(thumbSizeType);
        h += (h << 5) + Objects.hashCode(url);
        h += (h << 5) + Objects.hashCode(stickerSet);
        h += (h << 5) + thumbVersion;
        h += (h << 5) + messageId;
        h += (h << 5) + Objects.hashCode(peer);
        return h;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("FileReferenceId{");
        builder.append("fileType=").append(fileType);

        switch (fileType) {
            case WEB_DOCUMENT:
                builder.append(", ").append("documentType=").append(documentType);
                builder.append(", ").append("url='").append(url).append('\'');
                if (accessHash != -1) {
                    builder.append(", ").append("accessHash=").append(accessHash);
                }
                builder.append(", ").append("messageId=").append(messageId);
                builder.append(", ").append("peer=").append(peer);
                break;
            case DOCUMENT:
                builder.append(", ").append("dcId=").append(dcId);
                builder.append(", ").append("documentId=").append(documentId);
                builder.append(", ").append("accessHash=").append(accessHash);
                builder.append(", ").append("documentType=").append(documentType);
                builder.append(", ").append("thumbSizeType=").append(thumbSizeType);
                Objects.requireNonNull(fileReference);
                builder.append(", ").append("fileReference='").append(ByteBufUtil.hexDump(fileReference)).append('\'');
                builder.append(", ").append("messageId=").append(messageId);
                builder.append(", ").append("peer=").append(peer);
                break;
            case STICKER_SET_THUMB:
                builder.append(", ").append("stickerSet=").append(stickerSet);
                builder.append(", ").append("thumbVersion=").append(thumbVersion);
                break;
            case CHAT_PHOTO:
                builder.append(", ").append("dcId=").append(dcId);
                builder.append(", ").append("documentId=").append(documentId);

                if (sizeType == SIZE_TYPE_ABSENT) {
                    builder.append(", ").append("accessHash=").append(accessHash);
                    Objects.requireNonNull(fileReference);
                    builder.append(", ").append("fileReference='").append(ByteBufUtil.hexDump(fileReference)).append('\'');
                    builder.append(", ").append("thumbSizeType=").append(thumbSizeType);
                } else {
                    builder.append(", ").append("big=").append(sizeType == SIZE_TYPE_BIG);
                }

                builder.append(", ").append("messageId=").append(messageId);
                builder.append(", ").append("peer=").append(peer);
                break;
            case PHOTO:
                builder.append(", ").append("dcId=").append(dcId);
                builder.append(", ").append("documentId=").append(documentId);
                builder.append(", ").append("accessHash=").append(accessHash);
                Objects.requireNonNull(fileReference);
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

    /** Types of web or tg documents. */
    public enum DocumentType {

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
        STICKER,

        /** Represents document with {@link DocumentAttributeCustomEmoji} attribute. */
        EMOJI;

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
                    case DocumentAttributeCustomEmoji.ID: return EMOJI;
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
