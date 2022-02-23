package telegram4j.mtproto.service;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.BotCompatible;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.InputStickerSet;
import telegram4j.tl.InputStickerSetItem;
import telegram4j.tl.messages.StickerSetWithDocuments;
import telegram4j.tl.request.stickers.*;
import telegram4j.tl.stickers.SuggestedShortName;

public class StickersService extends RpcService {

    public StickersService(MTProtoClient client, StoreLayout storeLayout) {
        super(client, storeLayout);
    }

    @BotCompatible
    public Mono<StickerSetWithDocuments> createStickerSet(CreateStickerSet request) {
        return client.sendAwait(request);
    }

    @BotCompatible
    public Mono<StickerSetWithDocuments> removeStickerFromSet(String stickerFileReferenceId) {
        return Mono.defer(() -> client.sendAwait(ImmutableRemoveStickerFromSet.of(
                FileReferenceId.deserialize(stickerFileReferenceId).asInputDocument())));
    }

    @BotCompatible
    public Mono<StickerSetWithDocuments> changeStickerPosition(String stickerFileReferenceId, int position) {
        return Mono.defer(() -> client.sendAwait(ImmutableChangeStickerPosition.of(
                FileReferenceId.deserialize(stickerFileReferenceId).asInputDocument(), position)));
    }

    @BotCompatible
    public Mono<StickerSetWithDocuments> addStickerToSet(InputStickerSet stickerSet, InputStickerSetItem sticker) {
        return client.sendAwait(ImmutableAddStickerToSet.of(stickerSet, sticker));
    }

    @BotCompatible
    public Mono<StickerSetWithDocuments> setStickerSetThumb(InputStickerSet stickerSet, String thumbFileReferenceId) {
        return Mono.defer(() -> client.sendAwait(ImmutableSetStickerSetThumb.of(stickerSet,
                FileReferenceId.deserialize(thumbFileReferenceId).asInputDocument())));
    }

    public Mono<Boolean> checkShortName(String shortName) {
        return client.sendAwait(ImmutableCheckShortName.of(shortName));
    }

    public Mono<String> suggestShortName(String title) {
        return client.sendAwait(ImmutableSuggestShortName.of(title)).map(SuggestedShortName::shortName);
    }
}
