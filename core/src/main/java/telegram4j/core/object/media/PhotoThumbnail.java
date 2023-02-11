package telegram4j.core.object.media;

import java.util.Objects;

public final class PhotoThumbnail implements Thumbnail {

    private final telegram4j.tl.BasePhotoSize data;

    public PhotoThumbnail(telegram4j.tl.BasePhotoSize data) {
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

    /**
     * Gets size of thumbnail in bytes.
     *
     * @return The size of thumbnail in bytes.
     */
    public int getSize() {
        return data.size();
    }

    @Override
    public String toString() {
        return "DefaultPhotoSize{" +
                "data=" + data +
                '}';
    }
}
