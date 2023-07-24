package telegram4j.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.auxiliary.AuxiliaryStickerSet;
import telegram4j.core.event.EventAdapter;
import telegram4j.core.event.UpdatesManager;
import telegram4j.core.event.domain.Event;
import telegram4j.core.handle.MTProtoPeerHandle;
import telegram4j.core.handle.PeerHandle;
import telegram4j.core.internal.AuxiliaryEntityFactory;
import telegram4j.core.internal.EntityFactory;
import telegram4j.core.internal.MappingUtil;
import telegram4j.mtproto.internal.Preconditions;
import telegram4j.core.object.BotInfo;
import telegram4j.core.object.Document;
import telegram4j.core.object.Message;
import telegram4j.core.object.MessageAction;
import telegram4j.core.object.MessageMedia;
import telegram4j.core.object.User;
import telegram4j.core.object.*;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.chat.ChatParticipant;
import telegram4j.core.object.chat.*;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.retriever.EntityRetriever;
import telegram4j.core.spec.BotCommandScopeSpec;
import telegram4j.core.util.Id;
import telegram4j.core.util.PeerId;
import telegram4j.mtproto.DcId;
import telegram4j.mtproto.client.MTProtoClientGroup;
import telegram4j.mtproto.file.*;
import telegram4j.mtproto.service.ServiceHolder;
import telegram4j.mtproto.service.UploadOptions;
import telegram4j.mtproto.service.UploadService;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;
import telegram4j.tl.messages.AffectedMessages;
import telegram4j.tl.request.bots.*;
import telegram4j.tl.request.messages.ImmutableDeleteMessages;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

public final class MTProtoTelegramClient implements EntityRetriever {
    private final AuthorizationResources authResources;
    private final MTProtoClientGroup mtProtoClientGroup;
    private final MTProtoResources mtProtoResources;
    private final UpdatesManager updatesManager;
    private final Id selfId;
    private final ServiceHolder serviceHolder;
    private final EntityRetriever entityRetriever;
    private final PeerHandle peerHandle;
    private final Mono<Void> onDisconnect;

    MTProtoTelegramClient(AuthorizationResources authResources,
                          MTProtoClientGroup mtProtoClientGroup, MTProtoResources mtProtoResources,
                          Function<MTProtoTelegramClient, UpdatesManager> updatesManager,
                          Id selfId, ServiceHolder serviceHolder,
                          EntityRetrievalStrategy entityRetriever,
                          Mono<Void> onDisconnect) {
        this.authResources = authResources;
        this.mtProtoClientGroup = mtProtoClientGroup;
        this.mtProtoResources = mtProtoResources;
        this.serviceHolder = serviceHolder;
        this.selfId = selfId;
        this.entityRetriever = entityRetriever.apply(this);
        this.updatesManager = updatesManager.apply(this);
        this.peerHandle = new PeerHandle(this, new MTProtoPeerHandle(this));
        this.onDisconnect = onDisconnect;
    }

    /**
     * Creates client builder with bot authorization schematic.
     *
     * @see <a href="https://core.telegram.org/bots/features#botfather">BotFather</a>
     * @see <a href="https://core.telegram.org/api/obtaining_api_id#obtaining-api-id">Obtaining Api Id and Hash</a>
     * @param apiId The api id.
     * @param apiHash The api hash.
     * @param botAuthToken The bot auth token from BotFather DM.
     * @return The new {@link MTProtoBootstrap} client builder.
     */
    public static MTProtoBootstrap create(int apiId, String apiHash, String botAuthToken) {
        return new MTProtoBootstrap(new AuthorizationResources(apiId, apiHash, botAuthToken), null);
    }

    /**
     * Creates client builder with user authorization schematic.
     *
     * @see <a href="https://core.telegram.org/api/auth">User authorization</a>
     * @see <a href="https://core.telegram.org/api/obtaining_api_id#obtaining-api-id">Obtaining Api Id</a>
     * @param apiId The api id.
     * @param apiHash The api hash.
     * @param authHandler The user authorization implementation. QR or phone code handler.
     * @return The new {@link MTProtoBootstrap} client builder.
     */
    public static MTProtoBootstrap create(int apiId, String apiHash, AuthorizationHandler authHandler) {
        return new MTProtoBootstrap(new AuthorizationResources(apiId, apiHash), authHandler);
    }

    /**
     * Gets id of <i>current</i> user.
     *
     * @return The id of <i>current</i> user.
     */
    public Id getSelfId() {
        return selfId;
    }

    public PeerHandle getPeerHandle() {
        return peerHandle;
    }

    /**
     * Gets {@link UpdatesManager} resource to handle incoming {@link Updates} from MTProto
     * client and controlling updates gaps.
     *
     * @return The {@link UpdatesManager} resource to handle {@link Updates}.
     */
    public UpdatesManager getUpdatesManager() {
        return updatesManager;
    }

    /**
     * Gets authorization resources used to log in.
     *
     * @return The authorization resources used to log in.
     */
    public AuthorizationResources getAuthResources() {
        return authResources;
    }

    public MTProtoResources getMtProtoResources() {
        return mtProtoResources;
    }

    public MTProtoClientGroup getMtProtoClientGroup() {
        return mtProtoClientGroup;
    }

    public ServiceHolder getServiceHolder() {
        return serviceHolder;
    }

    public Mono<Void> disconnect() {
        return mtProtoClientGroup.close();
    }

    public Mono<Void> onDisconnect() {
        return onDisconnect;
    }

    /**
     * Retrieves a {@link Flux} of the specified {@link Event} type for
     * subsequent processing and subscription.
     *
     * <p> This method doesn't handle errors occurred while processing
     * events, and occurred errors will terminate reactive sequence. For preventing
     * this behavior you should use special operators to handle them,
     * see <a href="https://projectreactor.io/docs/core/release/reference/#error.handling">this</a>
     * article o reactor wiki.
     *
     * <p> Invocation of this method is equivalent to this code:
     * {@code mtProtoResources.getEventDispatcher().on(type)}.
     *
     * @param type the event class of requested events.
     * @param <E> the event type.
     * @return a {@link Flux} of events.
     */
    public <E extends Event> Flux<E> on(Class<E> type) {
        return mtProtoResources.getEventDispatcher().on(type);
    }

    public Flux<Event> on(EventAdapter adapter) {
        return mtProtoResources.getEventDispatcher().on(adapter);
    }

    // Interaction methods
    // ===========================

    /**
     * Request to configure default admin rights for <i>current</i> bot
     * in {@link GroupChat} or {@link SupergroupChat} chats and {@link BroadcastChannel} channels.
     *
     * @param type The type of chat.
     * @param adminRights An {@link Iterable} with default admin rights.
     * @return A {@link Mono} emitting on successful completion boolean result state.
     */
    public Mono<Boolean> setChatDefaultAdminRights(Chat.Type type, Iterable<AdminRight> adminRights) {
        if (type == Chat.Type.PRIVATE)
            return Mono.error(new IllegalArgumentException("Invalid chat type: " + type));

        return Mono.defer(() -> {
            var chatAdminRights = ImmutableChatAdminRights.of(MappingUtil.getMaskValue(adminRights));
            var request = type == Chat.Type.CHANNEL
                    ? ImmutableSetBotBroadcastDefaultAdminRights.of(chatAdminRights)
                    : ImmutableSetBotGroupDefaultAdminRights.of(chatAdminRights);
            return mtProtoClientGroup.send(DcId.main(), request);
        });
    }

    /**
     * Request to upload file to Telegram Media DC.
     * The uploaded file will have the same name as local one and will upload with {@link UploadService#MAX_PART_SIZE max part size}.
     *
     * @see #uploadFile(Path, String, int)
     * @param path The path to local file.
     * @return A {@link Mono} emitting on successful completion {@link InputFile} with file id and MD5 hash if applicable.
     */
    public Mono<InputFile> uploadFile(Path path) {
        return uploadFile(path, path.getFileName().toString(), -1);
    }

    /**
     * Request to upload file to Telegram Media DC.
     *
     * @param path The path to local file.
     * @param filename The name of remote file.
     * @param partSize The part size for uploading, must be divisible by
     * {@link UploadService#MIN_PART_SIZE} and {@link UploadService#MAX_PART_SIZE} must be evenly divisible by part_size
     * @return A {@link Mono} emitting on successful completion {@link InputFile} with file id and MD5 hash if applicable.
     */
    public Mono<InputFile> uploadFile(Path path, String filename, int partSize) {
        return Mono.fromCallable(() -> Files.size(path))
                .flatMap(size -> {
                    int ps = UploadService.suggestPartSize(size, partSize);
                    return serviceHolder.getUploadService()
                            .saveFile(UploadOptions.builder()
                                    .data(fromPath(path, ps, ByteBufAllocator.DEFAULT))
                                    .size(size)
                                    .partSize(ps)
                                    .name(filename)
                                    .build());
                });
    }

    /**
     * Request to upload file to Telegram Media DC.
     *
     * @param options The options of uploading.
     * @return A {@link Mono} emitting on successful completion {@link InputFile} with file id and MD5 hash if applicable.
     */
    public Mono<InputFile> uploadFile(UploadOptions options) {
        return serviceHolder.getUploadService().saveFile(options);
    }

    /**
     * Request to download file by their reference from Telegram Media DC or
     * if file {@link Document#isWeb()} and haven't telegram-proxying try to directly download file by url.
     *
     * @see #downloadFile(FileReferenceId, long, int, boolean)
     * @param fileRefId The serialized {@link FileReferenceId} of file.
     * @param offset The number of bytes to be skipped.
     * @param limit The number of bytes to be returned.
     * @param precise Disable some checks on limit and offset values, useful for example to stream videos by keyframes.
     * @return A {@link Flux} emitting full or parts of downloading file.
     */
    public Flux<FilePart> downloadFile(String fileRefId, long offset, int limit, boolean precise) {
        return Mono.fromCallable(() -> FileReferenceId.deserialize(fileRefId))
                .flatMapMany(deser -> downloadFile(deser, offset, limit, precise));
    }

    /**
     * Request to download file by their reference from Telegram Media DC or
     * if file {@link Document#isWeb()} and haven't telegram-proxying try to directly download file by url.
     *
     * <p> File will fully download from zero offset with 1MB limit.
     *
     * @param fileRefId The serialized {@link FileReferenceId} of file.
     * @return A {@link Flux} emitting full or parts of downloading file.
     */
    public Flux<FilePart> downloadFile(String fileRefId) {
        return downloadFile(fileRefId, 0, 1024 * 1024, true);
    }

    /**
     * Request to download file by their reference from Telegram Media DC or
     * if file {@link Document#isWeb()} and haven't telegram-proxying try to directly download file by url.
     *
     * <p> File will fully download from zero offset with 1MB limit.
     *
     * @param fileRefId The location of file to download.
     * @return A {@link Flux} emitting full or parts of downloading file.
     */
    public Flux<FilePart> downloadFile(FileReferenceId fileRefId) {
        return downloadFile(fileRefId, 0, 1024 * 1024, true);
    }

    /**
     * Request to download file by their reference from Telegram Media DC.
     *
     * <p> Chunk parameters must meet the following requirements: </p>
     * If {@code precise} flag is set then:
     * <ul>
     *   <li>{@code offset % 1024 == 0}</li>
     *   <li>{@code limit % 1024 == 0}</li>
     *   <li>{@code limit <= 1024 * 1024}</li>
     * </ul>
     *
     * In other case:
     * <ul>
     *   <li>{@code offset % (4 * 1024) == 0}</li>
     *   <li>{@code limit % (4 * 1024) == 0}</li>
     *   <li>{@code (1024 * 1024) %  limit == 0}</li>
     * </ul>
     *
     * @see <a href="https://core.telegram.org/api/files#downloading-files">File Downloading</a>
     * @throws IllegalArgumentException If specified download parameters incorrect or if
     * {@code fileRefId} points to webfile and current authorized account is bot or file has no telegram proxying.
     * @param fileRefId The location of file.
     * @param offset The number of bytes to be skipped.
     * @param limit The number of bytes to be returned.
     * @param precise Disable some checks on limit and offset values, useful for example to stream videos by keyframes.
     * Ignored if downloading file is web.
     * @return A {@link Flux} emitting full or parts of downloading file.
     */
    public Flux<FilePart> downloadFile(FileReferenceId fileRefId, long offset, int limit, boolean precise) {
        return Flux.defer(() -> {
            if (fileRefId.getFileType() == FileReferenceId.Type.WEB_DOCUMENT) {
                if (authResources.isBot()) {
                    return Flux.error(new IllegalArgumentException("Bot accounts can't download web files"));
                }
                if (fileRefId.getAccessHash() == -1) {
                    return Flux.error(new IllegalArgumentException("Web document without access hash"));
                }
                return serviceHolder.getUploadService()
                        .getWebFile(fileRefId.asWebLocation().orElseThrow(), Math.toIntExact(offset), limit)
                        .map(FilePart::ofWebFile);
            }

            return serviceHolder.getUploadService().getFile(fileRefId, offset, limit, precise)
                    .map(FilePart::ofFile);
        });
    }

    /**
     * Request to delete messages in DM or group chats.
     *
     * @param revoke Whether to delete messages for all participants of the chat.
     * @param ids An {@link Iterable} of message ids.
     * @return A {@link Mono} emitting on successful completion {@link AffectedMessages} with range of affected <b>common</b> events.
     */
    public Mono<AffectedMessages> deleteMessages(boolean revoke, Iterable<Integer> ids) {
        var request = ImmutableDeleteMessages.of(revoke ? ImmutableDeleteMessages.REVOKE_MASK : 0, ids);
        return mtProtoClientGroup.send(DcId.main(), request);
    }

    /**
     * Request to delete messages in channel.
     *
     * @param channelId The id of channel where need to delete messages.
     * @param ids An {@link Iterable} of message ids.
     * @return A {@link Mono} emitting on successful completion {@link AffectedMessages} with range of affected <b>channel</b> events.
     */
    public Mono<AffectedMessages> deleteChannelMessages(Id channelId, Iterable<Integer> ids) {
        return asInputChannelExact(channelId)
                .flatMap(p -> mtProtoClientGroup.send(DcId.main(),
                                telegram4j.tl.request.channels.ImmutableDeleteMessages.of(p, ids)));
    }

    /**
     * Request to get all commands by specified scope and language.
     *
     * @param spec The specification which represents a command scope.
     * @param langCode The ISO 639-1 language code for commands
     * @return A {@link Mono} emitting on successful completion immutable list of bot commands.
     */
    public Mono<List<BotCommand>> getCommands(BotCommandScopeSpec spec, String langCode) {
        return spec.resolve(this)
                .flatMap(scope -> mtProtoClientGroup.send(DcId.main(),
                        ImmutableGetBotCommands.of(scope, langCode)));
    }

    /**
     * Request to delete all commands by specified scope and language.
     *
     * @param spec The specification which represents a command scope.
     * @param langCode The ISO 639-1 language code for commands
     * @return A {@link Mono} emitting on successful completion boolean result state.
     */
    public Mono<Boolean> resetCommands(BotCommandScopeSpec spec, String langCode) {
        return spec.resolve(this)
                .flatMap(scope -> mtProtoClientGroup.send(DcId.main(),
                        ImmutableResetBotCommands.of(scope, langCode)));
    }

    /**
     * Request to rewrite all commands by specified scope and language.
     *
     * @param spec The specification which represents a command scope.
     * @param langCode The ISO 639-1 language code for commands
     * @param commands An iterable of bot commands to update.
     * @return A {@link Mono} emitting on successful completion boolean result state.
     */
    public Mono<Boolean> setCommands(BotCommandScopeSpec spec, String langCode, Iterable<? extends BotCommand> commands) {
        return spec.resolve(this)
                .flatMap(scope -> mtProtoClientGroup.send(DcId.main(),
                        ImmutableSetBotCommands.of(scope, langCode, commands)));
    }

    /**
     * Requests to retrieve {@link Sticker custom emoji} by specified id.
     *
     * @param id The id of sticker.
     * @return A {@link Mono} emitting on successful completion {@link Sticker custom emoji}.
     */
    public Mono<Sticker> getCustomEmoji(long id) {
        return serviceHolder.getChatService().getCustomEmojiDocuments(List.of(id))
                .mapNotNull(d -> d.isEmpty() ? null : d.get(0))
                .ofType(BaseDocument.class)
                .map(d -> (Sticker) EntityFactory.createDocument(this, d, Context.noOpContext()));
    }

    /**
     * Requests to retrieve {@link Sticker custom emojis} by specified ids.
     *
     * @param ids An {@link Iterable} with ids.
     * @return A {@link Flux} which continually emits a {@link Sticker custom emojis}.
     */
    public Flux<Sticker> getCustomEmojis(Iterable<Long> ids) {
        return serviceHolder.getChatService().getCustomEmojiDocuments(ids)
                .flatMapIterable(Function.identity())
                .ofType(BaseDocument.class)
                .map(d -> (Sticker) EntityFactory.createDocument(this, d, Context.noOpContext()));
    }

    /**
     * Requests to retrieve full sticker set by specified id.
     *
     * @param id The id of sticker set.
     * @return A {@link Mono} emitting on successful completion {@link AuxiliaryStickerSet full sticker set info}.
     */
    public Mono<AuxiliaryStickerSet> getStickerSet(InputStickerSet id) {
        return getStickerSet(id, 0);
    }

    /**
     * Requests to retrieve full sticker set by specified id.
     *
     * @param id The id of sticker set.
     * @param hash The pagination hash.
     * @return A {@link Mono} emitting on successful completion {@link AuxiliaryStickerSet full sticker set info}.
     */
    public Mono<AuxiliaryStickerSet> getStickerSet(InputStickerSet id, int hash) {
        return serviceHolder.getChatService().getStickerSet(id, hash)
                .map(data -> AuxiliaryEntityFactory.createStickerSet(this, data));
    }

    // Utility methods
    // ===========================

    /**
     * Refresh {@link FileReferenceId} file reference and access hash from specified context.
     * Low-quality chat photos (Files with type {@link FileReferenceId.Type#CHAT_PHOTO}) photos will be refreshed as normal photos.
     *
     * @apiNote File ref ids with type {@link FileReferenceId.Type#STICKER_SET_THUMB} don't require refreshing.
     *
     * @param fileRefId The {@link FileReferenceId}.
     * @return A {@link Mono} that emitting on successful completion refreshed {@link FileReferenceId}.
     */
    public Mono<FileReferenceId> refresh(FileReferenceId fileRefId) {
        return Mono.defer(() -> switch (fileRefId.getFileType()) {
            case CHAT_PHOTO: {
                var chatCtx = (ProfilePhotoContext) fileRefId.getContext();

                Id peerId = Id.of(chatCtx.getPeer(), getSelfId())
                        // The access hash must be invalided because it may be
                        // received from min users which have a special access hash
                        // which valid only for downloading
                        .withAccessHash(null);
                yield asInputPeerExact(peerId)
                        .flatMap(peer -> switch (peerId.getType()) {
                            case USER -> serviceHolder.getUserService()
                                    .getUserPhotos(TlEntityUtil.toInputUser(peer),
                                            0, -fileRefId.getDocumentId(), 1)
                                    .mapNotNull(p -> p.photos().isEmpty() ? null : p.photos().get(0))
                                    .ofType(BasePhoto.class)
                                    .map(p -> FileReferenceId.ofChatPhoto(p,
                                            Context.createUserPhotoContext(peer)));
                            case CHANNEL, CHAT -> {
                                var messageChatCtx = (ChatPhotoContext) chatCtx;
                                var msgId = messageChatCtx.getMessageId()
                                        .map(i -> List.of(ImmutableInputMessageID.of(i)))
                                        .orElse(null);

                                // TODO: check this
                                if (msgId == null) { // e.g. from GroupChat#getPhoto()
                                    yield Mono.just(fileRefId.withContext(
                                            Context.createChatPhotoContext(peer, -1)));
                                }
                                int msgIdRaw = messageChatCtx.getMessageId().orElseThrow();
                                yield withRetrievalStrategy(EntityRetrievalStrategy.RPC)
                                        .getMessages(Id.of(peer, getSelfId()), msgId)
                                        .flatMap(c -> Mono.justOrEmpty(findMessageAction(c, msgIdRaw)
                                                .map(f -> f.withContext(Context.createChatPhotoContext(
                                                        peer, msgIdRaw)))));
                            }
                        });
            }
            case DOCUMENT:
                if (fileRefId.getDocumentType().orElseThrow() == FileReferenceId.DocumentType.EMOJI) {
                    yield getCustomEmoji(fileRefId.getDocumentId()).map(Sticker::getFileReferenceId);
                }
            case WEB_DOCUMENT:
            case PHOTO: // message id must be present
                yield switch (fileRefId.getContext().getType()) {
                    case MESSAGE_MEDIA -> {
                        var ctx = (MessageMediaContext) fileRefId.getContext();
                        Id chatPeer = Id.of(ctx.getChatPeer());
                        yield withRetrievalStrategy(EntityRetrievalStrategy.RPC)
                                .getMessages(chatPeer, List.of(ImmutableInputMessageID.of(ctx.getMessageId())))
                                .flatMap(b -> Mono.justOrEmpty(findMessageMedia(b, fileRefId, ctx)));
                    }
                    case MESSAGE_ACTION -> {
                        var ctx = (MessageActionContext) fileRefId.getContext();
                        Id chatPeer = Id.of(ctx.getChatPeer());
                        yield withRetrievalStrategy(EntityRetrievalStrategy.RPC)
                                .getMessages(chatPeer, List.of(ImmutableInputMessageID.of(ctx.getMessageId())))
                                .flatMap(b -> Mono.justOrEmpty(findMessageAction(b, ctx.getMessageId())));
                    }
                    case BOT_INFO -> {
                        var ctx = (BotInfoContext) fileRefId.getContext();
                        Id chatPeer = Id.of(ctx.getChatPeer());
                        yield switch (chatPeer.getType()) {
                            case CHAT, CHANNEL -> {
                                Id botId = Id.ofUser(ctx.getBotId());
                                yield withRetrievalStrategy(EntityRetrievalStrategy.RPC)
                                        .getChatFullById(chatPeer)
                                        .map(c -> {
                                            if (c instanceof SupergroupChat sg) { // TODO: common interface
                                                return sg.getBotInfo();
                                            }
                                            return ((GroupChat) c).getBotInfo();
                                        })
                                        .flatMap(u -> Mono.justOrEmpty(u.flatMap(list -> list.stream()
                                                .filter(b -> botId.equals(b.getBotId()))
                                                .findFirst()
                                                .flatMap(BotInfo::getDescriptionDocument)
                                                .map(Document::getFileReferenceId)
                                                // Need to select correct file, because getDescriptionDocument()
                                                // can return Photo or Document objects
                                                .filter(doc -> doc.getFileType() == fileRefId.getFileType()))));
                            }
                            case USER -> withRetrievalStrategy(EntityRetrievalStrategy.RPC)
                                    .getUserFullById(chatPeer)
                                    .flatMap(u -> Mono.justOrEmpty(u.getBotInfo()
                                            .flatMap(BotInfo::getDescriptionDocument)
                                            .map(Document::getFileReferenceId)
                                            .filter(doc -> doc.getFileType() == fileRefId.getFileType())));
                        };
                    }
                    case STICKER_SET -> {
                        var ctx = (StickerSetContext) fileRefId.getContext();
                        yield getStickerSet(ctx.getStickerSet(), 0)
                                .flatMap(st -> Mono.justOrEmpty(st.getSticker(fileRefId.getDocumentId())))
                                .map(Document::getFileReferenceId);
                    }
                    // handled above
                    case PROFILE_PHOTO, CHAT_PHOTO, UNKNOWN -> Mono.error(new IllegalStateException());
                };
            // No need refresh
            case STICKER_SET_THUMB: yield Mono.just(fileRefId);
        });
    }

    /**
     * Refresh {@link FileReferenceId} file reference and access hash from specified context.
     * Low-quality chat photos (Files with type {@link FileReferenceId.Type#CHAT_PHOTO}) photos will be refreshed as normal photos.
     *
     * @apiNote File ref ids with type {@link FileReferenceId.Type#STICKER_SET_THUMB} don't require refreshing.
     *
     * @param fileReferenceId The serialized {@link FileReferenceId}.
     * @return A {@link Mono} that emitting on successful completion refreshed {@link FileReferenceId}.
     */
    public Mono<FileReferenceId> refresh(String fileReferenceId) {
        return Mono.fromCallable(() -> FileReferenceId.deserialize(fileReferenceId))
                .flatMap(this::refresh);
    }

    /**
     * Converts specified {@link Id} into the low-leveled {@link InputPeer} object and
     * assigns access hash from cache if present and applicable.
     *
     * @param peerId The id of user/chat/channel to converting.
     * @return A {@link Mono}, emitting on successful completion resolved {@link InputPeer} object.
     */
    public Mono<InputPeer> asInputPeer(Id peerId) {
        return switch (peerId.getType()) {
            case USER -> asInputUser(peerId).map(TlEntityUtil::toInputPeer);
            case CHAT -> Mono.just(ImmutableInputPeerChat.of(peerId.asLong()));
            case CHANNEL -> asInputChannel(peerId).map(TlEntityUtil::toInputPeer);
        };
    }

    public Mono<InputPeer> asInputPeerExact(Id peerId) {
        return asInputPeer(peerId)
                .switchIfEmpty(MappingUtil.unresolvedPeer(peerId));
    }

    /**
     * Converts specified {@link Id} into the low-leveled {@link InputChannel} object and
     * assigns access hash from cache if present.
     *
     * @throws IllegalArgumentException If given id has min user/channel
     * information but current client authorized for bot account.
     * @param channelId The id of channel to converting.
     * @return A {@link Mono}, emitting on successful completion resolved {@link InputChannel} object.
     */
    public Mono<InputChannel> asInputChannel(Id channelId) {
        if (channelId.getType() != Id.Type.CHANNEL) {
            return Mono.error(new IllegalArgumentException("Specified id must be channel-typed: " + channelId));
        }

        var min = channelId.getMinInformation().orElse(null);
        if (min != null) {
            if (authResources.isBot()) {
                return Mono.error(new IllegalArgumentException("Min ids can not be used for bots"));
            }

            return asInputPeerExact(min.getPeerId())
                    .map(p -> ImmutableInputChannelFromMessage.of(p, min.getMessageId(), channelId.asLong()));
        }

        return Mono.justOrEmpty(channelId.getAccessHash())
                .<InputChannel>map(acc -> ImmutableBaseInputChannel.of(channelId.asLong(), acc))
                .switchIfEmpty(mtProtoResources.getStoreLayout().resolveChannel(channelId.asLong()));
    }

    public Mono<InputChannel> asInputChannelExact(Id channelId) {
        return asInputChannel(channelId)
                .switchIfEmpty(MappingUtil.unresolvedPeer(channelId));
    }

    /**
     * Converts specified {@link Id} into the low-leveled {@link InputUser} object and
     * assigns access hash from cache if present.
     *
     * @throws IllegalArgumentException If given id has min user/channel
     * information but current client authorized for bot account.
     * @param userId The id of user to converting.
     * @return A {@link Mono}, emitting on successful completion resolved {@link InputUser} object.
     */
    public Mono<InputUser> asInputUser(Id userId) {
        if (userId.getType() != Id.Type.USER) {
            return Mono.error(new IllegalArgumentException("Specified id must be user-typed: " + userId));
        }

        if (userId.equals(getSelfId())) {
            return Mono.just(InputUserSelf.instance());
        }

        var min = userId.getMinInformation().orElse(null);
        if (min != null) {
            if (authResources.isBot()) {
                return Mono.error(new IllegalArgumentException("Min ids can not be used for bots"));
            }

            return asInputPeerExact(min.getPeerId())
                    .map(p -> ImmutableInputUserFromMessage.of(p, min.getMessageId(), userId.asLong()));
        }

        return Mono.justOrEmpty(userId.getAccessHash())
                .<InputUser>map(acc -> ImmutableBaseInputUser.of(userId.asLong(), acc))
                .switchIfEmpty(mtProtoResources.getStoreLayout().resolveUser(userId.asLong()));
    }

    public Mono<InputUser> asInputUserExact(Id userId) {
        return asInputUser(userId)
                .switchIfEmpty(MappingUtil.unresolvedPeer(userId));
    }

    /**
     * Converts specified {@link Id} into the low-leveled {@link InputPeer} object without access hash resolving.
     *
     * @throws IllegalArgumentException If given id has min user/channel
     * information but current client authorized for bot account.
     * @throws NoSuchElementException If access hash is needed and absent for given id.
     * @param peerId The id of the peer to converting.
     * @return The new {@link InputPeer} from specified {@link Id}.
     */
    public InputPeer asResolvedInputPeer(Id peerId) {
        return switch (peerId.getType()) {
            case USER -> {
                if (getSelfId().equals(peerId)) {
                    yield InputPeerSelf.instance();
                }
                yield peerId.getMinInformation()
                        .<InputPeer>map(min -> {
                            Preconditions.requireArgument(!authResources.isBot(), "Min ids can not be used for bots");

                            InputPeer p = asResolvedInputPeer(min.getPeerId());
                            return ImmutableInputPeerUserFromMessage.of(p, min.getMessageId(), peerId.asLong());
                        })
                        .or(() -> peerId.getAccessHash().map(acc -> ImmutableInputPeerUser.of(peerId.asLong(), acc)))
                        .orElseThrow(() -> new IllegalArgumentException("No access hash present for id: " + peerId));
            }
            case CHAT -> ImmutableInputPeerChat.of(peerId.asLong());
            case CHANNEL -> peerId.getMinInformation()
                    .<InputPeer>map(min -> {
                        Preconditions.requireArgument(!authResources.isBot(), "Min ids can not be used for bots");

                        InputPeer p = asResolvedInputPeer(min.getPeerId());
                        return ImmutableInputPeerChannelFromMessage.of(p, min.getMessageId(), peerId.asLong());
                    })
                    .or(() -> peerId.getAccessHash().map(acc -> ImmutableInputPeerChannel.of(peerId.asLong(), acc)))
                    .orElseThrow(() -> new IllegalArgumentException("No access hash present for id: " + peerId));
        };
    }

    /**
     * Converts specified {@link Id} into the low-leveled {@link InputUser} object without access hash resolving.
     *
     * @throws IllegalArgumentException If given id has a type other than {@link Id.Type#USER}
     * or id has min user/channel information but current client authorized for bot account.
     * @throws NoSuchElementException If access hash is needed and absent for given id.
     * @param userId The id of user to converting.
     * @return The new {@link InputPeer} from specified {@link Id}.
     */
    public InputUser asResolvedInputUser(Id userId) {
        Preconditions.requireArgument(userId.getType() == Id.Type.USER, () ->
                "Unexpected type of userId: " + userId);
        if (userId.equals(getSelfId())) {
            return InputUserSelf.instance();
        }

        return userId.getMinInformation()
                .<InputUser>map(min -> {
                    Preconditions.requireArgument(!authResources.isBot(), "Min ids can not be used for bots");

                    InputPeer p = asResolvedInputPeer(min.getPeerId());
                    return ImmutableInputUserFromMessage.of(p, min.getMessageId(), userId.asLong());
                })
                .or(() -> userId.getAccessHash().map(acc -> ImmutableBaseInputUser.of(userId.asLong(), acc)))
                .orElseThrow(() -> new IllegalArgumentException("No access hash present for id: " + userId));
    }

    /**
     * Converts specified {@link Id} into the low-leveled {@link InputChannel} object without access hash resolving.
     *
     * @throws IllegalArgumentException If given id has a type other than {@link Id.Type#CHANNEL}
     * or id has min user/channel information but current client authorized for bot account.
     * @throws NoSuchElementException If access hash is needed and absent for given id.
     * @param channelId The id of channel to converting.
     * @return The new {@link InputChannel} from specified {@link Id}.
     */
    public InputChannel asResolvedInputChannel(Id channelId) {
        Preconditions.requireArgument(channelId.getType() == Id.Type.CHANNEL, () -> "Unexpected type of userId: " + channelId);

        return channelId.getMinInformation()
                .<InputChannel>map(min -> {
                    Preconditions.requireArgument(!authResources.isBot(), "Min ids can not be used for bots");

                    InputPeer p = asResolvedInputPeer(min.getPeerId());
                    return ImmutableInputChannelFromMessage.of(p, min.getMessageId(), channelId.asLong());
                })
                .or(() -> channelId.getAccessHash().map(acc -> ImmutableBaseInputChannel.of(channelId.asLong(), acc)))
                .orElseThrow(() -> new IllegalArgumentException("No access hash present for id: " + channelId));
    }

    /**
     * Applies the given retrieval strategy to retrieve objects using this {@link MTProtoTelegramClient}.
     *
     * @param strategy The retrieval strategy to apply.
     * @return A new {@code EntityRetriever} from strategy.
     */
    public EntityRetriever withRetrievalStrategy(EntityRetrievalStrategy strategy) {
        return strategy.apply(this);
    }

    // copied from https://github.com/tdlib/td/blob/c1a3fa633fbce67b8b89fee93130498db8adc039/td/telegram/ContactsManager.cpp#L5273

    /**
     * Gets id of special bot that is used to anonymize admins in groups if {@link AdminRight#ANONYMOUS} right is set.
     *
     * @return The id of bot that is used for anonymous admins.
     */
    public Id getGroupAnonymousBotId() {
        return Id.ofUser(mtProtoClientGroup.main().dc().isTest() ? 552888 : 1087968824, 0L);
    }

    /**
     * Gets id of special bot that is used as author of channel posts.
     *
     * @return The id of bot that is used for anonymous admins.
     */
    public Id getChannelBotId() {
        return Id.ofUser(mtProtoClientGroup.main().dc().isTest() ? 936174 : 136817688, 0L);
    }

    /**
     * Gets id of special bot that helps keep track of replies to your comments in channels.
     *
     * @return The id of bot that used to keep track of replies to your comments in channels.
     */
    public Id getRepliesBotId() {
        return Id.ofUser(mtProtoClientGroup.main().dc().isTest() ? 708513 : 1271266957, 0L);
    }

    /**
     * Gets id of service user that used for system notifications.
     *
     * @return The id of service user that used for system notifications.
     */
    public Id getServiceNotificationId() {
        return Id.ofUser(777000, 0L);
    }

    // EntityRetriever methods
    // ===========================

    @Override
    public Mono<PeerEntity> resolvePeer(PeerId peerId) {
        return entityRetriever.resolvePeer(peerId);
    }

    @Override
    public Mono<User> getUserById(Id userId) {
        return entityRetriever.getUserById(userId);
    }

    @Override
    public Mono<User> getUserMinById(Id userId) {
        return entityRetriever.getUserMinById(userId);
    }

    @Override
    public Mono<User> getUserFullById(Id userId) {
        return entityRetriever.getUserFullById(userId);
    }

    @Override
    public Mono<Chat> getChatById(Id chatId) {
        return entityRetriever.getChatById(chatId);
    }

    @Override
    public Mono<Chat> getChatMinById(Id chatId) {
        return entityRetriever.getChatMinById(chatId);
    }

    @Override
    public Mono<Chat> getChatFullById(Id chatId) {
        return entityRetriever.getChatFullById(chatId);
    }

    @Override
    public Mono<ChatParticipant> getParticipantById(Id chatId, Id peerId) {
        return entityRetriever.getParticipantById(chatId, peerId);
    }

    @Override
    public Flux<ChatParticipant> getParticipants(Id chatId) {
        return entityRetriever.getParticipants(chatId);
    }

    @Override
    public Mono<AuxiliaryMessages> getMessages(@Nullable Id chatId, Iterable<? extends InputMessage> messageIds) {
        return entityRetriever.getMessages(chatId, messageIds);
    }

    // Internal methods
    // ===========================

    private static Optional<FileReferenceId> findMessageAction(AuxiliaryMessages messages, int messageId) {
        return messages.getMessages().stream()
                .filter(msg -> msg.getId() == messageId)
                .findFirst()
                .flatMap(Message::getAction)
                .flatMap(action -> {
                    if (action instanceof MessageAction.UpdateChatPhoto c) {
                        return c.getCurrentPhoto();
                    } else if (action instanceof MessageAction.SuggestProfilePhoto c) {
                        return Optional.of(c.getPhoto());
                    } else {
                        throw new IllegalStateException("Unexpected MessageAction type: " + action);
                    }
                })
                .map(Document::getFileReferenceId);
    }

    private static Optional<FileReferenceId> findMessageMedia(AuxiliaryMessages messages, FileReferenceId original,
                                                       MessageMediaContext context) {
        return messages.getMessages().stream()
                .filter(msg -> msg.getId() == context.getMessageId())
                .findFirst()
                .flatMap(Message::getMedia)
                .flatMap(media -> {
                    if (media instanceof MessageMedia.Document c) {
                        return c.getDocument();
                    } else if (media instanceof MessageMedia.Invoice c) {
                        return c.getPhoto();
                    } else if (media instanceof MessageMedia.Game c) {
                        if (original.getFileType() == FileReferenceId.Type.PHOTO) {
                            return Optional.of(c.getGame().getPhoto());
                        }
                        return c.getGame().getDocument();
                    } else {
                        throw new IllegalStateException("Unexpected MessageMedia type: " + media);
                    }
                })
                .map(Document::getFileReferenceId);
    }

    private static Flux<ByteBuf> fromPath(Path path, int maxChunkSize, ByteBufAllocator allocator) {
        return Flux.generate(() -> FileChannel.open(path), (fc, sink) -> {
            ByteBuf buf = allocator.buffer();
            try {
                if (buf.writeBytes(fc, maxChunkSize) < 0) {
                    buf.release();
                    sink.complete();
                } else {
                    sink.next(buf);
                }
            } catch (IOException e) {
                buf.release();
                sink.error(e);
            }
            return fc;
        }, MTProtoTelegramClient::closeFileChannel);
    }

    private static void closeFileChannel(FileChannel fc) {
        try {
            fc.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
