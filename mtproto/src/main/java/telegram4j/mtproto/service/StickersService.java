package telegram4j.mtproto.service;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.InputDocument;
import telegram4j.tl.InputStickerSet;
import telegram4j.tl.InputStickerSetItem;
import telegram4j.tl.messages.StickerSetWithDocuments;
import telegram4j.tl.request.stickers.*;
import telegram4j.tl.stickers.SuggestedShortName;

public class StickersService extends RpcService {

    public StickersService(MTProtoClient client, StoreLayout storeLayout) {
        super(client, storeLayout);
    }

    public Mono<StickerSetWithDocuments> createStickerSet(CreateStickerSet request) {
        return client.sendAwait(request);
    }

    public Mono<StickerSetWithDocuments> removeStickerFromSet(InputDocument sticker) {
        return client.sendAwait(ImmutableRemoveStickerFromSet.of(sticker));
    }

    public Mono<StickerSetWithDocuments> changeStickerPosition(InputDocument sticker, int position) {
        return client.sendAwait(ImmutableChangeStickerPosition.of(sticker, position));
    }

    public Mono<StickerSetWithDocuments> addStickerToSet(InputStickerSet stickerSet, InputStickerSetItem sticker) {
        return client.sendAwait(ImmutableAddStickerToSet.of(stickerSet, sticker));
    }

    public Mono<StickerSetWithDocuments> setStickerSetThumb(InputStickerSet stickerSet, InputDocument thumb) {
        return client.sendAwait(ImmutableSetStickerSetThumb.of(stickerSet, thumb));
    }

    public Mono<Boolean> checkShortName(String shortName) {
        return client.sendAwait(ImmutableCheckShortName.of(shortName));
    }

    public Mono<SuggestedShortName> suggestShortName(String title) {
        return client.sendAwait(ImmutableSuggestShortName.of(title));
    }
}
