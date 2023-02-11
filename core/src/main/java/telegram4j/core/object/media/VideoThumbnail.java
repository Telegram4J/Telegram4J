package telegram4j.core.object.media;

import java.util.Objects;
import java.util.Optional;

/**
 * Representation of animated thumbnail of animated profile pictures in MPEG4 format.
 *
 * @see <a href="https://core.telegram.org/api/files#animated-profile-pictures">Animated Profile Pictures</a>
 */
public final class VideoThumbnail implements AnimatedThumbnail {

    private final telegram4j.tl.BaseVideoSize data;

    public VideoThumbnail(telegram4j.tl.BaseVideoSize data) {
        this.data = Objects.requireNonNull(data);
    }

    /**
     * Gets single-char type of applied transformations to video.
     * Can be one of these chars:
     * <ul>
     *   <li>{@code 'u'}: if it's a animated profile photo.</li>
     *   <li>{@code 'v'}: if it's a trimmed and downscaled video previews.</li>
     * </ul>
     *
     * @return The single-char type of applied transformations.
     * @see <a href="https://core.telegram.org/api/files#video-types">Video Thumbnail Types</a>
     */
    public char getType() {
        return data.type().charAt(0);
    }

    /**
     * Gets width of video.
     *
     * @return The width of video.
     */
    public int getWidth() {
        return data.w();
    }

    /**
     * Gets height of video.
     *
     * @return The height of video.
     */
    public int getHeight() {
        return data.h();
    }

    /**
     * Gets video size in bytes.
     *
     * @return The video size in bytes.
     */
    public int getSize() {
        return data.size();
    }

    /**
     * Gets video timestamp (in seconds) that should be used as static preview, if present.
     *
     * @return The video timestamp (in seconds) that should be used as static preview, if present.
     */
    public Optional<Double> getVideoStartTimestamp() {
        return Optional.ofNullable(data.videoStartTs());
    }

    @Override
    public String toString() {
        return "VideoThumbnail{" +
                "data=" + data +
                '}';
    }
}
