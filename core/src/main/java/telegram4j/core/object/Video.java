package telegram4j.core.object;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.BaseDocument;
import telegram4j.tl.DocumentAttributeVideo;
import telegram4j.tl.InputPeer;

import java.time.Duration;
import java.util.Objects;

public class Video extends Document {

    private final telegram4j.tl.DocumentAttributeVideo videoData;
    private final boolean hasStickers;
    private final boolean gif;

    public Video(MTProtoTelegramClient client, BaseDocument data,
                 String fileName, int messageId, InputPeer peer,
                 DocumentAttributeVideo videoData, boolean hasStickers, boolean gif) {
        super(client, data, fileName, messageId, peer);
        this.videoData = Objects.requireNonNull(videoData, "videoData");
        this.hasStickers = hasStickers;
        this.gif = gif;
    }

    public boolean isHasStickers() {
        return hasStickers;
    }

    public boolean isGif() {
        return gif;
    }

    public boolean isRoundMessage() {
        return videoData.roundMessage();
    }

    public boolean isSupportsStreaming() {
        return videoData.supportsStreaming();
    }

    public Duration getDuration() {
        return Duration.ofSeconds(videoData.duration());
    }

    public int getWight() {
        return videoData.w();
    }

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
