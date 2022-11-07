package telegram4j.mtproto.service;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.MTProtoClientGroupManager;
import telegram4j.mtproto.service.Compatible.Type;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.InputDocument;
import telegram4j.tl.InputStickerSet;
import telegram4j.tl.InputStickerSetItem;
import telegram4j.tl.messages.StickerSet;
import telegram4j.tl.request.stickers.*;
import telegram4j.tl.stickers.SuggestedShortName;

public class StickersService extends RpcService {

    public StickersService(MTProtoClientGroupManager groupManager, StoreLayout storeLayout) {
        super(groupManager, storeLayout);
    }

    @Compatible(Type.BOT)
    public Mono<StickerSet> createStickerSet(CreateStickerSet request) {
        return sendMain(request);
    }

    @Compatible(Type.BOT)
    public Mono<StickerSet> removeStickerFromSet(InputDocument sticker) {
        return sendMain(ImmutableRemoveStickerFromSet.of(sticker));
    }

    @Compatible(Type.BOT)
    public Mono<StickerSet> changeStickerPosition(InputDocument sticker, int position) {
        return sendMain(ImmutableChangeStickerPosition.of(sticker, position));
    }

    @Compatible(Type.BOT)
    public Mono<StickerSet> addStickerToSet(InputStickerSet stickerSet, InputStickerSetItem sticker) {
        return sendMain(ImmutableAddStickerToSet.of(stickerSet, sticker));
    }

    @Compatible(Type.BOT)
    public Mono<StickerSet> setStickerSetThumb(InputStickerSet stickerSet, InputDocument thumb) {
        return sendMain(ImmutableSetStickerSetThumb.of(stickerSet, thumb));
    }

    public Mono<Boolean> checkShortName(String shortName) {
        return sendMain(ImmutableCheckShortName.of(shortName));
    }

    public Mono<String> suggestShortName(String title) {
        return sendMain(ImmutableSuggestShortName.of(title)).map(SuggestedShortName::shortName);
    }
}
