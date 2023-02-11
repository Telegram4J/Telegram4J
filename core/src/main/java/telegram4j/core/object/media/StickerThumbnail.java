package telegram4j.core.object.media;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.TelegramObject;
import telegram4j.core.util.Variant2;
import telegram4j.tl.InputStickerSet;
import telegram4j.tl.VideoSizeEmojiMarkup;
import telegram4j.tl.VideoSizeStickerMarkup;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class StickerThumbnail implements AnimatedThumbnail, TelegramObject {

    private final MTProtoTelegramClient client;
    private final Variant2<VideoSizeEmojiMarkup, VideoSizeStickerMarkup> data;

    public StickerThumbnail(MTProtoTelegramClient client, VideoSizeEmojiMarkup data) {
        this.client = Objects.requireNonNull(client);
        this.data = Variant2.ofT1(data);
    }

    public StickerThumbnail(MTProtoTelegramClient client, VideoSizeStickerMarkup data) {
        this.client = Objects.requireNonNull(client);
        this.data = Variant2.ofT2(data);
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    public boolean isEmoji() {
        return data.isT1Present();
    }

    public Optional<InputStickerSet> getStickerSet() {
        return data.getT2().map(VideoSizeStickerMarkup::stickerset);
    }

    public long getStickerId() {
        return data.map(VideoSizeEmojiMarkup::emojiId, VideoSizeStickerMarkup::stickerId);
    }

    public List<Integer> getBackgroundColors() {
        return data.map(VideoSizeEmojiMarkup::backgroundColors, VideoSizeStickerMarkup::backgroundColors);
    }

    @Override
    public String toString() {
        return "StickerThumbnail{" +
                "data=" + data +
                '}';
    }
}
