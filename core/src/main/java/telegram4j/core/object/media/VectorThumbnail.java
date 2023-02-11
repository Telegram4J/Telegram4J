package telegram4j.core.object.media;

import io.netty.buffer.ByteBuf;

import java.util.Objects;

public final class VectorThumbnail implements Thumbnail {

    private final telegram4j.tl.PhotoPathSize data;

    public VectorThumbnail(telegram4j.tl.PhotoPathSize data) {
        this.data = Objects.requireNonNull(data);
    }

    /**
     * Gets a single-char <a href="https://core.telegram.org/api/files#image-thumbnail-types">type</a> of thumbnail.
     *
     * @return The type of thumbnail, always is {@code 'j'}.
     */
    @Override
    public char getType() {
        return data.type().charAt(0);
    }

    public ByteBuf getContent() {
        return data.bytes();
    }

    @Override
    public String toString() {
        return "PhotoPathSize{" +
                "data=" + data +
                '}';
    }
}
