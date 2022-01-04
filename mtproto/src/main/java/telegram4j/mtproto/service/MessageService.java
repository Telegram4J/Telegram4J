package telegram4j.mtproto.service;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.function.TupleUtils;
import reactor.util.annotation.Nullable;
import reactor.util.concurrent.Queues;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.mtproto.util.CryptoUtil;
import telegram4j.mtproto.util.EmissionHandlers;
import telegram4j.tl.ExportedChatInvite;
import telegram4j.tl.*;
import telegram4j.tl.messages.MessageViews;
import telegram4j.tl.messages.PeerSettings;
import telegram4j.tl.messages.*;
import telegram4j.tl.request.messages.*;
import telegram4j.tl.request.upload.SaveBigFilePart;
import telegram4j.tl.request.upload.SaveFilePart;

import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class MessageService extends RpcService {

    private static final int PART_SIZE = 512 * 1024;
    private static final int TEN_MB = 10 * 1024 * 1024;
    private static final int LIMIT_MB = 2000 * 1024 * 1024;
    private static final int PARALLELISM = 5;

    private static final Sinks.EmitFailureHandler emissionHandler = EmissionHandlers.park(Duration.ofNanos(10));

    public MessageService(MTProtoClient client, StoreLayout storeLayout) {
        super(client, storeLayout);
    }

    // TODO list:

    public Mono<Messages> getMessages(Iterable<? extends InputMessage> ids) {
        return client.sendAwait(GetMessages.builder()
                .id(ids)
                .build());
    }

    public Mono<Dialogs> getDialogs(GetDialogs request) {
        return client.sendAwait(request);
    }

    public Mono<Messages> getHistory(InputPeer peer, int offsetId, int offsetDate, int addOffset,
                                     int limit, int maxId, int minId, long hash) {
        return client.sendAwait(ImmutableGetHistory.of(peer, offsetId, offsetDate, addOffset, limit, maxId, minId, hash));
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

    public Mono<Boolean> setTyping(InputPeer peer, @Nullable Integer topMsgId, SendMessageAction action) {
        return client.sendAwait(SetTyping.builder()
                .peer(peer)
                .topMsgId(topMsgId)
                .action(action)
                .build());
    }

    public Flux<Message> forwardMessages(ForwardMessages request) {
        return client.sendAwait(request)
                .ofType(BaseUpdates.class)
                .flatMapMany(updates -> {
                    client.updates().emitNext(updates, emissionHandler);

                    return Flux.fromIterable(updates.updates())
                            .ofType(UpdateNewMessageFields.class)
                            .map(UpdateNewMessageFields::message);
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
        return client.sendAwait(ReadMessageContents.builder()
                .id(ids)
                .build());
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

    public Mono<Boolean> saveGif(InputDocument document, boolean unsave) {
        return client.sendAwait(ImmutableSaveGif.of(document, unsave));
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

    public Mono<MessageEditData> getMessageEditData(InputPeer peer, int id) {
        return client.sendAwait(ImmutableGetMessageEditData.of(peer, id));
    }

    public Mono<Boolean> editInlineBotMessage(EditInlineBotMessage request) {
        return client.sendAwait(request);
    }

    public Mono<BotCallbackAnswer> getBotCallbackAnswer(GetBotCallbackAnswer request) {
        return client.sendAwait(request);
    }

    public Mono<Boolean> setBotCallbackAnswer(SetBotCallbackAnswer request) {
        return client.sendAwait(request);
    }

    public Mono<PeerDialogs> getPeerDialogs(Iterable<? extends InputDialogPeer> peers) {
        return client.sendAwait(GetPeerDialogs.builder()
                .peers(peers)
                .build());
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

    // readFeaturedStickers id:Vector<long> = Bool;
    // getRecentStickers flags:# attached:flags.0?true hash:long = messages.RecentStickers;
    // saveRecentSticker flags:# attached:flags.0?true id:InputDocument unsave:Bool = Bool;
    // clearRecentStickers flags:# attached:flags.0?true = Bool;
    // getArchivedStickers flags:# masks:flags.0?true offset_id:long limit:int = messages.ArchivedStickers;
    // getMaskStickers hash:long = messages.AllStickers;
    // getAttachedStickers media:InputStickeredMedia = Vector<StickerSetCovered>;
    // setGameScore flags:# edit_message:flags.0?true force:flags.1?true peer:InputPeer id:int user_id:InputUser score:int = Updates;
    // setInlineGameScore flags:# edit_message:flags.0?true force:flags.1?true id:InputBotInlineMessageID user_id:InputUser score:int = Bool;
    // getGameHighScores peer:InputPeer id:int user_id:InputUser = messages.HighScores;
    // getInlineGameHighScores id:InputBotInlineMessageID user_id:InputUser = messages.HighScores;
    // getCommonChats user_id:InputUser max_id:long limit:int = messages.Chats;
    // getAllChats except_ids:Vector<long> = messages.Chats;
    // getWebPage url:string hash:int = WebPage;
    // toggleDialogPin flags:# pinned:flags.0?true peer:InputDialogPeer = Bool;
    // reorderPinnedDialogs flags:# force:flags.0?true folder_id:int order:Vector<InputDialogPeer> = Bool;
    // getPinnedDialogs folder_id:int = messages.PeerDialogs;
    // setBotShippingResults flags:# query_id:long error:flags.0?string shipping_options:flags.1?Vector<ShippingOption> = Bool;
    // setBotPrecheckoutResults flags:# success:flags.1?true query_id:long error:flags.0?string = Bool;
    // uploadMedia peer:InputPeer media:InputMedia = MessageMedia;
    // sendScreenshotNotification peer:InputPeer reply_to_msg_id:int random_id:long = Updates;
    // getFavedStickers hash:long = messages.FavedStickers;
    // faveSticker id:InputDocument unfave:Bool = Bool;
    // getUnreadMentions peer:InputPeer offset_id:int add_offset:int limit:int max_id:int min_id:int = messages.Messages;
    // readMentions peer:InputPeer = messages.AffectedHistory;
    // getRecentLocations peer:InputPeer limit:int hash:long = messages.Messages;
    // sendMultiMedia flags:# silent:flags.5?true background:flags.6?true clear_draft:flags.7?true peer:InputPeer reply_to_msg_id:flags.0?int multi_media:Vector<InputSingleMedia> schedule_date:flags.10?int = Updates;
    // uploadEncryptedFile peer:InputEncryptedChat file:InputEncryptedFile = EncryptedFile;
    // searchStickerSets flags:# exclude_featured:flags.0?true q:string hash:long = messages.FoundStickerSets;
    // getSplitRanges = Vector<MessageRange>;
    // markDialogUnread flags:# unread:flags.0?true peer:InputDialogPeer = Bool;
    // getDialogUnreadMarks = Vector<DialogPeer>;
    // clearAllDrafts = Bool;
    // updatePinnedMessage flags:# silent:flags.0?true unpin:flags.1?true pm_oneside:flags.2?true peer:InputPeer id:int = Updates;
    // sendVote peer:InputPeer msg_id:int options:Vector<bytes> = Updates;
    // getPollResults peer:InputPeer msg_id:int = Updates;
    // getOnlines peer:InputPeer = ChatOnlines;
    // editChatAbout peer:InputPeer about:string = Bool;
    // editChatDefaultBannedRights peer:InputPeer banned_rights:ChatBannedRights = Updates;
    // getEmojiKeywords lang_code:string = EmojiKeywordsDifference;
    // getEmojiKeywordsDifference lang_code:string from_version:int = EmojiKeywordsDifference;
    // getEmojiKeywordsLanguages lang_codes:Vector<string> = Vector<EmojiLanguage>;
    // getEmojiURL lang_code:string = EmojiURL;
    // getSearchCounters peer:InputPeer filters:Vector<MessagesFilter> = Vector<messages.SearchCounter>;
    // requestUrlAuth flags:# peer:flags.1?InputPeer msg_id:flags.1?int button_id:flags.1?int url:flags.2?string = UrlAuthResult;
    // acceptUrlAuth flags:# write_allowed:flags.0?true peer:flags.1?InputPeer msg_id:flags.1?int button_id:flags.1?int url:flags.2?string = UrlAuthResult;
    // hidePeerSettingsBar peer:InputPeer = Bool;
    // getScheduledHistory peer:InputPeer hash:long = messages.Messages;
    // getScheduledMessages peer:InputPeer id:Vector<int> = messages.Messages;
    // sendScheduledMessages peer:InputPeer id:Vector<int> = Updates;
    // deleteScheduledMessages peer:InputPeer id:Vector<int> = Updates;
    // getPollVotes flags:# peer:InputPeer id:int option:flags.0?bytes offset:flags.1?string limit:int = messages.VotesList;
    // toggleStickerSets flags:# uninstall:flags.0?true archive:flags.1?true unarchive:flags.2?true stickersets:Vector<InputStickerSet> = Bool;
    // getDialogFilters = Vector<DialogFilter>;
    // getSuggestedDialogFilters = Vector<DialogFilterSuggested>;
    // updateDialogFilter flags:# id:int filter:flags.0?DialogFilter = Bool;
    // updateDialogFiltersOrder order:Vector<int> = Bool;
    // getOldFeaturedStickers offset:int limit:int hash:long = messages.FeaturedStickers;
    // getReplies peer:InputPeer msg_id:int offset_id:int offset_date:int add_offset:int limit:int max_id:int min_id:int hash:long = messages.Messages;
    // getDiscussionMessage peer:InputPeer msg_id:int = messages.DiscussionMessage;
    // readDiscussion peer:InputPeer msg_id:int read_max_id:int = Bool;
    // unpinAllMessages peer:InputPeer = messages.AffectedHistory;
    // deleteChat chat_id:long = Bool;
    // deletePhoneCallHistory flags:# revoke:flags.0?true = messages.AffectedFoundMessages;
    // checkHistoryImport import_head:string = messages.HistoryImportParsed;
    // initHistoryImport peer:InputPeer file:InputFile media_count:int = messages.HistoryImport;
    // uploadImportedMedia peer:InputPeer import_id:long file_name:string media:InputMedia = MessageMedia;
    // startHistoryImport peer:InputPeer import_id:long = Bool;
    // getExportedChatInvites flags:# revoked:flags.3?true peer:InputPeer admin_id:InputUser offset_date:flags.2?int offset_link:flags.2?string limit:int = messages.ExportedChatInvites;
    // getExportedChatInvite peer:InputPeer link:string = messages.ExportedChatInvite;
    // editExportedChatInvite flags:# revoked:flags.2?true peer:InputPeer link:string expire_date:flags.0?int usage_limit:flags.1?int = messages.ExportedChatInvite;
    // deleteRevokedExportedChatInvites peer:InputPeer admin_id:InputUser = Bool;
    // deleteExportedChatInvite peer:InputPeer link:string = Bool;
    // getAdminsWithInvites peer:InputPeer = messages.ChatAdminsWithInvites;
    // getChatInviteImporters peer:InputPeer link:string offset_date:int offset_user:InputUser limit:int = messages.ChatInviteImporters;
    // setHistoryTTL peer:InputPeer period:int = Updates;
    // checkHistoryImportPeer peer:InputPeer = messages.CheckedHistoryImportPeer;
    // setChatTheme peer:InputPeer emoticon:string = Updates;
    // getMessageReadParticipants peer:InputPeer msg_id:int = Vector<long>;

    public Mono<InputFile> saveFile(ByteBuf data, String name) {
        long fileId = CryptoUtil.random.nextLong();
        int parts = (int) Math.ceil((float) data.readableBytes() / PART_SIZE);
        boolean big = data.readableBytes() > TEN_MB;

        if (data.readableBytes() > LIMIT_MB) {
            return Mono.error(new IllegalArgumentException("File size is under limit. Size: "
                    + data.readableBytes() + ", limit: " + LIMIT_MB));
        }

        if (big) {
            Sinks.Many<SaveBigFilePart> queue = Sinks.many().multicast()
                    .onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);

            AtomicInteger it = new AtomicInteger(0);
            AtomicInteger suc = new AtomicInteger(0);

            List<MTProtoClient> clients = new ArrayList<>(PARALLELISM);
            DataCenter mediaDc = DataCenter.mediaDataCenters.get(0);

            Sinks.Empty<Void> done = Sinks.empty();

            Mono<Void> initialize = Flux.range(0, PARALLELISM)
                    .map(i -> client.createMediaClient(mediaDc))
                    .doOnNext(clients::add)
                    .flatMap(MTProtoClient::connect)
                    .then();

            Mono<Void> sender = queue.asFlux()
                    .publishOn(Schedulers.boundedElastic())
                    .handle((req, sink) -> {
                        MTProtoClient client = clients.get(it.getAndUpdate(i -> i + 1 == PARALLELISM ? 0 : i + 1));
                        client.sendAwait(req)
                                .filter(b -> b)
                                .switchIfEmpty(Mono.error(new IllegalStateException("Failed to upload part #" + req.filePart())))
                                .flatMap(b -> {
                                    if (suc.incrementAndGet() == req.fileTotalParts()) {
                                        done.emitEmpty(Sinks.EmitFailureHandler.FAIL_FAST);
                                        queue.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
                                        return Flux.fromIterable(clients)
                                                .flatMap(MTProtoClient::close)
                                                .then(Mono.fromRunnable(sink::complete));
                                    }
                                    return Mono.empty();
                                })
                                .subscribe();
                    })
                    .then();

            Mono<Void> reader = Flux.range(0, parts)
                    .doOnNext(filePart -> {
                        ByteBuf part = data.readBytes(Math.min(PART_SIZE, data.readableBytes()));
                        byte[] partBytes = CryptoUtil.toByteArray(part);

                        SaveBigFilePart req = SaveBigFilePart.builder()
                                .fileId(fileId)
                                .filePart(filePart)
                                .bytes(partBytes)
                                .fileTotalParts(parts)
                                .build();

                        queue.emitNext(req, emissionHandler);
                    })
                    .then();

            return Mono.when(initialize, reader, sender)
                    .then(Mono.fromSupplier(() -> ImmutableInputFileBig.of(fileId, parts, name)));
        }

        MessageDigest md5 = CryptoUtil.MD5.get();

        return Flux.range(0, parts)
                .flatMap(filePart -> {
                    ByteBuf part = data.readBytes(Math.min(PART_SIZE, data.readableBytes()));
                    byte[] partBytes = CryptoUtil.toByteArray(part);

                    synchronized (md5) {
                        md5.update(partBytes);
                    }

                    SaveFilePart req = SaveFilePart.builder()
                            .fileId(fileId)
                            .filePart(filePart)
                            .bytes(partBytes)
                            .build();

                    return client.sendAwait(req);
                })
                .then(Mono.fromSupplier(() -> ImmutableBaseInputFile.of(fileId,
                        parts, name, ByteBufUtil.hexDump(md5.digest()))));
    }

    public Mono<Message> sendMessage(SendMessage request) {
        return client.sendAwait(request)
                .zipWith(toPeer(request.peer()))
                .map(TupleUtils.function((updates, peer) -> {
                    var upd = transformMessageUpdate(request, updates, peer);

                    client.updates().emitNext(upd.getT2(), emissionHandler);

                    return upd.getT1();
                }));
    }

    public Mono<Message> sendMedia(SendMedia request) {
        return client.sendAwait(request)
                .zipWith(toPeer(request.peer()))
                .map(TupleUtils.function((updates, peer) -> {
                    var upd = transformMessageUpdate(request, updates, peer);

                    client.updates().emitNext(upd.getT2(), emissionHandler);

                    return upd.getT1();
                }));
    }

    public Mono<Message> editMessage(EditMessage request) {
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

                            client.updates().emitNext(updates, emissionHandler);

                            return update.message();
                        default:
                            throw new IllegalArgumentException("Unknown updates type: " + updates);
                    }
                });
    }

    // Short-send related updates object should be transformed to the updateShort or baseUpdate.
    // https://core.telegram.org/api/updates-sequence
    static Tuple2<Message, Updates> transformMessageUpdate(BaseSendMessageRequest request, Updates updates, Peer peer) {
        switch (updates.identifier()) {
            case UpdateShortSentMessage.ID: {
                UpdateShortSentMessage casted = (UpdateShortSentMessage) updates;
                Integer replyToMsgId = request.replyToMsgId();
                Message message = BaseMessage.builder()
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

                Message message = BaseMessage.builder()
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

                Message message = BaseMessage.builder()
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
                        .filter(upd -> upd instanceof UpdateMessageID)
                        .map(upd -> (UpdateMessageID) upd)
                        .findFirst()
                        .orElseThrow(IllegalStateException::new);

                if (updateMessageID.randomId() != request.randomId()) {
                    throw new IllegalArgumentException("Incorrect random id. Excepted: " + request.randomId()
                            + ", received: " + updateMessageID.randomId());
                }

                Message message = casted.updates().stream()
                        .filter(upd -> upd instanceof UpdateNewMessageFields)
                        .map(upd -> ((UpdateNewMessageFields) upd).message())
                        .findFirst()
                        .orElseThrow(IllegalStateException::new);

                return Tuples.of(message, casted);
            }
            default:
                throw new IllegalArgumentException("Unknown updates type: " + updates);
        }
    }
}
