package telegram4j.mtproto.service;

import io.netty.buffer.ByteBuf;
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
import telegram4j.tl.channels.AdminLogResults;
import telegram4j.tl.channels.ChannelParticipant;
import telegram4j.tl.channels.ChannelParticipants;
import telegram4j.tl.channels.SendAsPeers;
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

import static telegram4j.mtproto.util.EmissionHandlers.DEFAULT_PARKING;

/** Rpc service with chat and channel related methods. */
public class ChatService extends RpcService {

    public ChatService(MTProtoClient client, StoreLayout storeLayout) {
        super(client, storeLayout);
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
    @BotCompatible
    public Mono<Chat> getChat(long id) {
        return getChats(List.of(id))
                .cast(BaseChats.class)
                .map(c -> c.chats().get(0));
    }

    /**
     * Retrieve minimal channel by given id.
     * This method can return only {@link Channel} or {@link ChannelForbidden} objects.
     *
     * @param id The id of channel
     * @return A {@link Mono} emitting on successful completion minimal information about channel
     */
    @BotCompatible
    public Mono<Chat> getChannel(InputChannel id) {
        return getChannels(List.of(id))
                .cast(BaseChats.class)
                .map(c -> c.chats().get(0));
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
    // translateText#24ce6dee flags:# peer:flags.0?InputPeer msg_id:flags.0?int text:flags.1?string from_lang:flags.2?string to_lang:string = messages.TranslatedText;
    // getUnreadReactions#e85bae1a peer:InputPeer offset_id:int add_offset:int limit:int max_id:int min_id:int = messages.Messages;
    // readReactions#82e251d7 peer:InputPeer = messages.AffectedHistory;
    // searchSentMedia#107e31a0 q:string filter:MessagesFilter limit:int = messages.Messages;
    // getAttachMenuBots#16fcc2cb hash:long = AttachMenuBots;
    // getAttachMenuBot#77216192 bot:InputUser = AttachMenuBotsBot;
    // toggleBotInAttachMenu#1aee33af bot:InputUser enabled:Bool = Bool;
    // requestWebView#91b15831 flags:# from_bot_menu:flags.4?true silent:flags.5?true peer:InputPeer bot:InputUser url:flags.1?string start_param:flags.3?string theme_params:flags.2?DataJSON reply_to_msg_id:flags.0?int send_as:flags.13?InputPeer = WebViewResult;
    // prolongWebView#ea5fbcce flags:# silent:flags.5?true peer:InputPeer bot:InputUser query_id:long reply_to_msg_id:flags.0?int send_as:flags.13?InputPeer = Bool;

    @BotCompatible
    public Mono<Messages> getMessages(Iterable<? extends InputMessage> ids) {
        return Mono.defer(() -> client.sendAwait(telegram4j.tl.request.messages.ImmutableGetMessages.of(ids)));
    }

    public Mono<SentEncryptedMessage> sendEncryptedService(InputEncryptedChat peer, long randomId, ByteBuf data) {
        return Mono.defer(() -> client.sendAwait(ImmutableSendEncryptedService.of(peer, randomId, data)));
    }

    public Mono<SimpleWebViewResult> requestSimpleWebView(InputUser bot, String url, String platform,
                                                          @Nullable String themeParamsJson) {
        return client.sendAwait(ImmutableRequestSimpleWebView.of(bot, url, platform)
                .withThemeParams(themeParamsJson != null ? ImmutableDataJSON.of(themeParamsJson) : null));
    }

    public Mono<WebViewMessageSent> sendWebViewResultMessage(String botQueryId, InputBotInlineResult result) {
        return client.sendAwait(ImmutableSendWebViewResultMessage.of(botQueryId, result));
    }

    public Mono<Updates> sendWebViewData(InputUser bot, long randomId, String buttonText, String data) {
        return client.sendAwait(ImmutableSendWebViewData.of(bot, randomId, buttonText, data));
    }

    public Mono<TranscribedAudio> transcribeAudio(InputPeer peer, int messageId) {
        return client.sendAwait(ImmutableTranscribeAudio.of(peer, messageId));
    }

    public Mono<Boolean> rateTranscribedAudio(InputPeer peer, int messageId, long transcriptionId, boolean good) {
        return client.sendAwait(ImmutableRateTranscribedAudio.of(peer, messageId, transcriptionId, good));
    }

    public Mono<List<Document>> getCustomEmojiDocuments(Iterable<Long> documentIds) {
        return Mono.defer(() -> client.sendAwait(ImmutableGetCustomEmojiDocuments.of(documentIds)));
    }

    public Mono<AllStickers> getEmojiStickers(long hash) {
        return client.sendAwait(ImmutableGetEmojiStickers.of(hash));
    }

    public Mono<FeaturedStickers> getFeaturedEmojiStickers(long hash) {
        return client.sendAwait(ImmutableGetFeaturedStickers.of(hash));
    }

    public Mono<Dialogs> getDialogs(GetDialogs request) {
        return client.sendAwait(request);
    }

    public Mono<Messages> getHistory(GetHistory request) {
        return client.sendAwait(request);
    }

    @BotCompatible
    public Mono<AffectedMessages> deleteMessages(boolean revoke, Iterable<Integer> ids) {
        return client.sendAwait(DeleteMessages.builder().revoke(revoke).id(ids).build());
    }

    public Mono<Messages> search(Search request) {
        return client.sendAwait(request);
    }

    public Mono<AffectedMessages> readHistory(InputPeer peer, int maxId) {
        return client.sendAwait(telegram4j.tl.request.messages.ImmutableReadHistory.of(peer, maxId));
    }

    public Mono<AffectedHistory> deleteHistory(DeleteHistory request) {
        return client.sendAwait(request);
    }

    public Mono<List<ReceivedNotifyMessage>> receivedMessages(int maxId) {
        return client.sendAwait(ImmutableReceivedMessages.of(maxId));
    }

    @BotCompatible
    public Mono<Boolean> setTyping(InputPeer peer, @Nullable Integer topMsgId, SendMessageAction action) {
        return client.sendAwait(ImmutableSetTyping.of(peer, action)
                .withTopMsgId(topMsgId));
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
        return client.sendAwait(telegram4j.tl.request.messages.ImmutableReportSpam.of(peer));
    }

    public Mono<PeerSettings> getPeerSettings(InputPeer peer) {
        return client.sendAwait(ImmutableGetPeerSettings.of(peer));
    }

    public Mono<Boolean> report(InputPeer peer, Iterable<Integer> ids, ReportReason reason, String message) {
        return Mono.defer(() -> client.sendAwait(ImmutableReport.of(peer, ids, reason, message)));
    }

    public Mono<DhConfig> getDhConfig(int version, int randomLength) {
        return client.sendAwait(ImmutableGetDhConfig.of(version, randomLength));
    }

    public Mono<EncryptedChat> requestEncryption(InputUser user, int randomId, ByteBuf ga) {
        return Mono.defer(() -> client.sendAwait(ImmutableRequestEncryption.of(user, randomId, ga)));
    }

    public Mono<EncryptedChat> acceptEncryption(InputEncryptedChat peer, ByteBuf gb, long fingerprint) {
        return Mono.defer(() -> client.sendAwait(ImmutableAcceptEncryption.of(peer, gb, fingerprint)));
    }

    public Mono<Boolean> discardEncryption(boolean deleteEncryption, int chatId) {
        return client.sendAwait(ImmutableDiscardEncryption.of(deleteEncryption
                ? ImmutableDiscardEncryption.DELETE_HISTORY_MASK : 0, chatId));
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

    public Mono<List<Long>> receivedQueue(int maxQts) {
        return client.sendAwait(ImmutableReceivedQueue.of(maxQts));
    }

    public Mono<AffectedMessages> readMessageContents(Iterable<Integer> ids) {
        return Mono.defer(() -> client.sendAwait(telegram4j.tl.request.messages.ImmutableReadMessageContents.of(ids)));
    }

    public Mono<Stickers> getStickers(String emoticon, long hash) {
        return client.sendAwait(ImmutableGetStickers.of(emoticon, hash));
    }

    public Mono<AllStickers> getAllStickers(long hash) {
        return client.sendAwait(ImmutableGetAllStickers.of(hash));
    }

    public Mono<MessageMedia> getWebPagePreview(String message, @Nullable Iterable<? extends MessageEntity> entities) {
        return Mono.defer(() -> client.sendAwait(ImmutableGetWebPagePreview.of(message)
                .withEntities(entities)));
    }

    public Mono<ExportedChatInvite> exportChatInvite(ExportChatInvite request) {
        return client.sendAwait(request);
    }

    public Mono<ChatInvite> checkChatInvite(String hash) {
        return client.sendAwait(ImmutableCheckChatInvite.of(hash));
    }

    public Mono<Updates> importChatInvite(String hash) {
        return client.sendAwait(ImmutableImportChatInvite.of(hash));
    }

    @BotCompatible
    public Mono<telegram4j.tl.messages.StickerSet> getStickerSet(InputStickerSet stickerSet, int hash) {
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
        return Mono.defer(() -> client.sendAwait(ImmutableGetMessagesViews.of(peer, ids, increment)));
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

    public Mono<Boolean> reorderStickerSet(ReorderStickerSets request) {
        return client.sendAwait(request);
    }

    @BotCompatible
    public Mono<Document> getDocumentByHash(ByteBuf sha256, int size, String mimeType) {
        return Mono.defer(() -> client.sendAwait(ImmutableGetDocumentByHash.of(sha256, size, mimeType)));
    }

    public Mono<SavedGifs> getSavedGifs(long hash) {
        return client.sendAwait(ImmutableGetSavedGifs.of(hash));
    }

    public Mono<Boolean> saveGif(FileReferenceId document, boolean unsave) {
        return Mono.defer(() -> client.sendAwait(ImmutableSaveGif.of(document.asInputDocument(), unsave)));
    }

    public Mono<Boolean> saveGif(String documentFileReferenceId, boolean unsave) {
        return Mono.defer(() -> saveGif(FileReferenceId.deserialize(documentFileReferenceId), unsave));
    }

    public Mono<BotResults> getInlineBotResults(InputUser bot, InputPeer peer, @Nullable InputGeoPoint geoPoint,
                                                String query, String offset) {
        return client.sendAwait(ImmutableGetInlineBotResults.of(bot, peer, query, offset)
                .withGeoPoint(geoPoint));
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
        return client.sendAwait(ImmutableGetRecentStickers.of(attached ?
                        ImmutableGetRecentStickers.ATTACHED_MASK : 0, hash));
    }

    public Mono<Boolean> saveRecentSticker(boolean attached, InputDocument document, boolean unsave) {
        return client.sendAwait(ImmutableSaveRecentSticker.of(attached
                ? ImmutableSaveRecentSticker.ATTACHED_MASK : 0, document, unsave));
    }

    public Mono<Boolean> clearRecentStickers(boolean attached) {
        return client.sendAwait(ImmutableClearRecentStickers.of(attached
                ? ImmutableClearRecentStickers.ATTACHED_MASK : 0));
    }

    public Mono<ArchivedStickers> getArchivedStickers(GetArchivedStickers request) {
        return client.sendAwait(request);
    }

    public Mono<AllStickers> getMaskStickers(long hash) {
        return client.sendAwait(ImmutableGetMaskStickers.of(hash));
    }

    public Mono<List<StickerSetCovered>> getAttachedStickers(InputStickeredMedia media) {
        return client.sendAwait(ImmutableGetAttachedStickers.of(media));
    }

    public Mono<Updates> setGameScore(SetGameScore request) {
        return client.sendAwait(request);
    }

    @BotCompatible
    public Mono<Boolean> setInlineGameScore(SetInlineGameScore request) {
        return client.sendAwait(request);
    }

    @BotCompatible
    public Mono<HighScores> getGameHighScores(InputPeer peer, int id, InputUser user) {
        return client.sendAwait(ImmutableGetGameHighScores.of(peer, id, user));
    }

    @BotCompatible
    public Mono<HighScores> getInlineGameHighScores(InputBotInlineMessageID id, InputUser user) {
        return client.sendAwait(ImmutableGetInlineGameHighScores.of(id, user));
    }

    public Mono<Chats> getCommonChats(InputUser user, long maxId, int limit) {
        return client.sendAwait(ImmutableGetCommonChats.of(user, maxId, limit));
    }

    public Mono<Chats> getAllChats(Iterable<Long> exceptIds) {
        return client.sendAwait(GetAllChats.builder().exceptIds(exceptIds).build());
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

    public Mono<Boolean> faveSticker(InputDocument document, boolean unfave) {
        return client.sendAwait(ImmutableFaveSticker.of(document, unfave));
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
        return client.sendAwait(ImmutableSearchStickerSets.of(excludeFeatured
                ? ImmutableSearchStickerSets.EXCLUDE_FEATURED_MASK : 0, query, hash));
    }

    public Mono<List<MessageRange>> getSplitRanges() {
        return client.sendAwait(GetSplitRanges.instance());
    }

    public Mono<Boolean> markDialogUnread(boolean unread, InputDialogPeer peer) {
        return client.sendAwait(ImmutableMarkDialogUnread.of(unread ? ImmutableMarkDialogUnread.UNREAD_MASK : 0, peer));
    }

    public Mono<List<DialogPeer>> getDialogUnreadMarks() {
        return client.sendAwait(GetDialogUnreadMarks.instance());
    }

    public Mono<Boolean> clearAllDrafts() {
        return client.sendAwait(ClearAllDrafts.instance());
    }

    @BotCompatible
    public Mono<Void> updatePinnedMessage(UpdatePinnedMessage request) {
        return client.sendAwait(request)
                .flatMap(u -> Mono.fromRunnable(() -> client.updates().emitNext(u, DEFAULT_PARKING)));
    }

    public Mono<Updates> sendVote(InputPeer peer, int msgId, Iterable<? extends ByteBuf> options) {
        return Mono.defer(() -> client.sendAwait(ImmutableSendVote.builder()
                .peer(peer)
                .msgId(msgId)
                .options(options)
                .build()));
    }

    public Mono<Updates> getPollResults(InputPeer peer, int msgId) {
        return client.sendAwait(ImmutableGetPollResults.of(peer, msgId));
    }

    public Mono<Integer> getOnlines(InputPeer peer) {
        return client.sendAwait(ImmutableGetOnlines.of(peer)).map(ChatOnlines::onlines);
    }

    @BotCompatible
    public Mono<Updates> editChatDefaultBannedRights(InputPeer peer, ChatBannedRights rights) {
        return client.sendAwait(ImmutableEditChatDefaultBannedRights.of(peer, rights));
    }

    public Mono<EmojiKeywordsDifference> getEmojiKeywordsDifference(String langCode, int fromVersion) {
        return client.sendAwait(ImmutableGetEmojiKeywordsDifference.of(langCode, fromVersion));
    }

    public Mono<List<EmojiLanguage>> getEmojiKeywordsLanguages(Iterable<String> langCodes) {
        return client.sendAwait(GetEmojiKeywordsLanguages.builder().langCodes(langCodes).build());
    }

    public Mono<String> getEmojiUrl(String langCode) {
        return client.sendAwait(ImmutableGetEmojiURL.of(langCode)).map(EmojiURL::url);
    }

    public Mono<List<SearchCounter>> getSearchCounters(InputPeer peer, Iterable<? extends MessagesFilter> filters) {
        return Mono.defer(() -> client.sendAwait(ImmutableGetSearchCounters.of(peer, filters)));
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

    public Mono<VotesList> getPollVotes(InputPeer peer, int id, @Nullable ByteBuf option,
                                        @Nullable String offset, int limit) {
        return client.sendAwait(GetPollVotes.builder()
                .peer(peer)
                .id(id)
                .option(option)
                .offset(offset)
                .limit(limit)
                .build());
    }

    public Mono<Boolean> toggleStickerSets(ToggleStickerSets request) {
        return client.sendAwait(request);
    }

    public Mono<List<DialogFilter>> getDialogFilters() {
        return client.sendAwait(GetDialogFilters.instance());
    }

    public Mono<List<DialogFilterSuggested>> getSuggestedDialogFilters() {
        return client.sendAwait(GetSuggestedDialogFilters.instance());
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

    @BotCompatible
    public Mono<AffectedHistory> unpinAllMessages(InputPeer peer) {
        return client.sendAwait(ImmutableUnpinAllMessages.of(peer));
    }

    public Mono<Boolean> deleteChat(long chatId) {
        return client.sendAwait(ImmutableDeleteChat.of(chatId));
    }

    public Mono<AffectedFoundMessages> deletePhoneCallHistory(boolean revoked) {
        return client.sendAwait(ImmutableDeletePhoneCallHistory.of(revoked
                ? ImmutableDeletePhoneCallHistory.REVOKE_MASK : 0));
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
                // Typically: UpdatePeerHistoryTTL, UpdateNewMessage(service message)
                .flatMap(u -> Mono.fromRunnable(() -> client.updates().emitNext(u, DEFAULT_PARKING)));
    }

    public Mono<CheckedHistoryImportPeer> checkHistoryImportPeer(InputPeer peer) {
        return client.sendAwait(ImmutableCheckHistoryImportPeer.of(peer));
    }

    public Mono<Updates> setChatTheme(InputPeer peer, String emoticon) {
        return client.sendAwait(ImmutableSetChatTheme.of(peer, emoticon));
    }

    public Mono<List<Long>> getMessageReadParticipants(InputPeer peer, int msgId) {
        return client.sendAwait(ImmutableGetMessageReadParticipants.of(peer, msgId));
    }

    public Mono<Boolean> setInlineBotResults(SetInlineBotResults request) {
        return client.sendAwait(request);
    }

    // Docs lie - bots can't use method
    public Mono<Updates> getMessagesReactions(InputPeer peer, Iterable<Integer> messageIds) {
        return Mono.defer(() -> client.sendAwait(ImmutableGetMessagesReactions.of(peer, messageIds)));
    }

    public Mono<MessageReactionsList> getMessageReactionsList(GetMessageReactionsList request) {
        return client.sendAwait(request);
    }

    // Message interactions

    @BotCompatible
    public Mono<BaseMessageFields> sendMessage(SendMessage request) {
        return client.sendAwait(request)
                .flatMap(u -> transformMessageUpdate(request, u))
                .map(updates -> {
                    client.updates().emitNext(updates.getT2(), DEFAULT_PARKING);

                    return updates.getT1();
                });
    }

    @BotCompatible
    public Mono<BaseMessageFields> sendMedia(SendMedia request) {
        return client.sendAwait(request)
                .flatMap(u -> transformMessageUpdate(request, u))
                .map(updates -> {
                    client.updates().emitNext(updates.getT2(), DEFAULT_PARKING);

                    return updates.getT1();
                });
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

    /**
     * Retrieve minimal chats by their ids.
     * This method can return container which contains only {@link BaseChat},
     * {@link ChatForbidden} or {@link ChatEmpty} objects.
     *
     * @param ids An iterable of chat id elements
     * @return A {@link Mono} emitting on successful completion a list of
     * minimal chats or slice of list if there are a lot of chats
     */
    @BotCompatible
    public Mono<Chats> getChats(Iterable<Long> ids) {
        return Mono.defer(() -> client.sendAwait(ImmutableGetChats.of(ids)))
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
    @BotCompatible
    public Mono<ChatFull> getFullChat(long chatId) {
        return client.sendAwait(ImmutableGetFullChat.of(chatId))
                .flatMap(c -> storeLayout.onChatUpdate(c).thenReturn(c));
    }

    @BotCompatible
    public Mono<Void> editChatTitle(long chatId, String title) {
        return client.sendAwait(ImmutableEditChatTitle.of(chatId, title))
                .flatMap(u -> Mono.fromRunnable(() -> client.updates().emitNext(u, DEFAULT_PARKING)));
    }

    @BotCompatible
    public Mono<Void> editChatPhoto(long chatId, InputChatPhoto photo) {
        return client.sendAwait(ImmutableEditChatPhoto.of(chatId, photo))
                .flatMap(u -> Mono.fromRunnable(() -> client.updates().emitNext(u, DEFAULT_PARKING)));
    }

    public Mono<Updates> addChatUser(long chatId, InputUser user, int forwardLimit) {
        return client.sendAwait(ImmutableAddChatUser.of(chatId, user, forwardLimit));
    }

    @BotCompatible
    public Mono<Void> deleteChatUser(long chatId, InputUser userId, boolean revokeHistory) {
        return client.sendAwait(ImmutableDeleteChatUser.builder().revokeHistory(revokeHistory).chatId(chatId).userId(userId).build())
                .flatMap(u -> Mono.fromRunnable(() -> client.updates().emitNext(u, DEFAULT_PARKING)));
    }

    public Mono<Updates> createChat(Iterable<? extends InputUser> users, String title) {
        return Mono.defer(() -> client.sendAwait(ImmutableCreateChat.of(users, title)));
    }

    // folders namespace
    // =========================

    public Mono<Updates> editPeerFolders(Iterable<? extends InputFolderPeer> peers) {
        return Mono.defer(() -> client.sendAwait(ImmutableEditPeerFolders.of(peers)));
    }

    public Mono<Updates> deleteFolder(int folderId) {
        return client.sendAwait(ImmutableDeleteFolder.of(folderId));
    }

    // channels namespace
    // =========================

    @BotCompatible
    public Mono<SendAsPeers> getSendAs(InputPeer peer) {
        return client.sendAwait(ImmutableGetSendAs.of(peer));
    }

    @BotCompatible
    public Mono<Boolean> editChatAbout(InputPeer peer, String about) {
        return client.sendAwait(ImmutableEditChatAbout.of(peer, about));
    }

    @BotCompatible
    public Mono<AffectedMessages> deleteMessages(InputChannel channel, Iterable<Integer> ids) {
        return Mono.defer(() -> client.sendAwait(ImmutableDeleteMessages.of(channel, ids)));
    }

    @BotCompatible
    public Mono<Messages> getMessages(InputChannel channel, Iterable<? extends InputMessage> ids) {
        return Mono.defer(() -> client.sendAwait(ImmutableGetMessages.of(channel, ids)));
    }

    @BotCompatible
    public Mono<ChannelParticipants> getParticipants(InputChannel channel, ChannelParticipantsFilter filter,
                                                     int offset, int limit, Iterable<Long> ids) {
        return Mono.defer(() -> getParticipants(channel, filter, offset, limit, calculatePaginationHash(ids)));
    }

    @BotCompatible
    public Mono<ChannelParticipants> getParticipants(InputChannel channel, ChannelParticipantsFilter filter,
                                                     int offset, int limit, long hash) {
        return client.sendAwait(ImmutableGetParticipants.of(channel, filter, offset, limit, hash));
    }

    @BotCompatible
    public Mono<ChannelParticipant> getParticipant(InputChannel channel, InputPeer peer) {
        return client.sendAwait(ImmutableGetParticipant.of(channel, peer));
    }

    /**
     * Retrieve minimal channels by their ids.
     * This method can return container which contains only {@link Channel} or {@link ChannelForbidden} objects.
     *
     * @param ids An iterable of channel id elements
     * @return A {@link Mono} emitting on successful completion a list of
     * minimal channels or slice of list if there are a lot of channels
     */
    @BotCompatible
    public Mono<Chats> getChannels(Iterable<? extends InputChannel> ids) {
        return Mono.defer(() -> client.sendAwait(ImmutableGetChannels.of(ids)))
                .flatMap(c -> storeLayout.onContacts(c.chats(), List.of())
                        .thenReturn(c));
    }

    /**
     * Retrieve detailed channel by given id and update cache.
     *
     * @param id The id of channel
     * @return A {@link Mono} emitting on successful completion detailed information about channel
     */
    @BotCompatible
    public Mono<ChatFull> getFullChannel(InputChannel id) {
        return client.sendAwait(ImmutableGetFullChannel.of(id))
                .flatMap(c -> storeLayout.onChatUpdate(c).thenReturn(c));
    }

    @BotCompatible
    public Mono<Chat> editAdmin(InputChannel channel, InputUser user, ChatAdminRights rights, String rank) {
        return client.sendAwait(ImmutableEditAdmin.of(channel, user, rights, rank))
                .cast(BaseUpdates.class)
                .flatMap(u -> {
                    client.updates().emitNext(u, DEFAULT_PARKING);
                    return Mono.justOrEmpty(u.chats().get(0));
                });
    }

    @BotCompatible
    public Mono<Void> editTitle(InputChannel channel, String title) {
        return client.sendAwait(ImmutableEditTitle.of(channel, title))
                .flatMap(u -> Mono.fromRunnable(() -> client.updates().emitNext(u, DEFAULT_PARKING)));
    }

    @BotCompatible
    public Mono<Chat> editBanned(InputChannel channel, InputPeer participant, ChatBannedRights rights) {
        return client.sendAwait(ImmutableEditBanned.of(channel, participant, rights))
                .cast(BaseUpdates.class)
                .flatMap(u -> {
                    client.updates().emitNext(u, DEFAULT_PARKING);
                    return Mono.justOrEmpty(u.chats().get(0));
                });
    }

    @BotCompatible
    public Mono<Updates> editPhoto(InputChannel channel, InputChatPhoto photo) {
        return client.sendAwait(ImmutableEditPhoto.of(channel, photo));
    }

    @BotCompatible
    public Mono<Void> leaveChannel(InputChannel channel) {
        return client.sendAwait(ImmutableLeaveChannel.of(channel))
                .flatMap(u -> Mono.fromRunnable(() -> client.updates().emitNext(u, DEFAULT_PARKING)));
    }

    @BotCompatible
    public Mono<Boolean> setStickers(InputChannel channel, InputStickerSet stickerSet) {
        return client.sendAwait(ImmutableSetStickers.of(channel, stickerSet));
    }

    public Mono<Updates> toggleJoinToSend(InputChannel channel, boolean enabled) {
        return client.sendAwait(ImmutableToggleJoinToSend.of(channel, enabled));
    }

    public Mono<Updates> toggleJoinRequest(InputChannel channel, boolean enabled) {
        return client.sendAwait(ImmutableToggleJoinRequest.of(channel, enabled));
    }

    public Mono<AffectedHistory> deleteParticipantHistory(InputChannel channel, InputPeer participant) {
        return client.sendAwait(ImmutableDeleteParticipantHistory.of(channel, participant));
    }

    public Mono<Boolean> readHistory(InputChannel channel, int maxId) {
        return client.sendAwait(ImmutableReadHistory.of(channel, maxId));
    }

    public Mono<Boolean> reportSpam(InputChannel channel, InputPeer participant, Iterable<Integer> ids) {
        return Mono.defer(() -> client.sendAwait(ImmutableReportSpam.of(channel, participant, ids)));
    }

    public Mono<Updates> createChannel(CreateChannel request) {
        return client.sendAwait(request);
    }

    /**
     * Check if a username is free and can be assigned to a channel/supergroup.
     *
     * @param channel the channel/supergroup that will assign the specified username
     * @param username the username to check
     * @return A {@link Mono} emitting on successful completion {@code true}
     */
    public Mono<Boolean> checkUsername(InputChannel channel, String username) {
        return client.sendAwait(ImmutableCheckUsername.of(channel, username));
    }

    /**
     * Change the username of a supergroup/channel.
     *
     * @param channel the channel/supergroup that will assign the specified username
     * @param username the username to update
     * @return A {@link Mono} emitting on successful completion {@code true}
     */
    public Mono<Boolean> updateUsername(InputChannel channel, String username) {
        return client.sendAwait(ImmutableUpdateUsername.of(channel, username));
    }

    public Mono<Updates> joinChannel(InputChannel channel) {
        return client.sendAwait(ImmutableJoinChannel.of(channel));
    }

    public Mono<Updates> inviteToChannel(InputChannel channel, Iterable<? extends InputUser> ids) {
        return Mono.defer(() -> client.sendAwait(ImmutableInviteToChannel.of(channel, ids)));
    }

    public Mono<Updates> deleteChannel(InputChannel channel) {
        return client.sendAwait(ImmutableDeleteChannel.of(channel));
    }

    public Mono<ExportedMessageLink> exportMessageLink(ExportMessageLink request) {
        return client.sendAwait(request);
    }

    public Mono<Updates> toggleSignatures(InputChannel channel, boolean enabled) {
        return client.sendAwait(ImmutableToggleSignatures.of(channel, enabled));
    }

    public Mono<Chats> getAdminedPublicChannels(GetAdminedPublicChannels request) {
        return client.sendAwait(request);
    }

    public Mono<AdminLogResults> getAdminLog(GetAdminLog request) {
        return client.sendAwait(request);
    }

    public Mono<Boolean> readMessageContents(InputChannel channel, Iterable<Integer> ids) {
        return Mono.defer(() -> client.sendAwait(ImmutableReadMessageContents.of(channel, ids)));
    }

    public Mono<Updates> deleteHistory(InputChannel channel, int maxId, boolean forEveryone) {
        return client.sendAwait(ImmutableDeleteHistory.of(forEveryone
                ? ImmutableDeleteHistory.FOR_EVERYONE_MASK : 0, channel, maxId));
    }

    public Mono<Updates> togglePreHistoryHidden(InputChannel channel, boolean enabled) {
        return client.sendAwait(ImmutableTogglePreHistoryHidden.of(channel, enabled));
    }

    public Mono<Chats> getLeftChannels(int offset) {
        return client.sendAwait(ImmutableGetLeftChannels.of(offset));
    }

    public Mono<Chats> getGroupsForDiscussion() {
        return client.sendAwait(GetGroupsForDiscussion.instance());
    }

    public Mono<Boolean> setDiscussionGroup(InputChannel broadcast, InputChannel group) {
        return client.sendAwait(ImmutableSetDiscussionGroup.of(broadcast, group));
    }

    public Mono<Updates> editCreator(InputChannel channel, InputUser user, InputCheckPasswordSRP password) {
        return client.sendAwait(ImmutableEditCreator.of(channel, user, password));
    }

    public Mono<Boolean> editLocation(InputChannel channel, InputGeoPoint geoPint, String address) {
        return client.sendAwait(ImmutableEditLocation.of(channel, geoPint, address));
    }

    public Mono<Updates> toggleSlowMode(InputChannel channel, int seconds) {
        return client.sendAwait(ImmutableToggleSlowMode.of(channel, seconds));
    }

    public Mono<InactiveChats> getInactiveChats() {
        return client.sendAwait(GetInactiveChannels.instance());
    }

    public Mono<Updates> convertToGigagroup(InputChannel channel) {
        return client.sendAwait(ImmutableConvertToGigagroup.of(channel));
    }

    public Mono<Boolean> viewSponsoredMessage(InputChannel channel, ByteBuf randomId) {
        return Mono.defer(() -> client.sendAwait(ImmutableViewSponsoredMessage.of(channel, randomId)));
    }

    public Mono<SponsoredMessages> getSponsoredMessages(InputChannel channel) {
        return client.sendAwait(ImmutableGetSponsoredMessages.of(channel));
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
                        default: throw new IllegalArgumentException("Unknown Updates type: " + updates);
                    }
                }));
    }
}
