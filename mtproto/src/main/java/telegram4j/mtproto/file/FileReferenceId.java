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

    public static FileReferenceId ofDocument(BaseDocument document, int messageId, InputPeer peer) {
        var thumbs = document.thumbs();
        String firstThumbSize = thumbs != null ? thumbs.get(0).type() : "";

        return new FileReferenceId(Type.DOCUMENT, PhotoSizeType.UNKNOWN, document.dcId(), document.id(), document.accessHash(),
                document.fileReference(), firstThumbSize,
                InputStickerSetEmpty.instance(), -1, messageId, peer);
    }

    public static FileReferenceId ofChatPhoto(BasePhoto chatPhoto, int messageId, InputPeer peer) {
        var firstPhotoSize = chatPhoto.sizes().get(0);

        return new FileReferenceId(Type.CHAT_PHOTO, PhotoSizeType.UNKNOWN, chatPhoto.dcId(), chatPhoto.id(), chatPhoto.accessHash(),
                chatPhoto.fileReference(), firstPhotoSize.type(),
                InputStickerSetEmpty.instance(), -1, messageId, peer);
    }

    public static FileReferenceId ofChatPhoto(ChatPhotoFields chatPhoto, PhotoSizeType sizeType,
                                              int messageId, InputPeer peer) {
        if (sizeType != PhotoSizeType.CHAT_PHOTO_BIG && sizeType != PhotoSizeType.CHAT_PHOTO_SMALL) {
            throw new IllegalArgumentException("Unexpected size type for chat photo: " + sizeType);
        }

        return new FileReferenceId(Type.CHAT_PHOTO, sizeType, chatPhoto.dcId(), chatPhoto.photoId(), -1,
                EmptyArrays.EMPTY_BYTES, "", InputStickerSetEmpty.instance(), -1, messageId, peer);
    }

    public static FileReferenceId ofPhoto(BasePhoto photo, int messageId, InputPeer peer) {
        var firstPhotoSize = photo.sizes().get(0);

        return new FileReferenceId(Type.PHOTO, PhotoSizeType.UNKNOWN, photo.dcId(), photo.id(), photo.accessHash(),
                photo.fileReference(), firstPhotoSize.type(),
                InputStickerSetEmpty.instance(), -1, messageId, peer);
    }

    public static FileReferenceId ofStickerSet(InputStickerSet stickerSet, int version) {
        return new FileReferenceId(Type.STICKER_SET_THUMB, PhotoSizeType.UNKNOWN, -1, -1, -1,
                EmptyArrays.EMPTY_BYTES, "", stickerSet, version, -1, InputPeerEmpty.instance());
    }

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

    public Type getFileType() {
        return fileType;
    }

    public PhotoSizeType getSizeType() {
        return sizeType;
    }

    public int getDcId() {
        return dcId;
    }

    public long getPhotoId() {
        return photoId;
    }

    public long getAccessHash() {
        return accessHash;
    }

    public String getFileReference() {
        return ByteBufUtil.hexDump(fileReference);
    }

    public InputPeer getPeer() {
        return peer;
    }

    public String getThumbSizeType() {
        return thumbSizeType;
    }

    public int getMessageId() {
        return messageId;
    }

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

        try {
            return Base64.encode(buf, Base64Dialect.URL_SAFE).toString(StandardCharsets.UTF_8);
        } finally {
            buf.release();
        }
    }

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
        int result = Objects.hash(fileType, sizeType, dcId, photoId, accessHash, thumbSizeType, stickerSet, thumbVersion, messageId, peer);
        result = 31 * result + Arrays.hashCode(fileReference);
        return result;
    }

    public enum PhotoSizeType {
        UNKNOWN,
        CHAT_PHOTO_SMALL,
        CHAT_PHOTO_BIG;

        private static final PhotoSizeType[] ALL = values();
    }

    public enum Type {
        UNKNOWN,
        DOCUMENT,
        STICKER_SET_THUMB,
        PHOTO,
        CHAT_PHOTO;

        private static final Type[] ALL = values();
    }
}
