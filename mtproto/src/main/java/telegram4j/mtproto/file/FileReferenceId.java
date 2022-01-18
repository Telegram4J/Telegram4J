package telegram4j.mtproto.file;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
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

    private final Type fileType;
    private final PhotoSizeType sizeType;
    private final int dcId;
    private final long photoId;
    private final long accessHash;
    private final byte[] fileReference;
    private final InputPeer peerId;
    private final String thumbSizeType;
    private final InputStickerSet stickerSet;
    private final int thumbVersion;

    FileReferenceId(Type fileType, PhotoSizeType sizeType, int dcId, long photoId, long accessHash,
                    byte[] fileReference, InputPeer peerId, String thumbSizeType,
                    InputStickerSet stickerSet, int thumbVersion) {
        this.fileType = Objects.requireNonNull(fileType, "fileType");
        this.sizeType = Objects.requireNonNull(sizeType, "sizeType");
        this.dcId = dcId;
        this.photoId = photoId;
        this.accessHash = accessHash;
        this.fileReference = Objects.requireNonNull(fileReference, "fileReference");
        this.peerId = Objects.requireNonNull(peerId, "peerId");
        this.thumbSizeType = Objects.requireNonNull(thumbSizeType, "thumbSize");
        this.stickerSet = stickerSet;
        this.thumbVersion = thumbVersion;
    }

    public static FileReferenceId ofDocument(BaseDocument document) {
        var thumbs = document.thumbs();
        String firstThumbSize = thumbs != null ? thumbs.get(0).type() : "";

        return new FileReferenceId(Type.DOCUMENT, PhotoSizeType.UNKNOWN, document.dcId(), document.id(), document.accessHash(),
                document.fileReference(), InputPeerEmpty.instance(),
                firstThumbSize, InputStickerSetEmpty.instance(), -1);
    }

    public static FileReferenceId ofChatPhoto(InputPeer peer, ChatPhotoFields chatPhoto, PhotoSizeType sizeType) {
        if (sizeType != PhotoSizeType.CHAT_PHOTO_BIG && sizeType != PhotoSizeType.CHAT_PHOTO_SMALL) {
            throw new IllegalArgumentException("Unexpected size type for chat photo: " + sizeType);
        }

        return new FileReferenceId(Type.CHAT_PHOTO, sizeType, chatPhoto.dcId(), chatPhoto.photoId(), -1,
                EmptyArrays.EMPTY_BYTES, peer, "", InputStickerSetEmpty.instance(), -1);
    }

    public static FileReferenceId ofPhoto(BasePhoto photo) {
        var firstPhotoSize = photo.sizes().get(0);

        return new FileReferenceId(Type.PHOTO, PhotoSizeType.UNKNOWN, photo.dcId(), photo.id(), photo.accessHash(),
                photo.fileReference(), InputPeerEmpty.instance(),
                firstPhotoSize.type(), InputStickerSetEmpty.instance(), -1);
    }

    public static FileReferenceId ofStickerSet(InputStickerSet stickerSet, int version) {
        return new FileReferenceId(Type.STICKER_SET_THUMB, PhotoSizeType.UNKNOWN, -1, -1, -1,
                EmptyArrays.EMPTY_BYTES, InputPeerEmpty.instance(), "", stickerSet, version);
    }

    public static FileReferenceId deserialize(String str) {
        ByteBuf data = Unpooled.wrappedBuffer(str.getBytes(StandardCharsets.UTF_8));
        ByteBuf buf = Base64.decode(data, Base64Dialect.URL_SAFE);
        data.release();

        Type fileType = Type.ALL[buf.readIntLE()];
        switch (fileType) {
            case DOCUMENT:
            case PHOTO: {
                int dcId = buf.readIntLE();
                long photoId = buf.readLongLE();
                long accessHash = buf.readLongLE();

                byte[] fileReference = TlSerialUtil.deserializeBytes(buf);
                String thumbSizeType = TlSerialUtil.deserializeString(buf);

                return new FileReferenceId(fileType, PhotoSizeType.UNKNOWN, dcId, photoId, accessHash,
                        fileReference, InputPeerEmpty.instance(),
                        thumbSizeType, InputStickerSetEmpty.instance(), -1);
            }
            case CHAT_PHOTO:
                PhotoSizeType sizeType = PhotoSizeType.ALL[buf.readIntLE()];
                int dcId = buf.readIntLE();
                long photoId = buf.readLongLE();
                InputPeer peerId = TlDeserializer.deserialize(buf);

                return new FileReferenceId(fileType, sizeType, dcId, photoId, -1,
                        EmptyArrays.EMPTY_BYTES, peerId,
                        "", InputStickerSetEmpty.instance(), -1);
            case STICKER_SET_THUMB:
                int thumbVersion = buf.readIntLE();
                InputStickerSet stickerSet = TlDeserializer.deserialize(buf);

                return new FileReferenceId(fileType, PhotoSizeType.UNKNOWN, -1, -1, -1,
                        EmptyArrays.EMPTY_BYTES, InputPeerEmpty.instance(),
                        "", stickerSet, thumbVersion);
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

    // TODO: it can be changed and it's terrible
    public byte[] getFileReference() {
        return fileReference;
    }

    public InputPeer getPeerId() {
        return peerId;
    }

    public String getThumbSizeType() {
        return thumbSizeType;
    }

    public String serialize(ByteBufAllocator alloc) {
        ByteBuf buf = alloc.buffer();
        buf.writeIntLE(fileType.ordinal());

        switch (fileType) {
            case DOCUMENT:
            case PHOTO:
                ByteBuf fileReferenceBuf = TlSerialUtil.serializeBytes(alloc, fileReference);
                ByteBuf thumbSizeTypeBuf = TlSerialUtil.serializeString(alloc, thumbSizeType);

                buf.writeIntLE(dcId);
                buf.writeLongLE(photoId);
                buf.writeLongLE(accessHash);

                buf.writeBytes(fileReferenceBuf);
                fileReferenceBuf.release();
                buf.writeBytes(thumbSizeTypeBuf);
                thumbSizeTypeBuf.release();

                break;
            case CHAT_PHOTO:
                ByteBuf peerIdBuf = TlSerializer.serialize(alloc, peerId);

                buf.writeIntLE(sizeType.ordinal());
                buf.writeIntLE(dcId);
                buf.writeLongLE(photoId);
                buf.writeBytes(peerIdBuf);
                peerIdBuf.release();

                break;
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
                return InputPeerPhotoFileLocation.builder()
                        .peer(peerId)
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
        return dcId == that.dcId && photoId == that.photoId
                && accessHash == that.accessHash && thumbVersion == that.thumbVersion
                && fileType == that.fileType && sizeType == that.sizeType
                && Arrays.equals(fileReference, that.fileReference) && peerId.equals(that.peerId)
                && thumbSizeType.equals(that.thumbSizeType) && stickerSet.equals(that.stickerSet);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(fileType, sizeType, dcId, photoId,
                accessHash, peerId, thumbSizeType, stickerSet, thumbVersion);
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
