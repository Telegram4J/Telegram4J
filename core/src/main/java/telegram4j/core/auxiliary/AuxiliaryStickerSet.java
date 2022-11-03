package telegram4j.core.auxiliary;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Sticker;
import telegram4j.core.object.StickerSet;
import telegram4j.tl.StickerPack;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class AuxiliaryStickerSet {
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
