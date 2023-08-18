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
package telegram4j.core.auxiliary;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Sticker;
import telegram4j.core.object.StickerSet;
import telegram4j.tl.StickerPack;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class AuxiliaryStickerSet {

    private final MTProtoTelegramClient client;
    private final StickerSet stickerSet;
    private final Map<Long, Sticker> stickers;
    private final List<StickerPack> stickerPacks;

    public AuxiliaryStickerSet(MTProtoTelegramClient client, StickerSet stickerSet,
                               Map<Long, Sticker> stickers, List<StickerPack> stickerPacks) {
        this.client = Objects.requireNonNull(client);
        this.stickerSet = Objects.requireNonNull(stickerSet);
        this.stickers = Objects.requireNonNull(stickers);
        this.stickerPacks = Objects.requireNonNull(stickerPacks);
    }

    public MTProtoTelegramClient getClient() {
        return client;
    }

    public StickerSet getStickerSet() {
        return stickerSet;
    }

    public Map<Long, Sticker> getStickers() {
        return stickers;
    }

    public Optional<Sticker> getSticker(long id) {
        return Optional.ofNullable(stickers.get(id));
    }

    public List<StickerPack> getStickerPacks() {
        return stickerPacks;
    }

    @Override
    public String toString() {
        return "AuxiliaryStickerSet{" +
                "stickerSet=" + stickerSet +
                ", stickers=" + stickers +
                ", stickerPacks=" + stickerPacks +
                '}';
    }
}
