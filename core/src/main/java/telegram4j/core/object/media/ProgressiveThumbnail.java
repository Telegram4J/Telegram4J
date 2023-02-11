package telegram4j.core.object.media;

import java.util.List;
import java.util.Objects;

public final class ProgressiveThumbnail implements Thumbnail {

    private final telegram4j.tl.PhotoSizeProgressive data;

    public ProgressiveThumbnail(telegram4j.tl.PhotoSizeProgressive data) {
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

    public List<Integer> getSizes() {
        return data.sizes();
    }

    @Override
    public String toString() {
        return "PhotoSizeProgressive{" +
                "data=" + data +
                '}';
    }
}
