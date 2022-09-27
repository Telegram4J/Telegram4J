package telegram4j.mtproto.file;

import io.netty.buffer.ByteBuf;
import reactor.util.annotation.Nullable;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.storage.FileType;
import telegram4j.tl.upload.BaseFile;
import telegram4j.tl.upload.WebFile;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Object with part or full of file. */
public final class FilePart {
    private final FileType type;
    private final int mtime;
    private final ByteBuf bytes;
    private final int size;
    @Nullable
    private final String mimeType;

    /**
     * Constructs {@code FilePart} from raw parameters.
     *
     * @param type The telegram's file type for file. For parsing it from mime-type you can use {@link TlEntityUtil#suggestFileType(String)}.
     * @param mtime The latest modification timestamp, if present, otherwise {@code -1}.
     * @param bytes The file part content.
     * @param size The file size in bytes, if present, otherwise {@code -1}.
     * @param mimeType The mime-type in a string, if present.
     */
    public FilePart(FileType type, int mtime, ByteBuf bytes, int size, @Nullable String mimeType) {
        this.type = Objects.requireNonNull(type);
        this.mtime = mtime;
        this.bytes = Objects.requireNonNull(bytes);
        this.size = size;
        this.mimeType = mimeType;
    }

    /**
     * Constructs {@code FilePart} from specified web file.
     *
     * @param webFile The web file for constructing.
     * @return The new {@code FilePart}.
     */
    public static FilePart ofWebFile(WebFile webFile) {
        return new FilePart(webFile.fileType(), webFile.mtime(),
                webFile.bytes(), webFile.size(), webFile.mimeType());
    }

    /**
     * Constructs {@code FilePart} from specified telegram file.
     *
     * @param baseFile The telegram file for constructing.
     * @return The new {@code FilePart}.
     */
    public static FilePart ofFile(BaseFile baseFile) {
        return new FilePart(baseFile.type(), baseFile.mtime(), baseFile.bytes(), -1, null);
    }

    /**
     * Gets file type of this file part.
     *
     * @return The {@link FileType} of this file part.
     */
    public FileType getType() {
        return type;
    }

    /**
     * Gets the latest modification timestamp for telegram file, if present.
     *
     * @return The {@link Instant} with the latest modification timestamp for telegram file, if present.
     */
    public Optional<Instant> getModificationTimestamp() {
        return mtime != -1 ? Optional.of(Instant.ofEpochSecond(mtime)) : Optional.empty();
    }

    /**
     * Gets <b>immutable</b> {@link ByteBuf} with file part content.
     *
     * @return The {@link ByteBuf} with file part content.
     */
    public ByteBuf getBytes() {
        return bytes.duplicate();
    }

    /**
     * Gets size of the file in bytes, if present.
     *
     * @return The size of the file in bytes, if present.
     */
    public Optional<Integer> getSize() {
        return size != -1 ? Optional.of(size) : Optional.empty();
    }

    /**
     * Gets string mime-type of the file, if present.
     *
     * @return The string mime-type of the file, if present.
     */
    public Optional<String> getMimeType() {
        return Optional.ofNullable(mimeType);
    }
}
