package telegram4j.mtproto.service;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.function.TupleUtils;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.mtproto.util.CryptoUtil;
import telegram4j.tl.*;
import telegram4j.tl.request.messages.BaseSendMessageRequest;
import telegram4j.tl.request.messages.EditMessage;
import telegram4j.tl.request.messages.SendMedia;
import telegram4j.tl.request.messages.SendMessage;
import telegram4j.tl.request.upload.SaveFilePart;

import java.security.MessageDigest;

public class MessageService extends RpcService {

    private static final int PART_SIZE = 512 * 1024;
    private static final int TEN_MB = 10 * 1024 * 1024;
    private static final int LIMIT_MB = 2000 * 1024 * 1024;
    private static final int PARALLELISM = 3;

    public MessageService(MTProtoClient client, StoreLayout storeLayout) {
        super(client, storeLayout);
    }

    // TODO list:
    // getMessages id:Vector<InputMessage> = messages.Messages;
    // getDialogs flags:# exclude_pinned:flags.0?true folder_id:flags.1?int offset_date:int offset_id:int offset_peer:InputPeer limit:int hash:long = messages.Dialogs;
    // getHistory peer:InputPeer offset_id:int offset_date:int add_offset:int limit:int max_id:int min_id:int hash:long = messages.Messages;
    // search flags:# peer:InputPeer q:string from_id:flags.0?InputPeer top_msg_id:flags.1?int filter:MessagesFilter min_date:int max_date:int offset_id:int add_offset:int limit:int max_id:int min_id:int hash:long = messages.Messages;
    // readHistory peer:InputPeer max_id:int = messages.AffectedMessages;
    // deleteHistory flags:# just_clear:flags.0?true revoke:flags.1?true peer:InputPeer max_id:int = messages.AffectedHistory;
    // deleteMessages flags:# revoke:flags.0?true id:Vector<int> = messages.AffectedMessages;
    // receivedMessages max_id:int = Vector<ReceivedNotifyMessage>;
    // setTyping flags:# peer:InputPeer top_msg_id:flags.0?int action:SendMessageAction = Bool;
    // sendMessage flags:# no_webpage:flags.1?true silent:flags.5?true background:flags.6?true clear_draft:flags.7?true peer:InputPeer reply_to_msg_id:flags.0?int message:string random_id:long reply_markup:flags.2?ReplyMarkup entities:flags.3?Vector<MessageEntity> schedule_date:flags.10?int = Updates;
    // sendMedia flags:# silent:flags.5?true background:flags.6?true clear_draft:flags.7?true peer:InputPeer reply_to_msg_id:flags.0?int media:InputMedia message:string random_id:long reply_markup:flags.2?ReplyMarkup entities:flags.3?Vector<MessageEntity> schedule_date:flags.10?int = Updates;
    // forwardMessages flags:# silent:flags.5?true background:flags.6?true with_my_score:flags.8?true drop_author:flags.11?true drop_media_captions:flags.12?true from_peer:InputPeer id:Vector<int> random_id:Vector<long> to_peer:InputPeer schedule_date:flags.10?int = Updates;
    // reportSpam peer:InputPeer = Bool;
    // getPeerSettings peer:InputPeer = PeerSettings;
    // report peer:InputPeer id:Vector<int> reason:ReportReason message:string = Bool;
    // getChats id:Vector<long> = messages.Chats;
    // getFullChat chat_id:long = messages.ChatFull;
    // editChatTitle chat_id:long title:string = Updates;
    // editChatPhoto chat_id:long photo:InputChatPhoto = Updates;
    // addChatUser chat_id:long user_id:InputUser fwd_limit:int = Updates;
    // deleteChatUser flags:# revoke_history:flags.0?true chat_id:long user_id:InputUser = Updates;
    // createChat users:Vector<InputUser> title:string = Updates;
    // getDhConfig version:int random_length:int = messages.DhConfig;
    // requestEncryption user_id:InputUser random_id:int g_a:bytes = EncryptedChat;
    // acceptEncryption peer:InputEncryptedChat g_b:bytes key_fingerprint:long = EncryptedChat;
    // discardEncryption flags:# delete_history:flags.0?true chat_id:int = Bool;
    // setEncryptedTyping peer:InputEncryptedChat typing:Bool = Bool;
    // readEncryptedHistory peer:InputEncryptedChat max_date:int = Bool;
    // sendEncrypted flags:# silent:flags.0?true peer:InputEncryptedChat random_id:long data:bytes = messages.SentEncryptedMessage;
    // sendEncryptedFile flags:# silent:flags.0?true peer:InputEncryptedChat random_id:long data:bytes file:InputEncryptedFile = messages.SentEncryptedMessage;
    // sendEncryptedService peer:InputEncryptedChat random_id:long data:bytes = messages.SentEncryptedMessage;
    // receivedQueue max_qts:int = Vector<long>;
    // reportEncryptedSpam peer:InputEncryptedChat = Bool;
    // readMessageContents id:Vector<int> = messages.AffectedMessages;
    // getStickers emoticon:string hash:long = messages.Stickers;
    // getAllStickers hash:long = messages.AllStickers;
    // getWebPagePreview flags:# message:string entities:flags.3?Vector<MessageEntity> = MessageMedia;
    // exportChatInvite flags:# legacy_revoke_permanent:flags.2?true peer:InputPeer expire_date:flags.0?int usage_limit:flags.1?int = ExportedChatInvite;
    // checkChatInvite hash:string = ChatInvite;
    // importChatInvite hash:string = Updates;
    // getStickerSet stickerset:InputStickerSet = messages.StickerSet;
    // installStickerSet stickerset:InputStickerSet archived:Bool = messages.StickerSetInstallResult;
    // uninstallStickerSet stickerset:InputStickerSet = Bool;
    // startBot bot:InputUser peer:InputPeer random_id:long start_param:string = Updates;
    // getMessagesViews peer:InputPeer id:Vector<int> increment:Bool = messages.MessageViews;
    // editChatAdmin chat_id:long user_id:InputUser is_admin:Bool = Bool;
    // migrateChat chat_id:long = Updates;
    // searchGlobal flags:# folder_id:flags.0?int q:string filter:MessagesFilter min_date:int max_date:int offset_rate:int offset_peer:InputPeer offset_id:int limit:int = messages.Messages;
    // reorderStickerSets flags:# masks:flags.0?true order:Vector<long> = Bool;
    // getDocumentByHash sha256:bytes size:int mime_type:string = Document;
    // getSavedGifs hash:long = messages.SavedGifs;
    // saveGif id:InputDocument unsave:Bool = Bool;
    // getInlineBotResults flags:# bot:InputUser peer:InputPeer geo_point:flags.0?InputGeoPoint query:string offset:string = messages.BotResults;
    // setInlineBotResults flags:# gallery:flags.0?true private:flags.1?true query_id:long results:Vector<InputBotInlineResult> cache_time:int next_offset:flags.2?string switch_pm:flags.3?InlineBotSwitchPM = Bool;
    // sendInlineBotResult flags:# silent:flags.5?true background:flags.6?true clear_draft:flags.7?true hide_via:flags.11?true peer:InputPeer reply_to_msg_id:flags.0?int random_id:long query_id:long id:string schedule_date:flags.10?int = Updates;
    // getMessageEditData peer:InputPeer id:int = messages.MessageEditData;
    // editMessage flags:# no_webpage:flags.1?true peer:InputPeer id:int message:flags.11?string media:flags.14?InputMedia reply_markup:flags.2?ReplyMarkup entities:flags.3?Vector<MessageEntity> schedule_date:flags.15?int = Updates;
    // editInlineBotMessage flags:# no_webpage:flags.1?true id:InputBotInlineMessageID message:flags.11?string media:flags.14?InputMedia reply_markup:flags.2?ReplyMarkup entities:flags.3?Vector<MessageEntity> = Bool;
    // getBotCallbackAnswer flags:# game:flags.1?true peer:InputPeer msg_id:int data:flags.0?bytes password:flags.2?InputCheckPasswordSRP = messages.BotCallbackAnswer;
    // setBotCallbackAnswer flags:# alert:flags.1?true query_id:long message:flags.0?string url:flags.2?string cache_time:int = Bool;
    // getPeerDialogs peers:Vector<InputDialogPeer> = messages.PeerDialogs;
    // saveDraft flags:# no_webpage:flags.1?true reply_to_msg_id:flags.0?int peer:InputPeer message:string entities:flags.3?Vector<MessageEntity> = Bool;
    // getAllDrafts = Updates;
    // getFeaturedStickers hash:long = messages.FeaturedStickers;
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
            // TODO: implement session paralleling
            return Mono.empty();
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

                    client.updates().emitNext(upd.getT2(), Sinks.EmitFailureHandler.FAIL_FAST);

                    return upd.getT1();
                }));
    }

    public Mono<Message> sendMedia(SendMedia request) {
        return client.sendAwait(request)
                .zipWith(toPeer(request.peer()))
                .map(TupleUtils.function((updates, peer) -> {
                    var upd = transformMessageUpdate(request, updates, peer);

                    client.updates().emitNext(upd.getT2(), Sinks.EmitFailureHandler.FAIL_FAST);

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

                            client.updates().emitNext(updates, Sinks.EmitFailureHandler.FAIL_FAST);

                            return update.message();
                        default:
                            throw new IllegalArgumentException("Unknown updates type: " + updates);
                    }
                });
    }

    // Short-send related updates object should be transformed to the updateShort.
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
            default: throw new IllegalArgumentException("Unknown updates type: " + updates);
        }
    }
}
