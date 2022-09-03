package telegram4j.core.object;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.BaseDocumentFields;
import telegram4j.tl.DocumentAttributeVideo;
import telegram4j.tl.InputPeer;

import java.time.Duration;
import java.util.Objects;

/** Inferred from {@link BaseDocumentFields#attributes()} type of video and gif documents. */
public class Video extends Document {

    private final telegram4j.tl.DocumentAttributeVideo videoData;
    private final boolean hasStickers;
    private final boolean gif;

    public Video(MTProtoTelegramClient client, BaseDocumentFields data,
                 String fileName, int messageId, InputPeer peer,
                 DocumentAttributeVideo videoData, boolean hasStickers, boolean gif) {
        super(client, data, fileName, messageId, peer);
        this.videoData = Objects.requireNonNull(videoData);
        this.hasStickers = hasStickers;
        this.gif = gif;
    }

    /**
     * Gets whether video document contains stickers.
     *
     * @return Whether video document contains stickers.
     */
    public boolean isHasStickers() {
        return hasStickers;
    }

    /**
     * Gets whether video document is gif.
     *
     * @return Whether video document is gif.
     */
    public boolean isGif() {
        return gif;
    }

    /**
     * Gets whether video document is round video.
     *
     * @return Whether video document is round video.
     */
    public boolean isRoundMessage() {
        return videoData.roundMessage();
    }

    /**
     * Gets whether video supports streaming.
     *
     * @return Whether video supports streaming.
     */
    public boolean isSupportsStreaming() {
        return videoData.supportsStreaming();
    }

    /**
     * Gets duration of video document.
     *
     * @return The duration of video document.
     */
    public Duration getDuration() {
        return Duration.ofSeconds(videoData.duration());
    }

    /**
     * Gets video document width.
     *
     * @return The video document width.
     */
    public int getWidth() {
        return videoData.w();
    }

    /**
     * Gets video document height.
     *
     * @return The video document height.
     */
    public int getHeight() {
        return videoData.h();
    }

    @Override
    public String toString() {
        return "Video{" +
                "videoData=" + videoData +
                ", hasStickers=" + hasStickers +
                ", gif=" + gif +
                "} " + super.toString();
    }
}
