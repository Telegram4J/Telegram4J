package telegram4j.mtproto.file;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.base64.Base64Dialect;
import io.netty.util.internal.EmptyArrays;
import reactor.util.annotation.Nullable;
import telegram4j.tl.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * File reference wrapper, which can be serialized to base 64 url string
 * via {@link #serialize(ByteBufAllocator)} and deserialized {@link #deserialize(String)}.
 * For compatibility with rpc method can be mapped to {@code InputPhoto}/{@code InputDocument}, {@code InputFileLocation}.
 */
public class FileReferenceId {

    static final int MESSAGE_ID_MASK = 1 << 0;
    static final int PEER_MASK = 1 << 1;

    private final Type fileType;
    private final PhotoSizeType sizeType;
    private final int dcId;
    private final long photoId;
    private final long accessHash;
    private final byte[] fileReference;
    private final String thumbSizeType;
    private final InputStickerSet stickerSet;
    private final int thumbVersion;

    private final int messageId;
    private final InputPeer peer;

    FileReferenceId(Type fileType, PhotoSizeType sizeType, int dcId, long photoId, long accessHash,
                    byte[] fileReference, String thumbSizeType,
                    InputStickerSet stickerSet, int thumbVersion,
                    int messageId, InputPeer peer) {
        this.fileType = Objects.requireNonNull(fileType, "fileType");
        this.sizeType = Objects.requireNonNull(sizeType, "sizeType");
        this.dcId = dcId;
        this.photoId = photoId;
        this.accessHash = accessHash;
        this.fileReference = Objects.requireNonNull(fileReference, "fileReference");
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
    public static FileReferenceId ofDocument(BaseDocument document, int messageId, InputPeer peer) {
        if (peer.identifier() == InputPeerEmpty.ID) {
            throw new IllegalArgumentException("Unexpected peer type.");
        }
        if (messageId < 0) {
            throw new IllegalArgumentException("Message id must be positive.");
        }

        var thumbs = document.thumbs();
        String firstThumbSize = thumbs != null ? thumbs.get(0).type() : "";

        return new FileReferenceId(Type.DOCUMENT, PhotoSizeType.UNKNOWN,
                document.dcId(), document.id(), document.accessHash(),
                document.fileReference(), firstThumbSize,
                InputStickerSetEmpty.instance(), -1, messageId, peer);
    }

    /**
     * Creates new {@code FileReferenceId} object from given <b>normal</b> photo and source context,
     * with <b>first</b> thumbnail.
     *
     * @throws IllegalArgumentException If peer id is {@link InputPeerEmpty}.
     * @param chatPhoto The chat photo info.
     * @param messageId The source message id, if photo from message, otherwise must be {@code -1}.
     * @param peer The source peer.
     * @return The new {@code FileReferenceId} from given <b>normal</b> photo and source context.
     */
    public static FileReferenceId ofChatPhoto(BasePhoto chatPhoto, int messageId, InputPeer peer) {
        if (peer.identifier() == InputPeerEmpty.ID) {
            throw new IllegalArgumentException("Unexpected peer type.");
        }
        var firstPhotoSize = chatPhoto.sizes().get(0);

        return new FileReferenceId(Type.CHAT_PHOTO, PhotoSizeType.UNKNOWN,
                chatPhoto.dcId(), chatPhoto.id(), chatPhoto.accessHash(),
                chatPhoto.fileReference(), firstPhotoSize.type(),
                InputStickerSetEmpty.instance(), -1, messageId, peer);
    }

    /**
     * Creates new {@code FileReferenceId} object from given minimal chat photo and source context.
     *
     * @throws IllegalArgumentException If {@code sizeType} is {@link PhotoSizeType#UNKNOWN} or peer id is {@link InputPeerEmpty}.
     * @param chatPhoto The chat photo info.
     * @param sizeType The size type of photo, must be {@link PhotoSizeType#CHAT_PHOTO_SMALL} or {@link PhotoSizeType#CHAT_PHOTO_BIG}.
     * @param messageId The source message id, if photo from message, otherwise must be {@code -1}.
     * @param peer The source peer.
     * @return The new {@code FileReferenceId} from given minimal chat photo and source context.
     */
    public static FileReferenceId ofChatPhoto(ChatPhotoFields chatPhoto, PhotoSizeType sizeType,
                                              int messageId, InputPeer peer) {
        if (peer.identifier() == InputPeerEmpty.ID) {
            throw new IllegalArgumentException("Unexpected peer type.");
        }
        if (sizeType == PhotoSizeType.UNKNOWN) {
            throw new IllegalArgumentException("Unexpected size type for chat photo: " + sizeType);
        }

        return new FileReferenceId(Type.CHAT_PHOTO, sizeType, chatPhoto.dcId(), chatPhoto.photoId(), -1,
                EmptyArrays.EMPTY_BYTES, "", InputStickerSetEmpty.instance(), -1, messageId, peer);
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
        var firstPhotoSize = photo.sizes().get(0);

        return new FileReferenceId(Type.PHOTO, PhotoSizeType.UNKNOWN, photo.dcId(), photo.id(), photo.accessHash(),
                photo.fileReference(), firstPhotoSize.type(),
                InputStickerSetEmpty.instance(), -1, messageId, peer);
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
            throw new IllegalArgumentException("Unexpected peer type.");
        }
        // TODO: test and add condition for version
        return new FileReferenceId(Type.STICKER_SET_THUMB, PhotoSizeType.UNKNOWN, -1, -1, -1,
                EmptyArrays.EMPTY_BYTES, "", stickerSet, version, -1, InputPeerEmpty.instance());
    }

    /**
     * Deserializes reference to {@code FileReferenceId}
     * with decoded information about file.
     *
     * @param str The base 64 url string.
     * @return The deserialized {@code FileReferenceId} with decoded information.
     */
    public static FileReferenceId deserialize(String str) {
        ByteBuf data = Unpooled.wrappedBuffer(str.getBytes(StandardCharsets.UTF_8));
        ByteBuf buf = Base64.decode(data, Base64Dialect.URL_SAFE);
        data.release();

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

        switch (fileType) {
            case DOCUMENT:
            case PHOTO: {
                int dcId = buf.readByte();
                long photoId = buf.readLongLE();
                long accessHash = buf.readLongLE();

                byte[] fileReference = TlSerialUtil.deserializeBytes(buf);
                String thumbSizeType = TlSerialUtil.deserializeString(buf);

                return new FileReferenceId(fileType, PhotoSizeType.UNKNOWN, dcId, photoId, accessHash,
                        fileReference, thumbSizeType, InputStickerSetEmpty.instance(),
                        -1, messageId, peer);
            }
            case CHAT_PHOTO: {
                PhotoSizeType sizeType = PhotoSizeType.ALL[buf.readByte()];
                int dcId = buf.readByte();
                long photoId = buf.readLongLE();

                return new FileReferenceId(fileType, sizeType, dcId, photoId, -1,
                        EmptyArrays.EMPTY_BYTES, "", InputStickerSetEmpty.instance(),
                        -1, messageId, peer);
            }
            case STICKER_SET_THUMB: {
                int thumbVersion = buf.readIntLE();
                InputStickerSet stickerSet = TlDeserializer.deserialize(buf);

                return new FileReferenceId(fileType, PhotoSizeType.UNKNOWN, -1, -1, -1,
                        EmptyArrays.EMPTY_BYTES, "", stickerSet, thumbVersion, messageId, peer);
            }
            default:
                throw new IllegalStateException("Malformed file reference id.");
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
    public long getPhotoId() {
        return photoId;
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
     * Gets a hex dump of file reference, if file has it.
     *
     * @return The hex dump of file reference, if applicable, otherwise {@code ""}.
     */
    public String getFileReference() {
        return ByteBufUtil.hexDump(fileReference);
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
     * Gets the source message id, if file from message.
     *
     * @return The source message id, if applicable, otherwise {@code -1}.
     */
    public int getMessageId() {
        return messageId;
    }

    /**
     * Serializes reference to base 64 url string,
     * which can be deserialized via {@link #deserialize(String)}.
     *
     * @param alloc The byte buf allocator.
     * @return The serialized base 64 url identifier string of file reference.
     */
    public String serialize(ByteBufAllocator alloc) {
        ByteBuf buf = alloc.buffer();
        buf.writeByte((byte) fileType.ordinal());

        byte flags = (byte) ((messageId != -1 ? MESSAGE_ID_MASK : 0) | (peer.identifier() != InputPeerEmpty.ID ? PEER_MASK : 0));
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
            case DOCUMENT:
            case PHOTO: {
                ByteBuf fileReferenceBuf = TlSerialUtil.serializeBytes(alloc, fileReference);
                ByteBuf thumbSizeTypeBuf = TlSerialUtil.serializeString(alloc, thumbSizeType);

                buf.writeByte((byte) dcId);
                buf.writeLongLE(photoId);
                buf.writeLongLE(accessHash);

                buf.writeBytes(fileReferenceBuf);
                fileReferenceBuf.release();
                buf.writeBytes(thumbSizeTypeBuf);
                thumbSizeTypeBuf.release();

                break;
            }
            case CHAT_PHOTO: {
                buf.writeByte((byte) sizeType.ordinal());
                buf.writeByte((byte) dcId);
                buf.writeLongLE(photoId);

                if (sizeType == PhotoSizeType.UNKNOWN) {
                    ByteBuf fileReferenceBuf = TlSerialUtil.serializeBytes(alloc, fileReference);
                    ByteBuf thumbSizeTypeBuf = TlSerialUtil.serializeString(alloc, thumbSizeType);

                    buf.writeLongLE(accessHash);
                    buf.writeBytes(fileReferenceBuf);
                    fileReferenceBuf.release();
                    buf.writeBytes(thumbSizeTypeBuf);
                    thumbSizeTypeBuf.release();
                }

                break;
            }
            case STICKER_SET_THUMB:
                ByteBuf stickerSetBuf = TlSerializer.serialize(alloc, stickerSet);

                buf.writeIntLE(thumbVersion);
                buf.writeBytes(stickerSetBuf);
                stickerSetBuf.release();

                break;
        }

        ByteBuf base64 = Base64.encode(buf, Base64Dialect.URL_SAFE);
        buf.release();
        try {
            return base64.toString(StandardCharsets.UTF_8);
        } finally {
            base64.release();
        }
    }

    /**
     * Creates a {@link InputFileLocation} from this reference,
     * which used in {@link telegram4j.tl.request.upload.GetFile} method.
     *
     * @return The new {@link InputFileLocation} from this reference.
     */
    public InputFileLocation asLocation() {
        switch (fileType) {
            case CHAT_PHOTO:
                if (fileReference != EmptyArrays.EMPTY_BYTES) { // is full image
                    return InputPhotoFileLocation.builder()
                            .accessHash(accessHash)
                            .fileReference(fileReference)
                            .id(photoId)
                            .thumbSize(thumbSizeType)
                            .build();
                }

                return InputPeerPhotoFileLocation.builder()
                        .peer(peer)
                        .photoId(photoId)
                        .big(sizeType == PhotoSizeType.CHAT_PHOTO_BIG)
                        .build();
            case PHOTO:
                return InputPhotoFileLocation.builder()
                        .accessHash(accessHash)
                        .fileReference(fileReference)
                        .id(photoId)
                        .thumbSize(thumbSizeType)
                        .build();
            case STICKER_SET_THUMB:
                return InputStickerSetThumb.builder()
                        .stickerset(stickerSet)
                        .thumbVersion(thumbVersion)
                        .build();
            case DOCUMENT:
                return InputDocumentFileLocation.builder()
                        .accessHash(accessHash)
                        .fileReference(fileReference)
                        .id(photoId)
                        .thumbSize(thumbSizeType)
                        .build();
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileReferenceId that = (FileReferenceId) o;
        return dcId == that.dcId && photoId == that.photoId &&
                accessHash == that.accessHash && thumbVersion == that.thumbVersion &&
                messageId == that.messageId && fileType == that.fileType &&
                sizeType == that.sizeType && Arrays.equals(fileReference, that.fileReference) &&
                thumbSizeType.equals(that.thumbSizeType) &&
                stickerSet.equals(that.stickerSet) && peer.equals(that.peer);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(fileType, sizeType, dcId, photoId, accessHash,
                thumbSizeType, stickerSet, thumbVersion, messageId, peer);
        result = 31 * result + Arrays.hashCode(fileReference);
        return result;
    }

    /**
     * Creates a {@link ImmutableBaseInputDocument} from this reference if file type is {@link Type#DOCUMENT}.
     *
     * @throws IllegalStateException If tried to create input document from non-document reference.
     * @return The new {@link ImmutableBaseInputDocument} from this reference.
     */
    public ImmutableBaseInputDocument asInputDocument() {
        if (fileType != Type.DOCUMENT) {
            throw new IllegalStateException("Cant create input document from file reference id.");
        }

        return ImmutableBaseInputDocument.of(photoId, accessHash, fileReference);
    }

    /**
     * Creates a {@link ImmutableBaseInputPhoto} from this reference if file type is {@link Type#PHOTO}.
     *
     * @throws IllegalStateException If tried to create input photo from non-photo reference.
     * @return The new {@link ImmutableBaseInputPhoto} from this reference.
     */
    public ImmutableBaseInputPhoto asInputPhoto() {
        if (fileType != Type.PHOTO) {
            throw new IllegalStateException("Cant create input document from file reference id.");
        }

        return ImmutableBaseInputPhoto.of(photoId, accessHash, fileReference);
    }

    /**
     * Types of peer photo size, i.e. {@link Type#CHAT_PHOTO}.
     * For other file types must be {@code UNKNOWN}.
     */
    public enum PhotoSizeType {
        /** Unknown size type represents documents, photos etc. */
        UNKNOWN,

        /** Small chat photo size type, that's indicated {@link InputPeerPhotoFileLocation#big()} in {@literal false} state. */
        CHAT_PHOTO_SMALL,

        /** Big chat photo size type, that's indicated {@link InputPeerPhotoFileLocation#big()} in {@literal true} state. */
        CHAT_PHOTO_BIG;

        private static final PhotoSizeType[] ALL = values();
    }

    /** Types of file. */
    public enum Type {
        /** Reserved unknown type. */
        UNKNOWN,

        /** File type, representing all documents. */
        DOCUMENT,

        /** Sticker set thumbnail type. */
        STICKER_SET_THUMB,

        /** Message photo type. */
        PHOTO,

        /** Type, representing chat/channel/user minimal or normal photo. */
        CHAT_PHOTO;

        private static final Type[] ALL = values();
    }
}
