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
    private final String thumbSize;

    FileReferenceId(Type fileType, PhotoSizeType sizeType, int dcId, long photoId, long accessHash,
                    byte[] fileReference, InputPeer peerId, String thumbSize) {
        this.fileType = Objects.requireNonNull(fileType, "fileType");
        this.sizeType = Objects.requireNonNull(sizeType, "sizeType");
        this.dcId = dcId;
        this.photoId = photoId;
        this.accessHash = accessHash;
        this.fileReference = Objects.requireNonNull(fileReference, "fileReference");
        this.peerId = Objects.requireNonNull(peerId, "peerId");
        this.thumbSize = Objects.requireNonNull(thumbSize, "thumbSize");
    }

    public static FileReferenceId ofDocument(BaseDocument document) {
        var thumbs = document.thumbs();
        String firstThumbSize = thumbs != null ? thumbs.get(0).type() : "";

        return new FileReferenceId(Type.DOCUMENT, PhotoSizeType.UNKNOWN, document.dcId(), document.id(), document.accessHash(),
                document.fileReference(), InputPeerEmpty.instance(), firstThumbSize);
    }

    public static FileReferenceId ofChatPhoto(InputPeer peer, ChatPhotoFields chatPhoto, PhotoSizeType sizeType) {
        if (sizeType != PhotoSizeType.CHAT_PHOTO_BIG && sizeType != PhotoSizeType.CHAT_PHOTO_SMALL) {
            throw new IllegalArgumentException("Unexpected size type for chat photo: " + sizeType);
        }

        return new FileReferenceId(Type.CHAT_PHOTO, sizeType, chatPhoto.dcId(), chatPhoto.photoId(), -1,
                EmptyArrays.EMPTY_BYTES, peer, "");
    }

    public static FileReferenceId ofPhoto(BasePhoto photo) {
        var firstPhotoSize = photo.sizes().get(0);

        return new FileReferenceId(Type.PHOTO, PhotoSizeType.UNKNOWN, photo.dcId(), photo.id(), photo.accessHash(),
                photo.fileReference(), InputPeerEmpty.instance(), firstPhotoSize.type());
    }

    public static FileReferenceId deserialize(String str) {
        ByteBuf data = Unpooled.wrappedBuffer(str.getBytes(StandardCharsets.UTF_8));
        ByteBuf buf = Base64.decode(data, Base64Dialect.URL_SAFE);
        data.release();

        Type fileType = Type.ALL[buf.readIntLE()];
        PhotoSizeType sizeType = PhotoSizeType.ALL[buf.readIntLE()];
        int dcId = buf.readIntLE();
        long photoId = buf.readLongLE();
        long accessHash = buf.readLongLE();

        byte[] fileReference = TlSerialUtil.deserializeBytes(buf);
        InputPeer peerId = TlDeserializer.deserialize(buf);
        String thumbSize = TlSerialUtil.deserializeString(buf);
        buf.release();

        return new FileReferenceId(fileType, sizeType, dcId, photoId, accessHash, fileReference, peerId, thumbSize);
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

    public byte[] getFileReference() {
        return fileReference;
    }

    public InputPeer getPeerId() {
        return peerId;
    }

    public String getThumbSize() {
        return thumbSize;
    }

    public String serialize(ByteBufAllocator alloc) {
        ByteBuf fileReferenceBuf = TlSerialUtil.serializeBytes(alloc, fileReference);
        ByteBuf peerIdBuf = TlSerializer.serialize(alloc, peerId);
        ByteBuf thumbSizeBuf = TlSerialUtil.serializeString(alloc, thumbSize);

        ByteBuf buf = alloc.buffer(fileReferenceBuf.readableBytes() +
                peerIdBuf.readableBytes() + thumbSizeBuf.readableBytes() +
                4 * 3 + 8 + 2);

        buf.writeIntLE(fileType.ordinal());
        buf.writeIntLE(sizeType.ordinal());
        buf.writeIntLE(dcId);
        buf.writeLongLE(photoId);
        buf.writeLongLE(accessHash);

        buf.writeBytes(fileReferenceBuf);
        fileReferenceBuf.release();
        buf.writeBytes(peerIdBuf);
        peerIdBuf.release();
        buf.writeBytes(thumbSizeBuf);
        thumbSizeBuf.release();

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
                        .thumbSize(thumbSize)
                        .build();
            case DOCUMENT:
                return InputDocumentFileLocation.builder()
                        .accessHash(accessHash)
                        .fileReference(fileReference)
                        .id(photoId)
                        .thumbSize(thumbSize)
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
        return dcId == that.dcId && photoId == that.photoId && accessHash == that.accessHash
                && fileType == that.fileType && sizeType == that.sizeType
                && Arrays.equals(fileReference, that.fileReference)
                && peerId.equals(that.peerId) && thumbSize.equals(that.thumbSize);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(fileType, sizeType, dcId, photoId, accessHash, peerId, thumbSize);
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
        PHOTO,
        CHAT_PHOTO;

        private static final Type[] ALL = values();
    }
}
