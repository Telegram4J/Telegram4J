/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package telegram4j.mtproto.file;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import reactor.util.annotation.Nullable;
import telegram4j.tl.*;
import telegram4j.tl.api.TlEncodingUtil;
import telegram4j.tl.request.upload.GetFile;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

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
public final class FileReferenceId {

    static final String PREFIX = "x74JF1D0";

    static final int MAX_DC_ID = 0xffff;

    static final byte SIZE_TYPE_ABSENT = -1;
    static final byte SIZE_TYPE_SMALL = 1 << 2;
    static final byte SIZE_TYPE_BIG = 1 << 3;

    static final int ACCESS_HASH_MASK = 1;
    static final int THUMB_SIZE_TYPE_MASK = 1 << 1;
    static final int CONTEXT_TYPE_MASK = 1 << 4;

    private final Type fileType;
    @Nullable
    private final DocumentType documentType;
    private final byte sizeType;
    private final short dcId;
    private final long documentId;
    private final long accessHash;
    @Nullable
    private final ByteBuf fileReference;
    private final char thumbSizeType;
    @Nullable
    private final String url;
    private final InputStickerSet stickerSet;
    private final int thumbVersion;

    private final Context context;

    FileReferenceId(Type fileType, @Nullable DocumentType documentType, byte sizeType,
                    short dcId, long documentId, long accessHash,
                    @Nullable ByteBuf fileReference, char thumbSizeType,
                    @Nullable String url, @Nullable InputStickerSet stickerSet, int thumbVersion,
                    Context context) {
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
        this.context = Objects.requireNonNull(context);
    }

    private static char thumbTypeAsChar(String type) {
        char c;
        if (type.length() != 1 || (c = type.charAt(0)) > 0xff)
            throw new IllegalArgumentException("Unknown format of the photo size type: '" + type + "'");
        return c;
    }

    /**
     * Creates new {@code FileReferenceId} object from given web document and source context.
     *
     * @param document The document info.
     * @param context The context of document.
     * @return The new {@code FileReferenceId} from given web document and source context.
     */
    public static FileReferenceId ofDocument(WebDocument document, Context context) {
        long accessHash = document instanceof BaseWebDocument
                ? ((BaseWebDocument) document).accessHash()
                : -1;
        DocumentType documentType = DocumentType.fromAttributes(document.attributes());

        return new FileReferenceId(Type.WEB_DOCUMENT, documentType, SIZE_TYPE_ABSENT,
                (short) -1, -1, accessHash, null, '\0', document.url(),
                null, -1, context);
    }

    /**
     * Creates new {@code FileReferenceId} object from given document and source context,
     * with <b>first</b> video or static thumbnail.
     *
     * @param document The document info.
     * @param context The context of document.
     * @return The new {@code FileReferenceId} from given document and source context.
     */
    public static FileReferenceId ofDocument(BaseDocument document, Context context) {
        char thumbSizeType = Optional.ofNullable(document.videoThumbs())
                .flatMap(thumbs -> thumbs.stream()
                        .flatMap(d -> d instanceof BaseVideoSize b
                                ? Stream.of(thumbTypeAsChar(b.type()))
                                : Stream.empty())
                        .findFirst())
                .or(() -> Optional.ofNullable(document.thumbs())
                        .map(d -> thumbTypeAsChar(d.get(0).type())))
                .orElse('\0');

        return ofDocument(document, thumbSizeType, context);
    }

    /**
     * Creates new {@code FileReferenceId} object from given document and source context.
     *
     * @param document The document info.
     * @param thumbSizeType The type of thumbnail used for downloading.
     * @param context The context of document.
     * @return The new {@code FileReferenceId} from given document and source context.
     */
    public static FileReferenceId ofDocument(BaseDocument document, char thumbSizeType, Context context) {
        if (document.dcId() > MAX_DC_ID)
            throw new IllegalArgumentException("Unexpected dcId: " + document.dcId() + " > " + MAX_DC_ID);

        DocumentType documentType = DocumentType.fromAttributes(document.attributes());
        ByteBuf fileReference = TlEncodingUtil.copyAsUnpooled(document.fileReference());
        return new FileReferenceId(Type.DOCUMENT, documentType, SIZE_TYPE_ABSENT,
                (short) document.dcId(), document.id(), document.accessHash(), fileReference, thumbSizeType, null,
                null, -1, context);
    }

    /**
     * Creates new {@code FileReferenceId} object from given <b>normal</b> photo and source context,
     * with <b>first</b> video or static thumbnail.
     *
     * @throws IllegalArgumentException If peer id is {@link InputPeerEmpty}.
     * @param chatPhoto The chat photo info.
     * @param context The context of profile photo.
     * @return The new {@code FileReferenceId} from given <b>normal</b> photo and source context.
     */
    public static FileReferenceId ofChatPhoto(BasePhoto chatPhoto, ProfilePhotoContext context) {
        char thumbSizeType = Optional.ofNullable(chatPhoto.videoSizes())
                .flatMap(thumbs -> thumbs.stream()
                        .flatMap(d -> d instanceof BaseVideoSize b
                                ? Stream.of(thumbTypeAsChar(b.type()))
                                : Stream.empty())
                        .findFirst())
                .orElseGet(() -> thumbTypeAsChar(chatPhoto.sizes().get(0).type()));

        return ofChatPhoto(chatPhoto, thumbSizeType, context);
    }

    /**
     * Creates new {@code FileReferenceId} object from given <b>normal</b> photo and source context.
     *
     * @throws IllegalArgumentException If peer id is {@link InputPeerEmpty}.
     * @param chatPhoto The chat photo info.
     * @param thumbSizeType The type of thumbnail used for downloading.
     * @param context The context of profile photo.
     * @return The new {@code FileReferenceId} from given <b>normal</b> photo and source context.
     */
    public static FileReferenceId ofChatPhoto(BasePhoto chatPhoto, char thumbSizeType, ProfilePhotoContext context) {
        if (chatPhoto.dcId() > MAX_DC_ID)
            throw new IllegalArgumentException("Unexpected dcId: " + chatPhoto.dcId() + " > " + MAX_DC_ID);

        ByteBuf fileReference = TlEncodingUtil.copyAsUnpooled(chatPhoto.fileReference());
        return new FileReferenceId(Type.CHAT_PHOTO, null, SIZE_TYPE_ABSENT,
                (short) chatPhoto.dcId(), chatPhoto.id(), chatPhoto.accessHash(),
                fileReference, thumbSizeType, null, null, -1, context);
    }

    /**
     * Creates new {@code FileReferenceId} object from given minimal chat photo and source context.
     *
     * @throws IllegalArgumentException If peer id is {@link InputPeerEmpty}.
     * @param chatPhoto The chat photo info.
     * @param big When chat photo is big.
     * @param peer The peer that's have this photo.
     * @return The new {@code FileReferenceId} from given minimal chat photo and source context.
     */
    public static FileReferenceId ofChatPhoto(BaseChatPhoto chatPhoto, boolean big, InputPeer peer) {
        if (chatPhoto.dcId() > MAX_DC_ID)
            throw new IllegalArgumentException("Unexpected dcId: " + chatPhoto.dcId() + " > " + MAX_DC_ID);

        byte sizeType = big ? SIZE_TYPE_BIG : SIZE_TYPE_SMALL;
        var context = new ProfilePhotoContext(peer);
        return new FileReferenceId(Type.CHAT_PHOTO, null, sizeType,
                (short) chatPhoto.dcId(), chatPhoto.photoId(), -1,
                null, '\0', null,
                null, -1, context);
    }

    public static FileReferenceId ofChatPhoto(BaseUserProfilePhoto chatPhoto, boolean big, InputPeer peer) {
        if (chatPhoto.dcId() > MAX_DC_ID)
            throw new IllegalArgumentException("Unexpected dcId: " + chatPhoto.dcId() + " > " + MAX_DC_ID);

        byte sizeType = big ? SIZE_TYPE_BIG : SIZE_TYPE_SMALL;
        var context = new ProfilePhotoContext(peer);
        return new FileReferenceId(Type.CHAT_PHOTO, null, sizeType,
                (short) chatPhoto.dcId(), chatPhoto.photoId(), -1,
                null, '\0', null,
                null, -1, context);
    }

    /**
     * Creates new {@code FileReferenceId} object from given <b>message</b> photo and source context,
     * with <b>first</b> video or static thumbnail.
     *
     * @param photo The photo object.
     * @param context The context of message photo.
     * @return The new {@code FileReferenceId} from given photo and source context.
     */
    public static FileReferenceId ofPhoto(BasePhoto photo, Context context) {
        char thumbSizeType = Optional.ofNullable(photo.videoSizes())
                .flatMap(thumbs -> thumbs.stream()
                        .flatMap(d -> d instanceof BaseVideoSize b
                                ? Stream.of(thumbTypeAsChar(b.type()))
                                : Stream.empty())
                        .findFirst())
                .orElseGet(() -> thumbTypeAsChar(photo.sizes().get(0).type()));

        return ofPhoto(photo, thumbSizeType, context);
    }

    /**
     * Creates new {@code FileReferenceId} object from given <b>message</b> photo and source context.
     *
     * @param photo The photo object.
     * @param thumbSizeType The type of thumbnail used for downloading.
     * @param context The context of message photo.
     * @return The new {@code FileReferenceId} from given photo and source context.
     */
    public static FileReferenceId ofPhoto(BasePhoto photo, char thumbSizeType, Context context) {
        if (photo.dcId() > MAX_DC_ID)
            throw new IllegalArgumentException("Unexpected dcId: " + photo.dcId() + " > " + MAX_DC_ID);

        ByteBuf fileReference = TlEncodingUtil.copyAsUnpooled(photo.fileReference());
        return new FileReferenceId(Type.PHOTO, null,
                SIZE_TYPE_ABSENT, (short) photo.dcId(), photo.id(), photo.accessHash(),
                fileReference, thumbSizeType,
                null, null, -1, context);
    }

    /**
     * Creates new {@code FileReferenceId} object with sticker set thumbnail from given their id.
     *
     * @throws IllegalArgumentException If sticker set id is {@link InputStickerSetEmpty} or {@code version} is negative.
     * @param stickerSet The sticker set identifier.
     * @param version The id of sticker set thumbnail.
     * @param dcId The id of media dc.
     * @return The new {@code FileReferenceId} with sticker set thumbnail from their id.
     */
    public static FileReferenceId ofStickerSet(InputStickerSet stickerSet, int version, int dcId) {
        if (stickerSet == InputStickerSetEmpty.instance())
            throw new IllegalArgumentException("Unexpected stickerSet type.");
        if (version < 0)
            throw new IllegalArgumentException("Invalid sticker set thumbnail version.");
        if (dcId > MAX_DC_ID)
            throw new IllegalArgumentException("Unexpected dcId: " + dcId + " > " + MAX_DC_ID);

        return new FileReferenceId(Type.STICKER_SET_THUMB, null,
                SIZE_TYPE_ABSENT, (short) dcId, -1, -1,
                null, '\0', null, stickerSet,
                version, Context.noOpContext());
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
        if (!str.startsWith(PREFIX))
            throw new IllegalArgumentException("Incorrect file reference id format: '" + str + "'");

        ByteBuf buf = Unpooled.wrappedBuffer(Base64.getUrlDecoder().decode(
                ByteBuffer.wrap(str.getBytes(StandardCharsets.US_ASCII))
                        .position(PREFIX.length())));

        short rev = buf.readUnsignedByte();
        Version ver = Version.of(rev);
        return ver.handler.deserialize(buf, str);
    }

    /**
     * Serializes reference to base 64 url string,
     * which can be deserialized via {@link #deserialize(String)}.
     *
     * @return The serialized base 64 url identifier string of file reference.
     */
    public String serialize() {
        ByteBuf buf = Unpooled.buffer(); // TODO: presize

        buf.writeByte(Version.CURRENT.rev);
        serialize(buf);

        return PREFIX + new String(Base64.getUrlEncoder()
                .encode(buf.nioBuffer())
                .array(), StandardCharsets.US_ASCII);
    }

    private void serialize(ByteBuf buf) {
        buf.writeByte((byte) fileType.ordinal());

        byte flags = 0;
        if (context.getType() != Context.Type.UNKNOWN) {
            flags |= CONTEXT_TYPE_MASK;
        }
        if (fileType == Type.WEB_DOCUMENT && accessHash != -1) {
            flags |= ACCESS_HASH_MASK;
        }
        if (sizeType != SIZE_TYPE_ABSENT) {
            flags |= sizeType;
        }
        if (thumbSizeType != '\0') {
            flags |= THUMB_SIZE_TYPE_MASK;
        }

        buf.writeByte(flags);

        if ((flags & CONTEXT_TYPE_MASK) != 0) {
            buf.writeByte(context.getType().ordinal());
        }

        switch (fileType) {
            case WEB_DOCUMENT:
                Objects.requireNonNull(documentType);
                buf.writeByte((byte) documentType.ordinal());

                Objects.requireNonNull(url);
                TlSerialUtil.serializeString(buf, url);

                if ((flags & ACCESS_HASH_MASK) != 0) {
                    buf.writeLongLE(accessHash);
                }
                break;
            case DOCUMENT:
                Objects.requireNonNull(documentType);
                buf.writeByte((byte) documentType.ordinal());

            case PHOTO: {
                buf.writeShortLE(dcId);
                buf.writeLongLE(documentId);
                buf.writeLongLE(accessHash);

                Objects.requireNonNull(fileReference);
                TlSerialUtil.serializeBytes(buf, fileReference);

                if ((flags & THUMB_SIZE_TYPE_MASK) != 0) {
                    buf.writeByte(thumbSizeType);
                }

                break;
            }
            case CHAT_PHOTO:
                buf.writeShortLE(dcId);
                buf.writeLongLE(documentId);

                if (sizeType == SIZE_TYPE_ABSENT) {
                    buf.writeLongLE(accessHash);

                    Objects.requireNonNull(fileReference);
                    TlSerialUtil.serializeBytes(buf, fileReference);

                    if ((flags & THUMB_SIZE_TYPE_MASK) != 0) {
                        buf.writeByte(thumbSizeType);
                    }
                }

                break;
            case STICKER_SET_THUMB:
                buf.writeShortLE(dcId);
                buf.writeIntLE(thumbVersion);

                Objects.requireNonNull(stickerSet);
                TlSerializer.serialize(buf, stickerSet);
                break;
            default:
                throw new IllegalStateException();
        }

        if (context.getType() != Context.Type.UNKNOWN) {
            context.serialize(buf);
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
    public short getDcId() {
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
     * Gets the thumbnail transformation type, if file has it.
     * @apiNote {@link GetFile} will return a thumb at the first instead
     * of the file itself if file has it and has {@link Type#DOCUMENT} type. To download only file content
     * you should update this id via {@code fileRefId.withThumbSizeType('\0')} before downloading.
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
     * Gets {@link DocumentType} of web or tg file, if file is document.
     *
     * @return The {@link DocumentType} of file, if applicable
     */
    public Optional<DocumentType> getDocumentType() {
        return Optional.ofNullable(documentType);
    }

    /**
     * Gets original context where document was found.
     *
     * @return The original context where document was found.
     */
    public Context getContext() {
        return context;
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
     * which used in {@link GetFile} method.
     * <p> Web files would be ignored, use {@link #asWebLocation()}.
     *
     * @return The new {@link InputFileLocation} from this reference.
     */
    public Optional<InputFileLocation> asLocation() {
        return switch (fileType) {
            case CHAT_PHOTO -> {
                if (sizeType == SIZE_TYPE_ABSENT) { // is full image
                    Objects.requireNonNull(fileReference);
                    yield Optional.of(InputPhotoFileLocation.builder()
                            .accessHash(accessHash)
                            .fileReference(fileReference)
                            .id(documentId)
                            .thumbSize(String.valueOf(thumbSizeType))
                            .build());
                }
                var ctx = (ProfilePhotoContext) context;
                yield Optional.of(InputPeerPhotoFileLocation.builder()
                        .peer(ctx.getPeer())
                        .photoId(documentId)
                        .big(sizeType == SIZE_TYPE_BIG)
                        .build());
            }
            case PHOTO -> {
                Objects.requireNonNull(fileReference);
                yield Optional.of(InputPhotoFileLocation.builder()
                        .accessHash(accessHash)
                        .fileReference(fileReference)
                        .id(documentId)
                        .thumbSize(String.valueOf(thumbSizeType))
                        .build());
            }
            case STICKER_SET_THUMB -> {
                Objects.requireNonNull(stickerSet);
                yield Optional.of(InputStickerSetThumb.builder()
                        .stickerset(stickerSet)
                        .thumbVersion(thumbVersion)
                        .build());
            }
            case DOCUMENT -> {
                Objects.requireNonNull(fileReference);
                yield Optional.of(InputDocumentFileLocation.builder()
                        .accessHash(accessHash)
                        .fileReference(fileReference)
                        .id(documentId)
                        .thumbSize(String.valueOf(thumbSizeType))
                        .build());
            }
            default -> Optional.empty();
        };
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
            throw new IllegalStateException("Cant create input photo from file ref id: " + this);
        }

        Objects.requireNonNull(fileReference);
        return ImmutableBaseInputPhoto.of(documentId, accessHash, fileReference);
    }

    /**
     * Creates a new {@code FileReferenceId} with specified {@code thumbSizeType} or
     * if it equals to current returns this object.
     *
     * @see <a href="https://core.telegram.org/api/files#image-thumbnail-types">Thumbnail Types</a>
     * @param thumbSizeType The new type of thumbnail.
     * @return A new {@code FileReferenceId} with specified thumb type or current object
     * if it is not changed.
     */
    public FileReferenceId withThumbSizeType(char thumbSizeType) {
        if (fileType != Type.DOCUMENT && fileType != Type.PHOTO && (fileType != Type.CHAT_PHOTO || sizeType != SIZE_TYPE_ABSENT)) {
            throw new IllegalStateException("Thumb size type can't be set for file ref id: " + this);
        }
        if (thumbSizeType == this.thumbSizeType) return this;
        return new FileReferenceId(fileType, documentType, sizeType, dcId, documentId, accessHash,
                fileReference, thumbSizeType, url, stickerSet, thumbVersion, context);
    }

    /**
     * Creates a new {@code FileReferenceId} with specified {@code context} or
     * if it equals to current returns this object.
     *
     * @param context The new context for this file ref id.
     * @return A new {@code FileReferenceId} with specified context or current object
     * if it is not changed.
     */
    public FileReferenceId withContext(Context context) {
        if (this.context.equals(context)) return this;
        return new FileReferenceId(fileType, documentType, sizeType,
                dcId, documentId, accessHash, fileReference,
                thumbSizeType, url, stickerSet, thumbVersion, context);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileReferenceId that = (FileReferenceId) o;
        return sizeType == that.sizeType && dcId == that.dcId &&
                documentId == that.documentId && accessHash == that.accessHash &&
                thumbSizeType == that.thumbSizeType && thumbVersion == that.thumbVersion &&
                fileType == that.fileType &&
                documentType == that.documentType &&
                Objects.equals(fileReference, that.fileReference) &&
                Objects.equals(url, that.url) &&
                Objects.equals(stickerSet, that.stickerSet) &&
                context.equals(that.context);
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
        h += (h << 5) + context.hashCode();
        return h;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("FileReferenceId{");
        builder.append("fileType=").append(fileType);

        switch (fileType) {
            case WEB_DOCUMENT -> {
                builder.append(", ").append("documentType=").append(documentType);
                builder.append(", ").append("url='").append(url).append('\'');
                if (accessHash != -1) {
                    builder.append(", ").append("accessHash=").append(accessHash);
                }
            }
            case DOCUMENT -> {
                builder.append(", ").append("dcId=").append(Short.toUnsignedInt(dcId));
                builder.append(", ").append("documentId=").append(documentId);
                builder.append(", ").append("accessHash=").append(accessHash);
                builder.append(", ").append("documentType=").append(documentType);
                builder.append(", ").append("thumbSizeType=").append(thumbSizeType);
                Objects.requireNonNull(fileReference);
                builder.append(", ").append("fileReference='").append(ByteBufUtil.hexDump(fileReference)).append('\'');
            }
            case STICKER_SET_THUMB -> {
                builder.append(", ").append("stickerSet=").append(stickerSet);
                builder.append(", ").append("thumbVersion=").append(thumbVersion);
                builder.append(", ").append("dcId=").append(Short.toUnsignedInt(dcId));
            }
            case CHAT_PHOTO -> {
                builder.append(", ").append("dcId=").append(Short.toUnsignedInt(dcId));
                builder.append(", ").append("documentId=").append(documentId);
                if (sizeType == SIZE_TYPE_ABSENT) {
                    builder.append(", ").append("accessHash=").append(accessHash);
                    Objects.requireNonNull(fileReference);
                    builder.append(", ").append("fileReference='").append(ByteBufUtil.hexDump(fileReference)).append('\'');
                    builder.append(", ").append("thumbSizeType=").append(thumbSizeType);
                } else {
                    builder.append(", ").append("big=").append(sizeType == SIZE_TYPE_BIG);
                }
            }
            case PHOTO -> {
                builder.append(", ").append("dcId=").append(Short.toUnsignedInt(dcId));
                builder.append(", ").append("documentId=").append(documentId);
                builder.append(", ").append("accessHash=").append(accessHash);
                Objects.requireNonNull(fileReference);
                builder.append(", ").append("fileReference='").append(ByteBufUtil.hexDump(fileReference)).append('\'');
                builder.append(", ").append("thumbSizeType=").append(thumbSizeType);
            }
        }

        if (context != Context.noOpContext())
            builder.append(", context=").append(context);
        builder.append('}');

        return builder.toString();
    }

    /** Types of web or tg documents. */
    public enum DocumentType {

        /** Default type for all other documents. */
        GENERAL,

        /**
         * Represents photo sent as document.
         * This documents will always have a {@link DocumentAttributeImageSize} attribute.
         */
        PHOTO,

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
            boolean haveSizeAttr = false;
            for (DocumentAttribute attribute : attributes) {
                switch (attribute.identifier()) {
                    case DocumentAttributeAnimated.ID -> {
                        return GIF;
                    }
                    case DocumentAttributeAudio.ID -> {
                        DocumentAttributeAudio d = (DocumentAttributeAudio) attribute;
                        return d.voice() ? VOICE : AUDIO;
                    }
                    case DocumentAttributeCustomEmoji.ID -> {
                        return EMOJI;
                    }
                    case DocumentAttributeSticker.ID -> {
                        return STICKER;
                    }
                    case DocumentAttributeVideo.ID -> type = VIDEO;
                    case DocumentAttributeImageSize.ID -> haveSizeAttr = true;
                }
            }
            return haveSizeAttr ? PHOTO : type;
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

    public enum Version {
        REVISION_0(new Revision0Deserializer()),
        CURRENT(REVISION_0);

        final RevisionDeserializer handler;
        final byte rev;

        Version(Version other) {
            this.handler = other.handler;
            this.rev = other.rev;
        }

        Version(RevisionDeserializer handler) {
            this.handler = handler;
            this.rev = (byte) ordinal();
        }

        public short getRevision() {
            return (short) (rev & 0xFF);
        }

        public static Version of(short version) {
            return switch (version) {
                case 0 -> REVISION_0;
                default -> throw new IllegalArgumentException("Unknown version: " + version);
            };
        }
    }

    static abstract class RevisionDeserializer {

        abstract FileReferenceId deserialize(ByteBuf buf, String str);
    }

    static class Revision0Deserializer extends RevisionDeserializer {

        @Override
        FileReferenceId deserialize(ByteBuf buf, String str) {
            Type fileType = Type.ALL[buf.readByte()];
            byte flags = buf.readByte();
            var contextType = (flags & CONTEXT_TYPE_MASK) != 0
                    ? Context.Type.ALL[buf.readByte()] : Context.Type.UNKNOWN;

            byte sizeType = (flags & SIZE_TYPE_BIG) != 0 ? SIZE_TYPE_BIG :
                    (flags & SIZE_TYPE_SMALL) != 0 ? SIZE_TYPE_SMALL : SIZE_TYPE_ABSENT;

            String url = null;
            long accessHash = -1;
            short dcId = -1;
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
                    dcId = buf.readShortLE();
                    documentId = buf.readLongLE();
                    accessHash = buf.readLongLE();

                    fileReference = TlEncodingUtil.copyAsUnpooled(TlSerialUtil.deserializeBytes(buf));
                    if ((flags & THUMB_SIZE_TYPE_MASK) != 0) {
                        thumbSizeType = (char) buf.readByte();
                    }

                    break;
                case CHAT_PHOTO:
                    dcId = buf.readShortLE();
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
                    dcId = buf.readShortLE();
                    thumbVersion = buf.readIntLE();
                    stickerSet = TlDeserializer.deserialize(buf);

                    break;
            }

            Context ctx = contextType != Context.Type.UNKNOWN
                    ? Context.deserialize(buf, contextType)
                    : Context.noOpContext();

            int remain = buf.readableBytes();
            if (remain != 0) {
                buf.release();
                throw new IllegalArgumentException("Malformed file reference id: '" + str + "', remaining bytes: " + remain);
            }

            buf.release();
            return new FileReferenceId(fileType, documentType, sizeType, dcId, documentId, accessHash,
                    fileReference, thumbSizeType, url, stickerSet, thumbVersion, ctx);
        }
    }
}
