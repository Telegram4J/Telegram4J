package telegram4j.mtproto.service;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.BotCompatible;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.InputDocument;
import telegram4j.tl.InputStickerSet;
import telegram4j.tl.InputStickerSetItem;
import telegram4j.tl.messages.StickerSet;
import telegram4j.tl.request.stickers.*;
import telegram4j.tl.stickers.SuggestedShortName;

public class StickersService extends RpcService {

    public StickersService(MTProtoClient client, StoreLayout storeLayout) {
        super(client, storeLayout);
    }

    @BotCompatible
    public Mono<StickerSet> createStickerSet(CreateStickerSet request) {
        return client.sendAwait(request);
    }

    @BotCompatible
    public Mono<StickerSet> removeStickerFromSet(InputDocument sticker) {
        return client.sendAwait(ImmutableRemoveStickerFromSet.of(sticker));
    }

    @BotCompatible
    public Mono<StickerSet> changeStickerPosition(InputDocument sticker, int position) {
        return client.sendAwait(ImmutableChangeStickerPosition.of(sticker, position));
    }

    @BotCompatible
    public Mono<StickerSet> addStickerToSet(InputStickerSet stickerSet, InputStickerSetItem sticker) {
        return client.sendAwait(ImmutableAddStickerToSet.of(stickerSet, sticker));
    }

    @BotCompatible
    public Mono<StickerSet> setStickerSetThumb(InputStickerSet stickerSet, InputDocument thumb) {
        return client.sendAwait(ImmutableSetStickerSetThumb.of(stickerSet, thumb));
    }

    public Mono<Boolean> checkShortName(String shortName) {
        return client.sendAwait(ImmutableCheckShortName.of(shortName));
    }

    public Mono<String> suggestShortName(String title) {
        return client.sendAwait(ImmutableSuggestShortName.of(title)).map(SuggestedShortName::shortName);
    }
}
