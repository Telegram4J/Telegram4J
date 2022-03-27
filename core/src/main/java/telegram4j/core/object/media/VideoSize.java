package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.TelegramObject;

import java.util.Objects;
import java.util.Optional;

/**
 * Representation for animated profile pictures in MPEG4 format.
 *
 * @see <a href="https://core.telegram.org/api/files#animated-profile-pictures">Animated Profile Pictures</a>
 */
public class VideoSize implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.VideoSize data;

    public VideoSize(MTProtoTelegramClient client, telegram4j.tl.VideoSize data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets single-char type of applied transformations to video.
     * Can be one of these chars:
     * <ul>
     *   <li>{@code u}: if it's a profile photo.</li>
     *   <li>{@code v}: if it's a trimmed and downscaled video previews.</li>
     * </ul>
     *
     * @return The single-char type of applied transformations.
     */
    public String getType() {
        return data.type();
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
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VideoSize videoSize = (VideoSize) o;
        return data.equals(videoSize.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "VideoSize{" +
                "data=" + data +
                '}';
    }
}
