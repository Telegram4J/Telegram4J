package telegram4j.mtproto.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.annotation.Nullable;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import telegram4j.mtproto.BotCompatible;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.ExportedChatInvite;
import telegram4j.tl.*;
import telegram4j.tl.messages.MessageViews;
import telegram4j.tl.messages.PeerSettings;
import telegram4j.tl.messages.*;
import telegram4j.tl.request.messages.UpdateDialogFilter;
import telegram4j.tl.request.messages.*;

import java.util.function.Function;

import static telegram4j.mtproto.util.EmissionHandlers.DEFAULT_PARKING;

public class MessageService extends RpcService {

    public MessageService(MTProtoClient client, StoreLayout storeLayout) {
        super(client, storeLayout);
    }

    @BotCompatible
    public Mono<Messages> getMessages(InputChannel channel, Iterable<? extends InputMessage> ids) {
        return client.sendAwait(telegram4j.tl.request.channels.GetMessages.builder().channel(channel).id(ids).build());
    }

    @BotCompatible
    public Mono<Messages> getMessages(Iterable<? extends InputMessage> ids) {
        return client.sendAwait(GetMessages.builder().id(ids).build());
    }

    public Mono<Dialogs> getDialogs(GetDialogs request) {
        return client.sendAwait(request);
    }

    public Mono<Messages> getHistory(InputPeer peer, int offsetId, int offsetDate, int addOffset,
                                     int limit, int maxId, int minId, long hash) {
        return client.sendAwait(ImmutableGetHistory.of(peer, offsetId, offsetDate, addOffset, limit, maxId, minId, hash));
    }

    @BotCompatible
    public Mono<AffectedMessages> deleteMessages(boolean revoke, Iterable<Integer> ids) {
        return client.sendAwait(DeleteMessages.builder().revoke(revoke).id(ids).build());
    }

    @BotCompatible
    public Mono<AffectedMessages> deleteMessages(InputChannel channel, Iterable<Integer> ids) {
        return client.sendAwait(telegram4j.tl.request.channels.DeleteMessages.builder().channel(channel).id(ids).build());
    }

    public Mono<Messages> search(Search request) {
        return client.sendAwait(request);
    }

    public Mono<AffectedMessages> readHistory(InputPeer peer, int maxId) {
        return client.sendAwait(ImmutableReadHistory.of(peer, maxId));
    }

    public Mono<AffectedHistory> deleteHistory(DeleteHistory request) {
        return client.sendAwait(request);
    }

    public Flux<ReceivedNotifyMessage> receivedMessages(int maxId) {
        return client.sendAwait(ImmutableReceivedMessages.of(maxId))
                .flatMapIterable(Function.identity());
    }

    @BotCompatible
    public Mono<Boolean> setTyping(InputPeer peer, @Nullable Integer topMsgId, SendMessageAction action) {
        return client.sendAwait(SetTyping.builder()
                .peer(peer)
                .topMsgId(topMsgId)
                .action(action)
                .build());
    }

    @BotCompatible
    public Flux<BaseMessageFields> forwardMessages(ForwardMessages request) {
        return client.sendAwait(request)
                .ofType(BaseUpdates.class)
                .flatMapMany(updates -> {
                    client.updates().emitNext(updates, DEFAULT_PARKING);

                    return Flux.fromIterable(updates.updates())
                            .ofType(UpdateNewMessageFields.class)
                            .map(UpdateNewMessageFields::message)
                            .ofType(BaseMessageFields.class);
                });
    }

    public Mono<Boolean> reportSpam(InputPeer peer) {
        return client.sendAwait(ImmutableReportSpam.of(peer));
    }

    public Mono<PeerSettings> getPeerSettings(InputPeer peer) {
        return client.sendAwait(ImmutableGetPeerSettings.of(peer));
    }

    public Mono<Boolean> report(InputPeer peer, Iterable<Integer> ids, ReportReason reason, String message) {
        return client.sendAwait(Report.builder()
                .peer(peer)
                .id(ids)
                .reason(reason)
                .message(message)
                .build());
    }

    public Mono<DhConfig> getDhConfig(int version, int randomLength) {
        return client.sendAwait(ImmutableGetDhConfig.of(version, randomLength));
    }

    public Mono<EncryptedChat> requestEncryption(InputUser user, int randomId, byte[] ga) {
        return client.sendAwait(ImmutableRequestEncryption.of(user, randomId, ga));
    }

    public Mono<EncryptedChat> acceptEncryption(InputEncryptedChat peer, byte[] ga, long fingerprint) {
        return client.sendAwait(ImmutableAcceptEncryption.of(peer, ga, fingerprint));
    }

    public Mono<Boolean> discardEncryption(boolean deleteEncryption, int chatId) {
        return client.sendAwait(DiscardEncryption.builder()
                .deleteHistory(deleteEncryption)
                .chatId(chatId)
                .build());
    }

    public Mono<Boolean> setEncryptedTyping(InputEncryptedChat peer, boolean typing) {
        return client.sendAwait(ImmutableSetEncryptedTyping.of(peer, typing));
    }

    public Mono<Boolean> readEncryptedHistory(InputEncryptedChat peer, int maxDate) {
        return client.sendAwait(ImmutableReadEncryptedHistory.of(peer, maxDate));
    }

    public Mono<SentEncryptedMessage> sendEncrypted(SendEncrypted request) {
        return client.sendAwait(request);
    }

    public Mono<SentEncryptedMessage> sendEncryptedFile(SendEncryptedFile request) {
        return client.sendAwait(request);
    }

    public Flux<Long> receivedQueue(int maxQts) {
        return client.sendAwait(ImmutableReceivedQueue.of(maxQts))
                .flatMapIterable(Function.identity());
    }

    public Mono<AffectedMessages> readMessageContents(Iterable<Integer> ids) {
        return client.sendAwait(ReadMessageContents.builder().id(ids).build());
    }

    public Mono<Stickers> getStickers(String emoticon, long hash) {
        return client.sendAwait(ImmutableGetStickers.of(emoticon, hash));
    }

    public Mono<AllStickers> getAllStickers(long hash) {
        return client.sendAwait(ImmutableGetAllStickers.of(hash));
    }

    public Mono<MessageMedia> getWebPagePreview(String message, @Nullable Iterable<? extends MessageEntity> entities) {
        return client.sendAwait(GetWebPagePreview.builder()
                .message(message)
                .entities(entities)
                .build());
    }

    public Mono<ExportedChatInvite> exportChatInvite(ExportChatInvite request) {
        return client.sendAwait(request);
    }

    public Mono<ChatInvite> checkChatInvite(String hash) {
        return client.sendAwait(ImmutableCheckChatInvite.of(hash));
    }

    // TODO: check updates type
    public Mono<Updates> importChatInvite(String hash) {
        return client.sendAwait(ImmutableImportChatInvite.of(hash));
    }

    public Mono<StickerSetWithDocuments> getStickerSet(InputStickerSet stickerSet, int hash) {
        return client.sendAwait(ImmutableGetStickerSet.of(stickerSet, hash));
    }

    public Mono<StickerSetInstallResult> installStickerSet(InputStickerSet stickerSet, boolean archived) {
        return client.sendAwait(ImmutableInstallStickerSet.of(stickerSet, archived));
    }

    public Mono<Boolean> uninstallStickerSet(InputStickerSet stickerSet) {
        return client.sendAwait(ImmutableUninstallStickerSet.of(stickerSet));
    }

    public Mono<Updates> startBot(InputUser bot, InputPeer peer, long randomId, String startParam) {
        return client.sendAwait(ImmutableStartBot.of(bot, peer, randomId, startParam));
    }

    public Mono<MessageViews> getMessagesViews(InputPeer peer, Iterable<Integer> ids, boolean increment) {
        return client.sendAwait(GetMessagesViews.builder()
                .peer(peer)
                .id(ids)
                .increment(increment)
                .build());
    }

    public Mono<Boolean> editChatAdmin(long chatId, InputUser user, boolean isAdmin) {
        return client.sendAwait(ImmutableEditChatAdmin.of(chatId, user, isAdmin));
    }

    public Mono<Updates> migrateChat(long chatId) {
        return client.sendAwait(ImmutableMigrateChat.of(chatId));
    }

    public Mono<Messages> searchGlobal(SearchGlobal request) {
        return client.sendAwait(request);
    }

    public Mono<Boolean> reorderStickerSet(boolean masks, Iterable<Long> order) {
        return client.sendAwait(ReorderStickerSets.builder()
                .masks(masks)
                .order(order)
                .build());
    }

    public Mono<Document> getDocumentByHash(byte[] sha256, int size, String mimeType) {
        return client.sendAwait(ImmutableGetDocumentByHash.of(sha256, size, mimeType));
    }

    public Mono<SavedGifs> getSavedGifs(long hash) {
        return client.sendAwait(ImmutableGetSavedGifs.of(hash));
    }

    public Mono<Boolean> saveGif(String documentFileReferenceId, boolean unsave) {
        return Mono.defer(() -> client.sendAwait(ImmutableSaveGif.of(
                FileReferenceId.deserialize(documentFileReferenceId).asInputDocument(), unsave)));
    }

    public Mono<BotResults> getInlineBotResults(InputUser bot, InputPeer peer, @Nullable InputGeoPoint geoPoint,
                                                String query, String offset) {
        return client.sendAwait(GetInlineBotResults.builder()
                .bot(bot)
                .peer(peer)
                .geoPoint(geoPoint)
                .query(query)
                .offset(offset)
                .build());
    }

    public Mono<Updates> sendInlineBotResult(SendInlineBotResult request) {
        return client.sendAwait(request);
    }

    public Mono<Boolean> getMessageEditData(InputPeer peer, int id) {
        return client.sendAwait(ImmutableGetMessageEditData.of(peer, id)).map(MessageEditData::caption);
    }

    @BotCompatible
    public Mono<Boolean> editInlineBotMessage(EditInlineBotMessage request) {
        return client.sendAwait(request);
    }

    public Mono<BotCallbackAnswer> getBotCallbackAnswer(GetBotCallbackAnswer request) {
        return client.sendAwait(request);
    }

    @BotCompatible
    public Mono<Boolean> setBotCallbackAnswer(SetBotCallbackAnswer request) {
        return client.sendAwait(request);
    }

    public Mono<PeerDialogs> getPeerDialogs(Iterable<? extends InputDialogPeer> peers) {
        return client.sendAwait(GetPeerDialogs.builder().peers(peers).build());
    }

    public Mono<Boolean> saveDraft(SaveDraft request) {
        return client.sendAwait(request);
    }

    // TODO: check updates type
    public Mono<Updates> getAllDrafts() {
        return client.sendAwait(GetAllDrafts.instance());
    }

    public Mono<FeaturedStickers> getFeaturedStickers(long hash) {
        return client.sendAwait(ImmutableGetFeaturedStickers.of(hash));
    }

    public Mono<Boolean> readFeaturedStickers(Iterable<Long> ids) {
        return client.sendAwait(ReadFeaturedStickers.builder().id(ids).build());
    }

    public Mono<RecentStickers> getRecentStickers(boolean attached, long hash) {
        return client.sendAwait(GetRecentStickers.builder()
                .attached(attached)
                .hash(hash)
                .build());
    }

    public Mono<Boolean> saveRecentSticker(boolean attached, String documentFileReferenceId, boolean unsave) {
        return Mono.defer(() -> client.sendAwait(SaveRecentSticker.builder()
                .attached(attached)
                .id(FileReferenceId.deserialize(documentFileReferenceId).asInputDocument())
                .unsave(unsave)
                .build()));
    }

    public Mono<Boolean> clearRecentStickers(boolean attached) {
        return client.sendAwait(ClearRecentStickers.builder().attached(attached).build());
    }

    public Mono<ArchivedStickers> getArchivedStickers(boolean marks, long offsetId, int limit) {
        return client.sendAwait(GetArchivedStickers.builder()
                .masks(marks)
                .offsetId(offsetId)
                .limit(limit)
                .build());
    }

    public Mono<AllStickers> getMaskStickers(long hash) {
        return client.sendAwait(ImmutableGetMaskStickers.of(hash));
    }

    public Flux<StickerSetCovered> getAttachedStickers(InputStickeredMedia media) {
        return client.sendAwait(ImmutableGetAttachedStickers.of(media))
                .flatMapIterable(Function.identity());
    }

    public Mono<Updates> setGameScore(SetGameScore request) {
        return client.sendAwait(request);
    }

    public Mono<Boolean> setInlineGameScore(SetInlineGameScore request) {
        return client.sendAwait(request);
    }

    public Mono<HighScores> getGameHighScores(InputPeer peer, int id, InputUser user) {
        return client.sendAwait(ImmutableGetGameHighScores.of(peer, id, user));
    }

    public Mono<HighScores> getInlineGameHighScores(InputBotInlineMessageID id, InputUser user) {
        return client.sendAwait(ImmutableGetInlineGameHighScores.of(id, user));
    }

    public Mono<Chats> getCommonChats(InputUser user, long maxId, int limit) {
        return client.sendAwait(ImmutableGetCommonChats.of(user, maxId, limit));
    }

    public Mono<Chats> getAllChats(Iterable<Long> exceptIds) {
        return client.sendAwait(GetAllChats.builder()
                .exceptIds(exceptIds)
                .build());
    }

    public Mono<WebPage> getWebPage(String url, int hash) {
        return client.sendAwait(ImmutableGetWebPage.of(url, hash));
    }

    public Mono<Boolean> toggleDialogPin(boolean pinned, InputDialogPeer peer) {
        return client.sendAwait(ToggleDialogPin.builder()
                .pinned(pinned)
                .peer(peer)
                .build());
    }

    public Mono<Boolean> reorderPinnedDialogs(boolean force, int folderId, Iterable<? extends InputDialogPeer> order) {
        return client.sendAwait(ReorderPinnedDialogs.builder()
                .force(force)
                .folderId(folderId)
                .order(order)
                .build());
    }

    public Mono<PeerDialogs> getPinnedDialogs(int folderId) {
        return client.sendAwait(ImmutableGetPinnedDialogs.of(folderId));
    }

    @BotCompatible
    public Mono<Boolean> setBotShippingResults(long queryId, @Nullable String error,
                                               @Nullable Iterable<? extends ShippingOption> shippingOptions) {
        return client.sendAwait(SetBotShippingResults.builder()
                .queryId(queryId)
                .error(error)
                .shippingOptions(shippingOptions)
                .build());
    }

    @BotCompatible
    public Mono<Boolean> setBotPrecheckoutResults(boolean success, long queryId, @Nullable String error) {
        return client.sendAwait(SetBotPrecheckoutResults.builder()
                .success(success)
                .queryId(queryId)
                .error(error)
                .build());
    }

    @BotCompatible
    public Mono<MessageMedia> uploadMedia(InputPeer peer, InputMedia media) {
        return client.sendAwait(ImmutableUploadMedia.of(peer, media));
    }

    public Mono<Updates> sendScreenshotNotification(InputPeer peer, int replyToMsgId, long randomId) {
        return client.sendAwait(ImmutableSendScreenshotNotification.of(peer, replyToMsgId, randomId));
    }

    public Mono<FavedStickers> getFavedStickers(long hash) {
        return client.sendAwait(ImmutableGetFavedStickers.of(hash));
    }

    public Mono<Boolean> faveSticker(String documentFileReferenceId, boolean unfave) {
        return Mono.defer(() -> client.sendAwait(ImmutableFaveSticker.of(
                FileReferenceId.deserialize(documentFileReferenceId).asInputDocument(), unfave)));
    }

    public Mono<Messages> getUnreadMentions(InputPeer peer, int offsetId, int addOffset, int limit, int maxId, int minId) {
        return client.sendAwait(ImmutableGetUnreadMentions.of(peer, offsetId, addOffset, limit, maxId, minId));
    }

    public Mono<AffectedHistory> readMentions(InputPeer peer) {
        return client.sendAwait(ImmutableReadMentions.of(peer));
    }

    public Mono<Messages> getRecentLocations(InputPeer peer, int limit, long hash) {
        return client.sendAwait(ImmutableGetRecentLocations.of(peer, limit, hash));
    }

    @BotCompatible
    public Mono<Updates> sendMultiMedia(SendMultiMedia request) {
        return client.sendAwait(request);
    }

    public Mono<EncryptedFile> uploadEncryptedFile(InputEncryptedChat peer, InputEncryptedFile file) {
        return client.sendAwait(ImmutableUploadEncryptedFile.of(peer, file));
    }

    public Mono<FoundStickerSets> searchStickerSets(boolean excludeFeatured, String query, long hash) {
        return client.sendAwait(SearchStickerSets.builder()
                .excludeFeatured(excludeFeatured)
                .q(query)
                .hash(hash)
                .build());
    }

    public Flux<MessageRange> getSplitRanges() {
        return client.sendAwait(GetSplitRanges.instance())
                .flatMapIterable(Function.identity());
    }

    public Mono<Boolean> markDialogUnread(boolean unread, InputDialogPeer peer) {
        return client.sendAwait(MarkDialogUnread.builder()
                .unread(unread)
                .peer(peer)
                .build());
    }

    public Flux<DialogPeer> getDialogUnreadMarks() {
        return client.sendAwait(GetDialogUnreadMarks.instance())
                .flatMapIterable(Function.identity());
    }

    public Mono<Boolean> clearAllDrafts() {
        return client.sendAwait(ClearAllDrafts.instance());
    }

    @BotCompatible
    public Mono<Void> updatePinnedMessage(UpdatePinnedMessage request) {
        return client.sendAwait(request)
                .flatMap(u -> {
                    switch (u.identifier()) {
                        case BaseUpdates.ID:
                            // This method can return 0-2 updates in a BaseUpdates and I don't know what I should return
                            client.updates().emitNext(u, DEFAULT_PARKING);

                            return Mono.empty();
                        default: return Mono.error(new IllegalStateException("Unknown updates type: " + u));
                    }
                });
    }

    public Mono<Updates> sendVote(InputPeer peer, int msgId, Iterable<byte[]> options) {
        return client.sendAwait(SendVote.builder()
                .peer(peer)
                .msgId(msgId)
                .options(options)
                .build());
    }

    public Mono<Updates> getPollResults(InputPeer peer, int msgId) {
        return client.sendAwait(ImmutableGetPollResults.of(peer, msgId));
    }

    public Mono<Integer> getOnlines(InputPeer peer) {
        return client.sendAwait(ImmutableGetOnlines.of(peer)).map(ChatOnlines::onlines);
    }

    @BotCompatible
    public Mono<Boolean> editChatAbout(InputPeer peer, String about) {
        return client.sendAwait(ImmutableEditChatAbout.of(peer, about));
    }

    @BotCompatible
    public Mono<Updates> editChatDefaultBannedRights(InputPeer peer, ChatBannedRights rights) {
        return client.sendAwait(ImmutableEditChatDefaultBannedRights.of(peer, rights));
    }

    public Mono<EmojiKeywordsDifference> getEmojiKeywordsDifference(String langCode, int fromVersion) {
        return client.sendAwait(ImmutableGetEmojiKeywordsDifference.of(langCode, fromVersion));
    }

    public Flux<EmojiLanguage> getEmojiKeywordsLanguages(Iterable<String> langCodes) {
        return client.sendAwait(GetEmojiKeywordsLanguages.builder().langCodes(langCodes).build())
                .flatMapIterable(Function.identity());
    }

    public Mono<String> getEmojiUrl(String langCode) {
        return client.sendAwait(ImmutableGetEmojiURL.of(langCode)).map(EmojiURL::url);
    }

    public Flux<SearchCounter> getSearchCounters(InputPeer peer, Iterable<? extends MessagesFilter> filters) {
        return client.sendAwait(GetSearchCounters.builder()
                        .peer(peer)
                        .filters(filters)
                        .build())
                .flatMapIterable(Function.identity());
    }

    public Mono<UrlAuthResult> requestUrlAuth(@Nullable InputPeer peer, @Nullable Integer msgId,
                                              @Nullable Integer buttonId, @Nullable String url) {
        return client.sendAwait(RequestUrlAuth.builder()
                .peer(peer)
                .msgId(msgId)
                .buttonId(buttonId)
                .url(url)
                .build());
    }

    public Mono<UrlAuthResult> acceptUrlAuth(AcceptUrlAuth request) {
        return client.sendAwait(request);
    }

    public Mono<Boolean> hidePeerSettingsBar(InputPeer peer) {
        return client.sendAwait(ImmutableHidePeerSettingsBar.of(peer));
    }

    public Mono<Messages> getScheduledHistory(InputPeer peer, long hash) {
        return client.sendAwait(ImmutableGetScheduledHistory.of(peer, hash));
    }

    public Mono<Messages> getScheduledMessages(InputPeer peer, Iterable<Integer> ids) {
        return client.sendAwait(GetScheduledMessages.builder()
                .peer(peer)
                .id(ids)
                .build());
    }

    public Mono<Updates> sendScheduledMessages(InputPeer peer, Iterable<Integer> ids) {
        return client.sendAwait(SendScheduledMessages.builder()
                .peer(peer)
                .id(ids)
                .build());
    }

    public Mono<Updates> deleteScheduledMessages(InputPeer peer, Iterable<Integer> ids) {
        return client.sendAwait(DeleteScheduledMessages.builder()
                .peer(peer)
                .id(ids)
                .build());
    }

    public Mono<VotesList> getPollVotes(InputPeer peer, int id, @Nullable byte[] option,
                                        @Nullable String offset, int limit) {
        return client.sendAwait(GetPollVotes.builder()
                .peer(peer)
                .id(id)
                .option(option) // It's ok
                .offset(offset)
                .limit(limit)
                .build());
    }

    public Mono<Boolean> toggleStickerSets(ToggleStickerSets request) {
        return client.sendAwait(request);
    }

    public Flux<DialogFilter> getDialogFilters() {
        return client.sendAwait(GetDialogFilters.instance())
                .flatMapIterable(Function.identity());
    }

    public Flux<DialogFilterSuggested> getSuggestedDialogFilters() {
        return client.sendAwait(GetSuggestedDialogFilters.instance())
                .flatMapIterable(Function.identity());
    }

    public Mono<Boolean> updateDialogFilter(int id, @Nullable DialogFilter filter) {
        return client.sendAwait(UpdateDialogFilter.builder()
                .id(id)
                .filter(filter)
                .build());
    }

    public Mono<Boolean> updateDialogFiltersOrder(Iterable<Integer> order) {
        return client.sendAwait(UpdateDialogFiltersOrder.builder().order(order).build());
    }

    public Mono<FeaturedStickers> getOldFeaturedStickers(int offset, int limit, long hash) {
        return client.sendAwait(ImmutableGetOldFeaturedStickers.of(offset, limit, hash));
    }

    public Mono<Messages> getReplies(InputPeer peer, int msgId, int offsetId, int offsetDate,
                                     int addOffset, int limit, int maxId, int minId, long hash) {
        return client.sendAwait(ImmutableGetReplies.of(peer, msgId, offsetId, offsetDate,
                addOffset, limit, maxId, minId, hash));
    }

    public Mono<DiscussionMessage> getDiscussionMessage(InputPeer peer, int msgId) {
        return client.sendAwait(ImmutableGetDiscussionMessage.of(peer, msgId));
    }

    public Mono<Boolean> readDiscussion(InputPeer peer, int msgId, int readMaxId) {
        return client.sendAwait(ImmutableReadDiscussion.of(peer, msgId, readMaxId));
    }

    public Mono<AffectedHistory> unpingAllMessages(InputPeer peer) {
        return client.sendAwait(ImmutableUnpinAllMessages.of(peer));
    }

    public Mono<Boolean> deleteChat(long chatId) {
        return client.sendAwait(ImmutableDeleteChat.of(chatId));
    }

    public Mono<AffectedFoundMessages> deletePhoneCallHistory(boolean revoked) {
        return client.sendAwait(DeletePhoneCallHistory.builder()
                .revoke(revoked)
                .build());
    }

    public Mono<HistoryImportParsed> checkHistoryImport(String importHead) {
        return client.sendAwait(ImmutableCheckHistoryImport.of(importHead));
    }

    public Mono<HistoryImport> initHistoryImport(InputPeer peer, InputFile file, int mediaCount) {
        return client.sendAwait(ImmutableInitHistoryImport.of(peer, file, mediaCount));
    }

    public Mono<MessageMedia> uploadImportedMedia(InputPeer peer, long importId, String fileName, InputMedia media) {
        return client.sendAwait(ImmutableUploadImportedMedia.of(peer, importId, fileName, media));
    }

    public Mono<Boolean> startHistoryImport(InputPeer peer, long importId) {
        return client.sendAwait(ImmutableStartHistoryImport.of(peer, importId));
    }

    public Mono<ExportedChatInvites> getExportedChatInvites(GetExportedChatInvites request) {
        return client.sendAwait(request);
    }

    public Mono<telegram4j.tl.messages.ExportedChatInvite> getExportedChatInvite(InputPeer peer, String link) {
        return client.sendAwait(ImmutableGetExportedChatInvite.of(peer, link));
    }

    public Mono<telegram4j.tl.messages.ExportedChatInvite> editExportedChatInvite(EditExportedChatInvite request) {
        return client.sendAwait(request);
    }

    public Mono<Boolean> deleteRevokedExportedChatInvites(InputPeer peer, InputUser admin) {
        return client.sendAwait(ImmutableDeleteRevokedExportedChatInvites.of(peer, admin));
    }

    public Mono<Boolean> deleteExportedChatInvite(InputPeer peer, String link) {
        return client.sendAwait(ImmutableDeleteExportedChatInvite.of(peer, link));
    }

    public Mono<ChatAdminsWithInvites> getAdminsWithInvites(InputPeer peer) {
        return client.sendAwait(ImmutableGetAdminsWithInvites.of(peer));
    }

    public Mono<ChatInviteImporters> getChatInviteImporters(GetChatInviteImporters request) {
        return client.sendAwait(request);
    }

    public Mono<Void> setHistoryTtl(InputPeer peer, int period) {
        return client.sendAwait(ImmutableSetHistoryTTL.of(peer, period))
                // Typically, UpdatePeerHistoryTTL,UpdateNewMessage(service message)
                .cast(BaseUpdates.class)
                .flatMap(u -> Mono.fromRunnable(() -> client.updates().emitNext(u, DEFAULT_PARKING)));
    }

    public Mono<CheckedHistoryImportPeer> checkHistoryImportPeer(InputPeer peer) {
        return client.sendAwait(ImmutableCheckHistoryImportPeer.of(peer));
    }

    public Mono<Updates> setChatTheme(InputPeer peer, String emoticon) {
        return client.sendAwait(ImmutableSetChatTheme.of(peer, emoticon));
    }

    public Flux<Long> getMessageReadParticipants(InputPeer peer, int msgId) {
        return client.sendAwait(ImmutableGetMessageReadParticipants.of(peer, msgId))
                .flatMapIterable(Function.identity());
    }

    // Message interactions

    @BotCompatible
    public Mono<BaseMessageFields> sendMessage(SendMessage request) {
        return client.sendAwait(request)
                .zipWith(toPeer(request.peer()))
                .map(TupleUtils.function((updates, peer) -> {
                    var upd = transformMessageUpdate(request, updates, peer);

                    client.updates().emitNext(upd.getT2(), DEFAULT_PARKING);

                    return upd.getT1();
                }));
    }

    @BotCompatible
    public Mono<BaseMessageFields> sendMedia(SendMedia request) {
        return client.sendAwait(request)
                .zipWith(toPeer(request.peer()))
                .map(TupleUtils.function((updates, peer) -> {
                    var upd = transformMessageUpdate(request, updates, peer);

                    client.updates().emitNext(upd.getT2(), DEFAULT_PARKING);

                    return upd.getT1();
                }));
    }

    @BotCompatible
    public Mono<BaseMessageFields> editMessage(EditMessage request) {
        return client.sendAwait(request)
                .map(updates -> {
                    switch (updates.identifier()) {
                        case BaseUpdates.ID:
                            BaseUpdates casted = (BaseUpdates) updates;

                            UpdateEditMessageFields update = casted.updates().stream()
                                    .filter(upd -> upd instanceof UpdateEditMessageFields)
                                    .map(upd -> (UpdateEditMessageFields) upd)
                                    .findFirst()
                                    .orElseThrow();

                            client.updates().emitNext(updates, DEFAULT_PARKING);

                            return (BaseMessageFields) update.message();
                        default:
                            throw new IllegalArgumentException("Unknown updates type: " + updates);
                    }
                });
    }

    // Short-send related updates object should be transformed to the updateShort or baseUpdates.
    // https://core.telegram.org/api/updates-sequence
    static Tuple2<BaseMessageFields, Updates> transformMessageUpdate(BaseSendMessageRequest request, Updates updates, Peer peer) {
        switch (updates.identifier()) {
            case UpdateShortSentMessage.ID: {
                UpdateShortSentMessage casted = (UpdateShortSentMessage) updates;
                Integer replyToMsgId = request.replyToMsgId();
                var message = BaseMessage.builder()
                        .flags(request.flags() | casted.flags())
                        .peerId(peer)
                        .replyTo(replyToMsgId != null ? ImmutableMessageReplyHeader.of(replyToMsgId) : null)
                        .message(request.message())
                        .id(casted.id())
                        .replyMarkup(request.replyMarkup())
                        .media(casted.media())
                        .entities(casted.entities())
                        .date(casted.date())
                        .ttlPeriod(casted.ttlPeriod())
                        .build();

                Updates upds = UpdateShort.builder()
                        .date(casted.date())
                        .update(UpdateNewMessage.builder()
                                .message(message)
                                .pts(casted.pts())
                                .ptsCount(casted.ptsCount())
                                .build())
                        .build();

                return Tuples.of(message, upds);
            }
            case UpdateShortMessage.ID: {
                UpdateShortMessage casted = (UpdateShortMessage) updates;

                var message = BaseMessage.builder()
                        .flags(request.flags() | casted.flags())
                        .peerId(peer)
                        .replyTo(casted.replyTo())
                        .message(request.message())
                        .id(casted.id())
                        .replyMarkup(request.replyMarkup())
                        .fwdFrom(casted.fwdFrom())
                        .entities(casted.entities())
                        .date(casted.date())
                        .viaBotId(casted.viaBotId())
                        .ttlPeriod(casted.ttlPeriod())
                        .build();

                Updates upds = UpdateShort.builder()
                        .date(casted.date())
                        .update(UpdateNewMessage.builder()
                                .message(message)
                                .pts(casted.pts())
                                .ptsCount(casted.ptsCount())
                                .build())
                        .build();

                return Tuples.of(message, upds);
            }
            case UpdateShortChatMessage.ID: {
                UpdateShortChatMessage casted = (UpdateShortChatMessage) updates;

                var message = BaseMessage.builder()
                        .flags(request.flags() | casted.flags())
                        .peerId(peer)
                        .viaBotId(casted.viaBotId())
                        .replyTo(casted.replyTo())
                        .fwdFrom(casted.fwdFrom())
                        .message(request.message())
                        .id(casted.id())
                        .replyMarkup(request.replyMarkup())
                        .entities(casted.entities())
                        .date(casted.date())
                        .ttlPeriod(casted.ttlPeriod())
                        .build();

                Updates upds = UpdateShort.builder()
                        .date(casted.date())
                        .update(UpdateNewMessage.builder()
                                .message(message)
                                .pts(casted.pts())
                                .ptsCount(casted.ptsCount())
                                .build())
                        .build();

                return Tuples.of(message, upds);
            }
            case BaseUpdates.ID: {
                BaseUpdates casted = (BaseUpdates) updates;

                UpdateMessageID updateMessageID = casted.updates().stream()
                        .filter(u -> u.identifier() == UpdateMessageID.ID)
                        .map(upd -> (UpdateMessageID) upd)
                        .findFirst()
                        .orElseThrow();

                if (updateMessageID.randomId() != request.randomId()) {
                    throw new IllegalArgumentException("Incorrect random id. Excepted: " + request.randomId()
                            + ", received: " + updateMessageID.randomId());
                }

                var message = casted.updates().stream()
                        .filter(upd -> upd instanceof UpdateNewMessageFields)
                        .map(upd -> (BaseMessageFields) ((UpdateNewMessageFields) upd).message())
                        .findFirst()
                        .orElseThrow();

                return Tuples.of(message, casted);
            }
            default:
                throw new IllegalArgumentException("Unknown updates type: " + updates);
        }
    }
}
