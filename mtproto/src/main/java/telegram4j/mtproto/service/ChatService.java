package telegram4j.mtproto.service;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.annotation.Nullable;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import telegram4j.mtproto.MTProtoClientGroupManager;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.mtproto.service.Compatible.Type;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.ExportedChatInvite;
import telegram4j.tl.*;
import telegram4j.tl.channels.ChannelParticipant;
import telegram4j.tl.channels.*;
import telegram4j.tl.messages.ChatFull;
import telegram4j.tl.messages.MessageViews;
import telegram4j.tl.messages.PeerSettings;
import telegram4j.tl.messages.*;
import telegram4j.tl.request.channels.ImmutableDeleteHistory;
import telegram4j.tl.request.channels.ImmutableDeleteMessages;
import telegram4j.tl.request.channels.ImmutableGetMessages;
import telegram4j.tl.request.channels.ImmutableReadHistory;
import telegram4j.tl.request.channels.ImmutableReadMessageContents;
import telegram4j.tl.request.channels.ImmutableReportSpam;
import telegram4j.tl.request.channels.*;
import telegram4j.tl.request.folders.ImmutableDeleteFolder;
import telegram4j.tl.request.folders.ImmutableEditPeerFolders;
import telegram4j.tl.request.messages.DeleteHistory;
import telegram4j.tl.request.messages.DeleteMessages;
import telegram4j.tl.request.messages.UpdateDialogFilter;
import telegram4j.tl.request.messages.*;

import java.util.List;
import java.util.Objects;

import static telegram4j.mtproto.util.EmissionHandlers.DEFAULT_PARKING;

/** Rpc service with chat and channel related methods. */
public class ChatService extends RpcService {

    public ChatService(MTProtoClientGroupManager groupManager, StoreLayout storeLayout) {
        super(groupManager, storeLayout);
    }

    // additional methods
    // =========================

    /**
     * Retrieve minimal chat by given id.
     * This method can return only {@link BaseChat}, {@link ChatForbidden} or {@link ChatEmpty} objects.
     *
     * @param id The id of chat
     * @return A {@link Mono} emitting on successful completion minimal information about chat
     */
    @Compatible(Type.BOTH)
    public Mono<Chat> getChat(long id) {
        return getChats(List.of(id))
                .cast(BaseChats.class)
                .mapNotNull(c -> c.chats().isEmpty() ? null : c.chats().get(0));
    }

    /**
     * Retrieve minimal channel by given id.
     * This method can return only {@link Channel} or {@link ChannelForbidden} objects.
     *
     * @param id The id of channel
     * @return A {@link Mono} emitting on successful completion minimal information about channel
     */
    @Compatible(Type.BOTH)
    public Mono<Chat> getChannel(InputChannel id) {
        return getChannels(List.of(id))
                .cast(BaseChats.class)
                .mapNotNull(c -> c.chats().isEmpty() ? null : c.chats().get(0));
    }

    // messages namespace
    // =========================
    // TODO list:
    // getMessageReadParticipants#2c6f97b7 peer:InputPeer msg_id:int = Vector<long>;
    // getSearchResultsCalendar#49f0bde9 peer:InputPeer filter:MessagesFilter offset_id:int offset_date:int = messages.SearchResultsCalendar;
    // getSearchResultsPositions#6e9583a3 peer:InputPeer filter:MessagesFilter offset_id:int limit:int = messages.SearchResultsPositions;
    // hideChatJoinRequest#7fe7e815 flags:# approved:flags.0?true peer:InputPeer user_id:InputUser = Updates;
    // hideAllChatJoinRequests#e085f4ea flags:# approved:flags.0?true peer:InputPeer link:flags.1?string = Updates;
    // toggleNoForwards#b11eafa2 peer:InputPeer enabled:Bool = Updates;
    // saveDefaultSendAs#ccfddf96 peer:InputPeer send_as:InputPeer = Bool;
    // sendReaction#25690ce4 flags:# big:flags.1?true peer:InputPeer msg_id:int reaction:flags.0?string = Updates;
    // setChatAvailableReactions#14050ea6 peer:InputPeer available_reactions:Vector<string> = Updates;
    // getAvailableReactions#18dea0ac hash:int = messages.AvailableReactions;
    // setDefaultReaction#d960c4d4 reaction:string = Bool;
    // getUnreadReactions#e85bae1a peer:InputPeer offset_id:int add_offset:int limit:int max_id:int min_id:int = messages.Messages;
    // readReactions#82e251d7 peer:InputPeer = messages.AffectedHistory;
    // searchSentMedia#107e31a0 q:string filter:MessagesFilter limit:int = messages.Messages;
    // getAttachMenuBots#16fcc2cb hash:long = AttachMenuBots;
    // getAttachMenuBot#77216192 bot:InputUser = AttachMenuBotsBot;
    // toggleBotInAttachMenu#1aee33af bot:InputUser enabled:Bool = Bool;
    // requestWebView#91b15831 flags:# from_bot_menu:flags.4?true silent:flags.5?true peer:InputPeer bot:InputUser url:flags.1?string start_param:flags.3?string theme_params:flags.2?DataJSON reply_to_msg_id:flags.0?int send_as:flags.13?InputPeer = WebViewResult;
    // prolongWebView#ea5fbcce flags:# silent:flags.5?true peer:InputPeer bot:InputUser query_id:long reply_to_msg_id:flags.0?int send_as:flags.13?InputPeer = Bool;

    @Compatible(Type.BOTH)
    public Mono<Messages> getMessages(Iterable<? extends InputMessage> ids) {
        return Mono.defer(() -> sendMain(telegram4j.tl.request.messages.ImmutableGetMessages.of(ids)))
                .flatMap(m -> storeLayout.onMessages(m).thenReturn(m));
    }

    public Mono<SentEncryptedMessage> sendEncryptedService(InputEncryptedChat peer, long randomId, ByteBuf data) {
        return Mono.defer(() -> sendMain(ImmutableSendEncryptedService.of(peer, randomId, data)));
    }

    public Mono<SimpleWebViewResult> requestSimpleWebView(InputUser bot, String url, String platform,
                                                          @Nullable String themeParamsJson) {
        return sendMain(ImmutableRequestSimpleWebView.of(bot, url, platform)
                .withThemeParams(themeParamsJson != null ? ImmutableDataJSON.of(themeParamsJson) : null));
    }

    public Mono<WebViewMessageSent> sendWebViewResultMessage(String botQueryId, InputBotInlineResult result) {
        return sendMain(ImmutableSendWebViewResultMessage.of(botQueryId, result));
    }

    public Mono<Updates> sendWebViewData(InputUser bot, long randomId, String buttonText, String data) {
        return sendMain(ImmutableSendWebViewData.of(bot, randomId, buttonText, data));
    }

    public Mono<TranscribedAudio> transcribeAudio(InputPeer peer, int messageId) {
        return sendMain(ImmutableTranscribeAudio.of(peer, messageId));
    }

    public Mono<Boolean> rateTranscribedAudio(InputPeer peer, int messageId, long transcriptionId, boolean good) {
        return sendMain(ImmutableRateTranscribedAudio.of(peer, messageId, transcriptionId, good));
    }

    @Compatible(Type.BOTH)
    public Mono<List<Document>> getCustomEmojiDocuments(Iterable<Long> documentIds) {
        return Mono.defer(() -> sendMain(ImmutableGetCustomEmojiDocuments.of(documentIds)));
    }

    public Mono<String> translateText(InputPeer peer, Integer messageId,
                                      @Nullable String fromLang, String toLang) {
        Objects.requireNonNull(peer);
        Objects.requireNonNull(messageId);
        return sendMain(ImmutableTranslateText.of(toLang)
                .withPeer(peer)
                .withMsgId(messageId)
                .withFromLang(fromLang))
                .ofType(TranslateResultText.class)
                .map(TranslateResultText::text);
    }

    public Mono<String> translateText(String text, @Nullable String fromLang, String toLang) {
        Objects.requireNonNull(text);
        return sendMain(ImmutableTranslateText.of(toLang)
                        .withText(text)
                        .withFromLang(fromLang))
                .ofType(TranslateResultText.class)
                .map(TranslateResultText::text);
    }

    public Mono<AllStickers> getEmojiStickers(long hash) {
        return sendMain(ImmutableGetEmojiStickers.of(hash));
    }

    public Mono<FeaturedStickers> getFeaturedEmojiStickers(long hash) {
        return sendMain(ImmutableGetFeaturedStickers.of(hash));
    }

    public Mono<Dialogs> getDialogs(GetDialogs request) {
        return sendMain(request);
    }

    public Mono<Messages> getHistory(GetHistory request) {
        return sendMain(request);
    }

    @Compatible(Type.BOTH)
    public Mono<AffectedMessages> deleteMessages(boolean revoke, Iterable<Integer> ids) {
        return sendMain(DeleteMessages.builder().revoke(revoke).id(ids).build());
    }

    public Mono<Messages> search(Search request) {
        return sendMain(request);
    }

    public Mono<AffectedMessages> readHistory(InputPeer peer, int maxId) {
        return sendMain(telegram4j.tl.request.messages.ImmutableReadHistory.of(peer, maxId));
    }

    public Mono<AffectedHistory> deleteHistory(DeleteHistory request) {
        return sendMain(request);
    }

    public Mono<List<ReceivedNotifyMessage>> receivedMessages(int maxId) {
        return sendMain(ImmutableReceivedMessages.of(maxId));
    }

    @Compatible(Type.BOTH)
    public Mono<Boolean> setTyping(InputPeer peer, @Nullable Integer topMsgId, SendMessageAction action) {
        return sendMain(ImmutableSetTyping.of(peer, action)
                .withTopMsgId(topMsgId));
    }

    @Compatible(Type.BOTH)
    public Flux<BaseMessageFields> forwardMessages(ForwardMessages request) {
        return sendMain(request)
                .ofType(BaseUpdates.class)
                .flatMapMany(updates -> {
                    groupManager.main().updates().emitNext(updates, DEFAULT_PARKING);

                    return Flux.fromIterable(updates.updates())
                            .ofType(UpdateNewMessageFields.class)
                            .map(UpdateNewMessageFields::message)
                            .ofType(BaseMessageFields.class);
                });
    }

    public Mono<Boolean> reportSpam(InputPeer peer) {
        return sendMain(telegram4j.tl.request.messages.ImmutableReportSpam.of(peer));
    }

    public Mono<PeerSettings> getPeerSettings(InputPeer peer) {
        return sendMain(ImmutableGetPeerSettings.of(peer));
    }

    public Mono<Boolean> report(InputPeer peer, Iterable<Integer> ids, ReportReason reason, String message) {
        return Mono.defer(() -> sendMain(ImmutableReport.of(peer, ids, reason, message)));
    }

    public Mono<DhConfig> getDhConfig(int version, int randomLength) {
        return sendMain(ImmutableGetDhConfig.of(version, randomLength));
    }

    public Mono<EncryptedChat> requestEncryption(InputUser user, int randomId, ByteBuf ga) {
        return Mono.defer(() -> sendMain(ImmutableRequestEncryption.of(user, randomId, ga)));
    }

    public Mono<EncryptedChat> acceptEncryption(InputEncryptedChat peer, ByteBuf gb, long fingerprint) {
        return Mono.defer(() -> sendMain(ImmutableAcceptEncryption.of(peer, gb, fingerprint)));
    }

    public Mono<Boolean> discardEncryption(boolean deleteEncryption, int chatId) {
        return sendMain(ImmutableDiscardEncryption.of(deleteEncryption
                ? ImmutableDiscardEncryption.DELETE_HISTORY_MASK : 0, chatId));
    }

    public Mono<Boolean> setEncryptedTyping(InputEncryptedChat peer, boolean typing) {
        return sendMain(ImmutableSetEncryptedTyping.of(peer, typing));
    }

    public Mono<Boolean> readEncryptedHistory(InputEncryptedChat peer, int maxDate) {
        return sendMain(ImmutableReadEncryptedHistory.of(peer, maxDate));
    }

    public Mono<SentEncryptedMessage> sendEncrypted(SendEncrypted request) {
        return sendMain(request);
    }

    public Mono<SentEncryptedMessage> sendEncryptedFile(SendEncryptedFile request) {
        return sendMain(request);
    }

    public Mono<List<Long>> receivedQueue(int maxQts) {
        return sendMain(ImmutableReceivedQueue.of(maxQts));
    }

    public Mono<AffectedMessages> readMessageContents(Iterable<Integer> ids) {
        return Mono.defer(() -> sendMain(telegram4j.tl.request.messages.ImmutableReadMessageContents.of(ids)));
    }

    public Mono<Stickers> getStickers(String emoticon, long hash) {
        return sendMain(ImmutableGetStickers.of(emoticon, hash));
    }

    public Mono<AllStickers> getAllStickers(long hash) {
        return sendMain(ImmutableGetAllStickers.of(hash));
    }

    public Mono<MessageMedia> getWebPagePreview(String message, @Nullable Iterable<? extends MessageEntity> entities) {
        return Mono.defer(() -> sendMain(ImmutableGetWebPagePreview.of(message)
                .withEntities(entities)));
    }

    public Mono<ExportedChatInvite> exportChatInvite(ExportChatInvite request) {
        return sendMain(request);
    }

    public Mono<ChatInvite> checkChatInvite(String hash) {
        return sendMain(ImmutableCheckChatInvite.of(hash));
    }

    public Mono<Updates> importChatInvite(String hash) {
        return sendMain(ImmutableImportChatInvite.of(hash));
    }

    @Compatible(Type.BOTH)
    public Mono<BaseStickerSet> getStickerSet(InputStickerSet stickerSet, int hash) {
        return sendMain(ImmutableGetStickerSet.of(stickerSet, hash))
                .ofType(BaseStickerSet.class);
    }

    public Mono<StickerSetInstallResult> installStickerSet(InputStickerSet stickerSet, boolean archived) {
        return sendMain(ImmutableInstallStickerSet.of(stickerSet, archived));
    }

    public Mono<Boolean> uninstallStickerSet(InputStickerSet stickerSet) {
        return sendMain(ImmutableUninstallStickerSet.of(stickerSet));
    }

    public Mono<Updates> startBot(InputUser bot, InputPeer peer, long randomId, String startParam) {
        return sendMain(ImmutableStartBot.of(bot, peer, randomId, startParam));
    }

    public Mono<MessageViews> getMessagesViews(InputPeer peer, Iterable<Integer> ids, boolean increment) {
        return Mono.defer(() -> sendMain(ImmutableGetMessagesViews.of(peer, ids, increment)));
    }

    public Mono<Boolean> editChatAdmin(long chatId, InputUser user, boolean isAdmin) {
        return sendMain(ImmutableEditChatAdmin.of(chatId, user, isAdmin));
    }

    public Mono<Updates> migrateChat(long chatId) {
        return sendMain(ImmutableMigrateChat.of(chatId));
    }

    public Mono<Messages> searchGlobal(SearchGlobal request) {
        return sendMain(request);
    }

    public Mono<Boolean> reorderStickerSet(ReorderStickerSets request) {
        return sendMain(request);
    }

    @Compatible(Type.BOTH)
    public Mono<Document> getDocumentByHash(ByteBuf sha256, int size, String mimeType) {
        return Mono.defer(() -> sendMain(ImmutableGetDocumentByHash.of(sha256, size, mimeType)));
    }

    public Mono<SavedGifs> getSavedGifs(long hash) {
        return sendMain(ImmutableGetSavedGifs.of(hash));
    }

    public Mono<Boolean> saveGif(FileReferenceId document, boolean unsave) {
        return Mono.defer(() -> sendMain(ImmutableSaveGif.of(document.asInputDocument(), unsave)));
    }

    public Mono<Boolean> saveGif(String documentFileReferenceId, boolean unsave) {
        return Mono.defer(() -> saveGif(FileReferenceId.deserialize(documentFileReferenceId), unsave));
    }

    public Mono<BotResults> getInlineBotResults(InputUser bot, InputPeer peer, @Nullable InputGeoPoint geoPoint,
                                                String query, String offset) {
        return sendMain(ImmutableGetInlineBotResults.of(bot, peer, query, offset)
                .withGeoPoint(geoPoint));
    }

    public Mono<Updates> sendInlineBotResult(SendInlineBotResult request) {
        return sendMain(request);
    }

    public Mono<Boolean> getMessageEditData(InputPeer peer, int id) {
        return sendMain(ImmutableGetMessageEditData.of(peer, id)).map(MessageEditData::caption);
    }

    @Compatible(Type.BOT)
    public Mono<Boolean> editInlineBotMessage(EditInlineBotMessage request) {
        return sendMain(request);
    }

    public Mono<BotCallbackAnswer> getBotCallbackAnswer(GetBotCallbackAnswer request) {
        return sendMain(request);
    }

    @Compatible(Type.BOT)
    public Mono<Boolean> setBotCallbackAnswer(SetBotCallbackAnswer request) {
        return sendMain(request);
    }

    public Mono<PeerDialogs> getPeerDialogs(Iterable<? extends InputDialogPeer> peers) {
        return Mono.defer(() -> sendMain(ImmutableGetPeerDialogs.of(peers)));
    }

    public Mono<Boolean> saveDraft(SaveDraft request) {
        return sendMain(request);
    }

    public Mono<Updates> getAllDrafts() {
        return sendMain(GetAllDrafts.instance());
    }

    public Mono<FeaturedStickers> getFeaturedStickers(long hash) {
        return sendMain(ImmutableGetFeaturedStickers.of(hash));
    }

    public Mono<Boolean> readFeaturedStickers(Iterable<Long> ids) {
        return sendMain(ReadFeaturedStickers.builder().id(ids).build());
    }

    public Mono<RecentStickers> getRecentStickers(boolean attached, long hash) {
        return sendMain(ImmutableGetRecentStickers.of(attached ?
                        ImmutableGetRecentStickers.ATTACHED_MASK : 0, hash));
    }

    public Mono<Boolean> saveRecentSticker(boolean attached, InputDocument document, boolean unsave) {
        return sendMain(ImmutableSaveRecentSticker.of(attached
                ? ImmutableSaveRecentSticker.ATTACHED_MASK : 0, document, unsave));
    }

    public Mono<Boolean> clearRecentStickers(boolean attached) {
        return sendMain(ImmutableClearRecentStickers.of(attached
                ? ImmutableClearRecentStickers.ATTACHED_MASK : 0));
    }

    public Mono<ArchivedStickers> getArchivedStickers(GetArchivedStickers request) {
        return sendMain(request);
    }

    public Mono<AllStickers> getMaskStickers(long hash) {
        return sendMain(ImmutableGetMaskStickers.of(hash));
    }

    public Mono<List<StickerSetCovered>> getAttachedStickers(InputStickeredMedia media) {
        return sendMain(ImmutableGetAttachedStickers.of(media));
    }

    public Mono<Updates> setGameScore(SetGameScore request) {
        return sendMain(request);
    }

    @Compatible(Type.BOT)
    public Mono<Boolean> setInlineGameScore(SetInlineGameScore request) {
        return sendMain(request);
    }

    @Compatible(Type.BOT)
    public Mono<HighScores> getGameHighScores(InputPeer peer, int id, InputUser user) {
        return sendMain(ImmutableGetGameHighScores.of(peer, id, user));
    }

    @Compatible(Type.BOT)
    public Mono<HighScores> getInlineGameHighScores(InputBotInlineMessageID id, InputUser user) {
        return sendMain(ImmutableGetInlineGameHighScores.of(id, user));
    }

    public Mono<Chats> getCommonChats(InputUser user, long maxId, int limit) {
        return sendMain(ImmutableGetCommonChats.of(user, maxId, limit));
    }

    public Mono<Chats> getAllChats(Iterable<Long> exceptIds) {
        return sendMain(GetAllChats.builder().exceptIds(exceptIds).build());
    }

    public Mono<WebPage> getWebPage(String url, int hash) {
        return sendMain(ImmutableGetWebPage.of(url, hash));
    }

    public Mono<Boolean> toggleDialogPin(boolean pinned, InputDialogPeer peer) {
        return sendMain(ImmutableToggleDialogPin.of(pinned
                ? ImmutableToggleDialogPin.PINNED_MASK : 0, peer));
    }

    public Mono<Boolean> reorderPinnedDialogs(boolean force, int folderId, Iterable<? extends InputDialogPeer> order) {
        return sendMain(ReorderPinnedDialogs.builder()
                .force(force)
                .folderId(folderId)
                .order(order)
                .build());
    }

    public Mono<PeerDialogs> getPinnedDialogs(int folderId) {
        return sendMain(ImmutableGetPinnedDialogs.of(folderId));
    }

    @Compatible(Type.BOT)
    public Mono<Boolean> setBotShippingResults(long queryId, @Nullable String error,
                                               @Nullable Iterable<? extends ShippingOption> shippingOptions) {
        return sendMain(SetBotShippingResults.builder()
                .queryId(queryId)
                .error(error)
                .shippingOptions(shippingOptions)
                .build());
    }

    @Compatible(Type.BOT)
    public Mono<Boolean> setBotPrecheckoutResults(boolean success, long queryId, @Nullable String error) {
        return sendMain(SetBotPrecheckoutResults.builder()
                .success(success)
                .queryId(queryId)
                .error(error)
                .build());
    }

    @Compatible(Type.BOT)
    public Mono<MessageMedia> uploadMedia(InputPeer peer, InputMedia media) {
        return sendMain(ImmutableUploadMedia.of(peer, media));
    }

    public Mono<Updates> sendScreenshotNotification(InputPeer peer, int replyToMsgId, long randomId) {
        return sendMain(ImmutableSendScreenshotNotification.of(peer, replyToMsgId, randomId));
    }

    public Mono<FavedStickers> getFavedStickers(long hash) {
        return sendMain(ImmutableGetFavedStickers.of(hash));
    }

    public Mono<Boolean> faveSticker(InputDocument document, boolean unfave) {
        return sendMain(ImmutableFaveSticker.of(document, unfave));
    }

    public Mono<Messages> getUnreadMentions(InputPeer peer, int offsetId, int addOffset, int limit, int maxId, int minId) {
        return sendMain(ImmutableGetUnreadMentions.of(peer, offsetId, addOffset, limit, maxId, minId));
    }

    public Mono<AffectedHistory> readMentions(InputPeer peer) {
        return sendMain(ImmutableReadMentions.of(peer));
    }

    public Mono<Messages> getRecentLocations(InputPeer peer, int limit, long hash) {
        return sendMain(ImmutableGetRecentLocations.of(peer, limit, hash));
    }

    @Compatible(Type.BOTH)
    public Mono<Updates> sendMultiMedia(SendMultiMedia request) {
        return sendMain(request);
    }

    public Mono<EncryptedFile> uploadEncryptedFile(InputEncryptedChat peer, InputEncryptedFile file) {
        return sendMain(ImmutableUploadEncryptedFile.of(peer, file));
    }

    public Mono<FoundStickerSets> searchStickerSets(boolean excludeFeatured, String query, long hash) {
        return sendMain(ImmutableSearchStickerSets.of(excludeFeatured
                ? ImmutableSearchStickerSets.EXCLUDE_FEATURED_MASK : 0, query, hash));
    }

    public Mono<List<MessageRange>> getSplitRanges() {
        return sendMain(GetSplitRanges.instance());
    }

    public Mono<Boolean> markDialogUnread(boolean unread, InputDialogPeer peer) {
        return sendMain(ImmutableMarkDialogUnread.of(unread ? ImmutableMarkDialogUnread.UNREAD_MASK : 0, peer));
    }

    public Mono<List<DialogPeer>> getDialogUnreadMarks() {
        return sendMain(GetDialogUnreadMarks.instance());
    }

    public Mono<Boolean> clearAllDrafts() {
        return sendMain(ClearAllDrafts.instance());
    }

    @Compatible(Type.BOTH)
    public Mono<Void> updatePinnedMessage(UpdatePinnedMessage request) {
        return sendMain(request)
                .flatMap(u -> Mono.fromRunnable(() -> groupManager.main().updates().emitNext(u, DEFAULT_PARKING)));
    }

    public Mono<Updates> sendVote(InputPeer peer, int msgId, Iterable<? extends ByteBuf> options) {
        return Mono.defer(() -> sendMain(ImmutableSendVote.builder()
                .peer(peer)
                .msgId(msgId)
                .options(options)
                .build()));
    }

    public Mono<Updates> getPollResults(InputPeer peer, int msgId) {
        return sendMain(ImmutableGetPollResults.of(peer, msgId));
    }

    public Mono<Integer> getOnlines(InputPeer peer) {
        return sendMain(ImmutableGetOnlines.of(peer)).map(ChatOnlines::onlines);
    }

    @Compatible(Type.BOTH)
    public Mono<Updates> editChatDefaultBannedRights(InputPeer peer, ChatBannedRights rights) {
        return sendMain(ImmutableEditChatDefaultBannedRights.of(peer, rights));
    }

    public Mono<EmojiKeywordsDifference> getEmojiKeywordsDifference(String langCode, int fromVersion) {
        return sendMain(ImmutableGetEmojiKeywordsDifference.of(langCode, fromVersion));
    }

    public Mono<List<EmojiLanguage>> getEmojiKeywordsLanguages(Iterable<String> langCodes) {
        return sendMain(GetEmojiKeywordsLanguages.builder().langCodes(langCodes).build());
    }

    public Mono<String> getEmojiUrl(String langCode) {
        return sendMain(ImmutableGetEmojiURL.of(langCode)).map(EmojiURL::url);
    }

    public Mono<List<SearchCounter>> getSearchCounters(InputPeer peer, Iterable<? extends MessagesFilter> filters) {
        return Mono.defer(() -> sendMain(ImmutableGetSearchCounters.of(peer, filters)));
    }

    public Mono<UrlAuthResult> requestUrlAuth(RequestUrlAuth request) {
        return sendMain(request);
    }

    public Mono<UrlAuthResult> acceptUrlAuth(AcceptUrlAuth request) {
        return sendMain(request);
    }

    public Mono<Boolean> hidePeerSettingsBar(InputPeer peer) {
        return sendMain(ImmutableHidePeerSettingsBar.of(peer));
    }

    public Mono<Messages> getScheduledHistory(InputPeer peer, long hash) {
        return sendMain(ImmutableGetScheduledHistory.of(peer, hash));
    }

    public Mono<Messages> getScheduledMessages(InputPeer peer, Iterable<Integer> ids) {
        return sendMain(GetScheduledMessages.builder()
                .peer(peer)
                .id(ids)
                .build());
    }

    public Mono<Updates> sendScheduledMessages(InputPeer peer, Iterable<Integer> ids) {
        return sendMain(SendScheduledMessages.builder()
                .peer(peer)
                .id(ids)
                .build());
    }

    public Mono<Updates> deleteScheduledMessages(InputPeer peer, Iterable<Integer> ids) {
        return sendMain(DeleteScheduledMessages.builder()
                .peer(peer)
                .id(ids)
                .build());
    }

    public Mono<VotesList> getPollVotes(InputPeer peer, int id, @Nullable ByteBuf option,
                                        @Nullable String offset, int limit) {
        return sendMain(GetPollVotes.builder()
                .peer(peer)
                .id(id)
                .option(option)
                .offset(offset)
                .limit(limit)
                .build());
    }

    public Mono<Boolean> toggleStickerSets(ToggleStickerSets request) {
        return sendMain(request);
    }

    public Mono<List<DialogFilter>> getDialogFilters() {
        return sendMain(GetDialogFilters.instance());
    }

    public Mono<List<DialogFilterSuggested>> getSuggestedDialogFilters() {
        return sendMain(GetSuggestedDialogFilters.instance());
    }

    public Mono<Boolean> updateDialogFilter(int id, @Nullable DialogFilter filter) {
        return sendMain(UpdateDialogFilter.builder()
                .id(id)
                .filter(filter)
                .build());
    }

    public Mono<Boolean> updateDialogFiltersOrder(Iterable<Integer> order) {
        return sendMain(UpdateDialogFiltersOrder.builder().order(order).build());
    }

    public Mono<FeaturedStickers> getOldFeaturedStickers(int offset, int limit, long hash) {
        return sendMain(ImmutableGetOldFeaturedStickers.of(offset, limit, hash));
    }

    public Mono<Messages> getReplies(InputPeer peer, int msgId, int offsetId, int offsetDate,
                                     int addOffset, int limit, int maxId, int minId, long hash) {
        return sendMain(ImmutableGetReplies.of(peer, msgId, offsetId, offsetDate,
                addOffset, limit, maxId, minId, hash));
    }

    public Mono<DiscussionMessage> getDiscussionMessage(InputPeer peer, int msgId) {
        return sendMain(ImmutableGetDiscussionMessage.of(peer, msgId));
    }

    public Mono<Boolean> readDiscussion(InputPeer peer, int msgId, int readMaxId) {
        return sendMain(ImmutableReadDiscussion.of(peer, msgId, readMaxId));
    }

    @Compatible(Type.BOTH)
    public Mono<AffectedHistory> unpinAllMessages(InputPeer peer, @Nullable Integer topMsgId) {
        return sendMain(ImmutableUnpinAllMessages.of(peer)
                .withTopMsgId(topMsgId));
    }

    public Mono<Boolean> deleteChat(long chatId) {
        return sendMain(ImmutableDeleteChat.of(chatId));
    }

    public Mono<AffectedFoundMessages> deletePhoneCallHistory(boolean revoked) {
        return sendMain(ImmutableDeletePhoneCallHistory.of(revoked
                ? ImmutableDeletePhoneCallHistory.REVOKE_MASK : 0));
    }

    public Mono<HistoryImportParsed> checkHistoryImport(String importHead) {
        return sendMain(ImmutableCheckHistoryImport.of(importHead));
    }

    public Mono<HistoryImport> initHistoryImport(InputPeer peer, InputFile file, int mediaCount) {
        return sendMain(ImmutableInitHistoryImport.of(peer, file, mediaCount));
    }

    public Mono<MessageMedia> uploadImportedMedia(InputPeer peer, long importId, String fileName, InputMedia media) {
        return sendMain(ImmutableUploadImportedMedia.of(peer, importId, fileName, media));
    }

    public Mono<Boolean> startHistoryImport(InputPeer peer, long importId) {
        return sendMain(ImmutableStartHistoryImport.of(peer, importId));
    }

    public Mono<ExportedChatInvites> getExportedChatInvites(GetExportedChatInvites request) {
        return sendMain(request);
    }

    public Mono<telegram4j.tl.messages.ExportedChatInvite> getExportedChatInvite(InputPeer peer, String link) {
        return sendMain(ImmutableGetExportedChatInvite.of(peer, link));
    }

    public Mono<telegram4j.tl.messages.ExportedChatInvite> editExportedChatInvite(EditExportedChatInvite request) {
        return sendMain(request);
    }

    public Mono<Boolean> deleteRevokedExportedChatInvites(InputPeer peer, InputUser admin) {
        return sendMain(ImmutableDeleteRevokedExportedChatInvites.of(peer, admin));
    }

    public Mono<Boolean> deleteExportedChatInvite(InputPeer peer, String link) {
        return sendMain(ImmutableDeleteExportedChatInvite.of(peer, link));
    }

    public Mono<ChatAdminsWithInvites> getAdminsWithInvites(InputPeer peer) {
        return sendMain(ImmutableGetAdminsWithInvites.of(peer));
    }

    public Mono<ChatInviteImporters> getChatInviteImporters(GetChatInviteImporters request) {
        return sendMain(request);
    }

    public Mono<Void> setHistoryTtl(InputPeer peer, int period) {
        return sendMain(ImmutableSetHistoryTTL.of(peer, period))
                // Typically: UpdatePeerHistoryTTL, UpdateNewMessage(service message)
                .flatMap(u -> Mono.fromRunnable(() -> groupManager.main().updates().emitNext(u, DEFAULT_PARKING)));
    }

    public Mono<CheckedHistoryImportPeer> checkHistoryImportPeer(InputPeer peer) {
        return sendMain(ImmutableCheckHistoryImportPeer.of(peer));
    }

    public Mono<Updates> setChatTheme(InputPeer peer, String emoticon) {
        return sendMain(ImmutableSetChatTheme.of(peer, emoticon));
    }

    public Mono<List<Long>> getMessageReadParticipants(InputPeer peer, int msgId) {
        return sendMain(ImmutableGetMessageReadParticipants.of(peer, msgId));
    }

    public Mono<Boolean> setInlineBotResults(SetInlineBotResults request) {
        return sendMain(request);
    }

    public Mono<Updates> getMessagesReactions(InputPeer peer, Iterable<Integer> messageIds) {
        return Mono.defer(() -> sendMain(ImmutableGetMessagesReactions.of(peer, messageIds)));
    }

    public Mono<MessageReactionsList> getMessageReactionsList(GetMessageReactionsList request) {
        return sendMain(request);
    }

    // Message interactions

    @Compatible(Type.BOTH)
    public Mono<BaseMessageFields> sendMessage(SendMessage request) {
        return sendMain(request)
                .flatMap(u -> transformMessageUpdate(request, u))
                .map(updates -> {
                    groupManager.main().updates().emitNext(updates.getT2(), DEFAULT_PARKING);

                    return updates.getT1();
                });
    }

    @Compatible(Type.BOTH)
    public Mono<BaseMessageFields> sendMedia(SendMedia request) {
        return sendMain(request)
                .flatMap(u -> transformMessageUpdate(request, u))
                .flatMap(tuple -> {
                    Mono<Void> pollSaving = Mono.empty();
                    var media = request.media();
                    if (media instanceof InputMediaPoll) {
                        var message = (BaseMessage) tuple.getT1();
                        var messageMediaPoll = (MessageMediaPoll) Objects.requireNonNull(message.media());
                        pollSaving = storeLayout.registerPoll(message.peerId(), message.id(),
                                ImmutableInputMediaPoll.copyOf((InputMediaPoll) media)
                                        .withPoll(ImmutablePoll.copyOf(messageMediaPoll.poll())));
                    }

                    return pollSaving.thenReturn(tuple);
                })
                .map(updates -> {
                    groupManager.main().updates().emitNext(updates.getT2(), DEFAULT_PARKING);

                    return updates.getT1();
                });
    }

    @Compatible(Type.BOTH)
    public Mono<BaseMessageFields> editMessage(EditMessage request) {
        return sendMain(request)
                .flatMap(updates -> {
                    if (updates.identifier() == BaseUpdates.ID) {
                        BaseUpdates casted = (BaseUpdates) updates;

                        // also can receive UpdateMessagePoll
                        UpdateEditMessageFields newMessage = null;
                        for (Update update : casted.updates()) {
                            if (update instanceof UpdateEditMessageFields) {
                                newMessage = (UpdateEditMessageFields) update;
                            }
                        }

                        groupManager.main().updates().emitNext(updates, DEFAULT_PARKING);
                        return Mono.justOrEmpty(newMessage)
                                .map(UpdateEditMessageFields::message)
                                .cast(BaseMessageFields.class);
                    }
                    return Mono.error(new IllegalArgumentException("Unknown updates type: " + updates));
                });
    }

    /**
     * Retrieve minimal chats by their ids.
     * This method can return container which contains only {@link BaseChat},
     * {@link ChatForbidden} or {@link ChatEmpty} objects.
     *
     * @param ids An iterable of chat id elements
     * @return A {@link Mono} emitting on successful completion a list of
     * minimal chats or slice of list if there are a lot of chats
     */
    @Compatible(Type.BOTH)
    public Mono<Chats> getChats(Iterable<Long> ids) {
        return Mono.defer(() -> sendMain(ImmutableGetChats.of(ids)))
                .flatMap(c -> storeLayout.onContacts(c.chats(), List.of())
                        .thenReturn(c));
    }

    /**
     * Retrieve detailed information about chat by their id and update cache.
     *
     * @param chatId The id of the chat.
     * @return A {@link Mono} emitting on successful completion an object contains
     * detailed info about chat and auxiliary data
     */
    @Compatible(Type.BOTH)
    public Mono<ChatFull> getFullChat(long chatId) {
        return sendMain(ImmutableGetFullChat.of(chatId))
                .flatMap(c -> storeLayout.onChatUpdate(c).thenReturn(c));
    }

    @Compatible(Type.BOTH)
    public Mono<Void> editChatTitle(long chatId, String title) {
        return sendMain(ImmutableEditChatTitle.of(chatId, title))
                .flatMap(u -> Mono.fromRunnable(() -> groupManager.main().updates().emitNext(u, DEFAULT_PARKING)));
    }

    @Compatible(Type.BOTH)
    public Mono<Void> editChatPhoto(long chatId, InputChatPhoto photo) {
        return sendMain(ImmutableEditChatPhoto.of(chatId, photo))
                .flatMap(u -> Mono.fromRunnable(() -> groupManager.main().updates().emitNext(u, DEFAULT_PARKING)));
    }

    public Mono<Updates> addChatUser(long chatId, InputUser user, int forwardLimit) {
        return sendMain(ImmutableAddChatUser.of(chatId, user, forwardLimit));
    }

    @Compatible(Type.BOTH)
    public Mono<Void> deleteChatUser(long chatId, InputUser userId, boolean revokeHistory) {
        return sendMain(ImmutableDeleteChatUser.builder().revokeHistory(revokeHistory).chatId(chatId).userId(userId).build())
                .flatMap(u -> Mono.fromRunnable(() -> groupManager.main().updates().emitNext(u, DEFAULT_PARKING)));
    }

    public Mono<Updates> createChat(Iterable<? extends InputUser> users, String title) {
        return Mono.defer(() -> sendMain(ImmutableCreateChat.of(users, title)));
    }

    // folders namespace
    // =========================

    public Mono<Updates> editPeerFolders(Iterable<? extends InputFolderPeer> peers) {
        return Mono.defer(() -> sendMain(ImmutableEditPeerFolders.of(peers)));
    }

    public Mono<Updates> deleteFolder(int folderId) {
        return sendMain(ImmutableDeleteFolder.of(folderId));
    }

    // channels namespace
    // =========================

    public Mono<SendAsPeers> getSendAs(InputPeer peer) {
        return sendMain(ImmutableGetSendAs.of(peer));
    }

    @Compatible(Type.BOTH)
    public Mono<Boolean> editChatAbout(InputPeer peer, String about) {
        return sendMain(ImmutableEditChatAbout.of(peer, about));
    }

    @Compatible(Type.BOTH)
    public Mono<AffectedMessages> deleteMessages(InputChannel channel, Iterable<Integer> ids) {
        return Mono.defer(() -> sendMain(ImmutableDeleteMessages.of(channel, ids)));
    }

    @Compatible(Type.BOTH)
    public Mono<Messages> getMessages(InputChannel channel, Iterable<? extends InputMessage> ids) {
        return Mono.defer(() -> sendMain(ImmutableGetMessages.of(channel, ids)))
                .flatMap(m -> storeLayout.onMessages(m).thenReturn(m));
    }

    @Compatible(Type.BOTH)
    public Mono<ChannelParticipants> getParticipants(InputChannel channel, ChannelParticipantsFilter filter,
                                                     int offset, int limit, Iterable<Long> ids) {
        return Mono.defer(() -> getParticipants(channel, filter, offset, limit, calculatePaginationHash(ids)));
    }

    @Compatible(Type.BOTH)
    public Mono<BaseChannelParticipants> getParticipants(InputChannel channel, ChannelParticipantsFilter filter,
                                                         int offset, int limit, long hash) {
        return sendMain(ImmutableGetParticipants.of(channel, filter, offset, limit, hash))
                .ofType(BaseChannelParticipants.class)
                .flatMap(p -> storeLayout.onChannelParticipants(TlEntityUtil.getRawPeerId(channel), p).thenReturn(p));
    }

    @Compatible(Type.BOTH)
    public Mono<ChannelParticipant> getParticipant(InputChannel channel, InputPeer peer) {
        return sendMain(ImmutableGetParticipant.of(channel, peer))
                .flatMap(p -> storeLayout.onChannelParticipant(TlEntityUtil.getRawPeerId(channel), p).thenReturn(p));
    }

    /**
     * Retrieve minimal channels by their ids.
     * This method can return container which contains only {@link Channel} or {@link ChannelForbidden} objects.
     *
     * @param ids An iterable of channel id elements
     * @return A {@link Mono} emitting on successful completion a list of
     * minimal channels or slice of list if there are a lot of channels
     */
    @Compatible(Type.BOTH)
    public Mono<Chats> getChannels(Iterable<? extends InputChannel> ids) {
        return Mono.defer(() -> sendMain(ImmutableGetChannels.of(ids)))
                .flatMap(c -> storeLayout.onContacts(c.chats(), List.of())
                        .thenReturn(c));
    }

    /**
     * Retrieve detailed channel by given id and update cache.
     *
     * @param id The id of channel
     * @return A {@link Mono} emitting on successful completion detailed information about channel
     */
    @Compatible(Type.BOTH)
    public Mono<ChatFull> getFullChannel(InputChannel id) {
        return sendMain(ImmutableGetFullChannel.of(id))
                .flatMap(c -> storeLayout.onChatUpdate(c).thenReturn(c));
    }

    @Compatible(Type.BOTH)
    public Mono<Chat> editAdmin(InputChannel channel, InputUser user, ChatAdminRights rights, String rank) {
        return sendMain(ImmutableEditAdmin.of(channel, user, rights, rank))
                .cast(BaseUpdates.class)
                .flatMap(u -> {
                    groupManager.main().updates().emitNext(u, DEFAULT_PARKING);
                    return Mono.justOrEmpty(u.chats().get(0));
                });
    }

    @Compatible(Type.BOTH)
    public Mono<Void> editTitle(InputChannel channel, String title) {
        return sendMain(ImmutableEditTitle.of(channel, title))
                .flatMap(u -> Mono.fromRunnable(() -> groupManager.main().updates().emitNext(u, DEFAULT_PARKING)));
    }

    @Compatible(Type.BOTH)
    public Mono<Chat> editBanned(InputChannel channel, InputPeer participant, ChatBannedRights rights) {
        return sendMain(ImmutableEditBanned.of(channel, participant, rights))
                .cast(BaseUpdates.class)
                .flatMap(u -> {
                    groupManager.main().updates().emitNext(u, DEFAULT_PARKING);
                    return Mono.justOrEmpty(u.chats().get(0));
                });
    }

    @Compatible(Type.BOTH)
    public Mono<Void> editPhoto(InputChannel channel, InputChatPhoto photo) {
        return sendMain(ImmutableEditPhoto.of(channel, photo))
                .flatMap(u -> Mono.fromRunnable(() -> groupManager.main().updates().emitNext(u, DEFAULT_PARKING)));
    }

    @Compatible(Type.BOTH)
    public Mono<Void> leaveChannel(InputChannel channel) {
        return sendMain(ImmutableLeaveChannel.of(channel))
                .flatMap(u -> Mono.fromRunnable(() -> groupManager.main().updates().emitNext(u, DEFAULT_PARKING)));
    }

    @Compatible(Type.BOTH)
    public Mono<Boolean> setStickers(InputChannel channel, InputStickerSet stickerSet) {
        return sendMain(ImmutableSetStickers.of(channel, stickerSet));
    }

    public Mono<Updates> toggleJoinToSend(InputChannel channel, boolean enabled) {
        return sendMain(ImmutableToggleJoinToSend.of(channel, enabled));
    }

    public Mono<Updates> toggleJoinRequest(InputChannel channel, boolean enabled) {
        return sendMain(ImmutableToggleJoinRequest.of(channel, enabled));
    }

    @Compatible(Type.BOTH)
    public Mono<AffectedHistory> deleteParticipantHistory(InputChannel channel, InputPeer participant) {
        return sendMain(ImmutableDeleteParticipantHistory.of(channel, participant));
    }

    public Mono<Boolean> readHistory(InputChannel channel, int maxId) {
        return sendMain(ImmutableReadHistory.of(channel, maxId));
    }

    public Mono<Boolean> reportSpam(InputChannel channel, InputPeer participant, Iterable<Integer> ids) {
        return Mono.defer(() -> sendMain(ImmutableReportSpam.of(channel, participant, ids)));
    }

    public Mono<Updates> createChannel(CreateChannel request) {
        return sendMain(request);
    }

    /**
     * Check if a username is free and can be assigned to a channel/supergroup.
     *
     * @param channel the channel/supergroup that will assign the specified username
     * @param username the username to check
     * @return A {@link Mono} emitting on successful completion {@code true}
     */
    public Mono<Boolean> checkUsername(InputChannel channel, String username) {
        return sendMain(ImmutableCheckUsername.of(channel, username));
    }

    /**
     * Change the username of a supergroup/channel.
     *
     * @param channel the channel/supergroup that will assign the specified username
     * @param username the username to update
     * @return A {@link Mono} emitting on successful completion {@code true}
     */
    public Mono<Boolean> updateUsername(InputChannel channel, String username) {
        return sendMain(ImmutableUpdateUsername.of(channel, username));
    }

    public Mono<Updates> joinChannel(InputChannel channel) {
        return sendMain(ImmutableJoinChannel.of(channel));
    }

    public Mono<Updates> inviteToChannel(InputChannel channel, Iterable<? extends InputUser> ids) {
        return Mono.defer(() -> sendMain(ImmutableInviteToChannel.of(channel, ids)));
    }

    public Mono<Updates> deleteChannel(InputChannel channel) {
        return sendMain(ImmutableDeleteChannel.of(channel));
    }

    public Mono<ExportedMessageLink> exportMessageLink(ExportMessageLink request) {
        return sendMain(request);
    }

    public Mono<Updates> toggleSignatures(InputChannel channel, boolean enabled) {
        return sendMain(ImmutableToggleSignatures.of(channel, enabled));
    }

    public Mono<Chats> getAdminedPublicChannels(GetAdminedPublicChannels request) {
        return sendMain(request);
    }

    public Mono<AdminLogResults> getAdminLog(GetAdminLog request) {
        return sendMain(request);
    }

    public Mono<Boolean> readMessageContents(InputChannel channel, Iterable<Integer> ids) {
        return Mono.defer(() -> sendMain(ImmutableReadMessageContents.of(channel, ids)));
    }

    public Mono<Updates> deleteHistory(InputChannel channel, int maxId, boolean forEveryone) {
        return sendMain(ImmutableDeleteHistory.of(forEveryone
                ? ImmutableDeleteHistory.FOR_EVERYONE_MASK : 0, channel, maxId));
    }

    public Mono<Updates> togglePreHistoryHidden(InputChannel channel, boolean enabled) {
        return sendMain(ImmutableTogglePreHistoryHidden.of(channel, enabled));
    }

    public Mono<Chats> getLeftChannels(int offset) {
        return sendMain(ImmutableGetLeftChannels.of(offset));
    }

    public Mono<Chats> getGroupsForDiscussion() {
        return sendMain(GetGroupsForDiscussion.instance());
    }

    public Mono<Boolean> setDiscussionGroup(InputChannel broadcast, InputChannel group) {
        return sendMain(ImmutableSetDiscussionGroup.of(broadcast, group));
    }

    public Mono<Updates> editCreator(InputChannel channel, InputUser user, InputCheckPasswordSRP password) {
        return sendMain(ImmutableEditCreator.of(channel, user, password));
    }

    public Mono<Boolean> editLocation(InputChannel channel, InputGeoPoint geoPint, String address) {
        return sendMain(ImmutableEditLocation.of(channel, geoPint, address));
    }

    public Mono<Updates> toggleSlowMode(InputChannel channel, int seconds) {
        return sendMain(ImmutableToggleSlowMode.of(channel, seconds));
    }

    public Mono<InactiveChats> getInactiveChats() {
        return sendMain(GetInactiveChannels.instance());
    }

    public Mono<Updates> convertToGigagroup(InputChannel channel) {
        return sendMain(ImmutableConvertToGigagroup.of(channel));
    }

    public Mono<Boolean> viewSponsoredMessage(InputChannel channel, ByteBuf randomId) {
        return Mono.defer(() -> sendMain(ImmutableViewSponsoredMessage.of(channel, randomId)));
    }

    public Mono<SponsoredMessages> getSponsoredMessages(InputChannel channel) {
        return sendMain(ImmutableGetSponsoredMessages.of(channel));
    }

    // Short-send related updates object should be transformed to the updateShort or baseUpdates.
    // https://core.telegram.org/api/updates#updates-sequence

    private Mono<Tuple2<BaseMessageFields, Updates>> transformMessageUpdate(BaseSendMessageRequest request, Updates updates) {
        return toPeer(request.peer())
                .zipWith(Mono.justOrEmpty(request.sendAs())
                        .defaultIfEmpty(InputPeerSelf.instance())
                        .flatMap(this::toPeer))
                .map(TupleUtils.function((chat, author) -> {

                    switch (updates.identifier()) {
                        case UpdateShortSentMessage.ID: {
                            UpdateShortSentMessage casted = (UpdateShortSentMessage) updates;
                            Integer replyToMsgId = request.replyToMsgId();
                            var message = BaseMessage.builder()
                                    .flags(request.flags() | casted.flags())
                                    .peerId(chat)
                                    .fromId(author)
                                    .replyTo(replyToMsgId != null ? ImmutableMessageReplyHeader.of(0, replyToMsgId) : null)
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
                                    .peerId(chat)
                                    .fromId(author)
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
                                    .peerId(chat)
                                    .fromId(author)
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
                                    .findFirst()
                                    .map(upd -> (UpdateMessageID) upd)
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
                        default: throw new IllegalArgumentException("Unknown Updates type: " + updates);
                    }
                }));
    }
}
