package telegram4j.core.object.media;

import io.netty.buffer.ByteBuf;

import java.util.Objects;

public final class CachedThumbnail implements Thumbnail {

    private final telegram4j.tl.PhotoCachedSize data;

    public CachedThumbnail(telegram4j.tl.PhotoCachedSize data) {
        this.data = Objects.requireNonNull(data);
    }

    @Override
    public char getType() {
        return data.type().charAt(0);
    }

    /**
     * Gets width of thumbnail.
     *
     * @return The width of thumbnail.
     */
    public int getWidth() {
        return data.w();
    }

    /**
     * Gets height of thumbnail.
     *
     * @return The height of thumbnail.
     */
    public int getHeight() {
        return data.h();
    }

    public ByteBuf getContent() {
        return data.bytes();
    }

    @Override
    public String toString() {
        return "PhotoCachedSize{" +
                "data=" + data +
                '}';
    }
}
