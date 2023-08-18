/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
