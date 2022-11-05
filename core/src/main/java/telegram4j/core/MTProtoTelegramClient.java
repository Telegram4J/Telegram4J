package telegram4j.core;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.ByteBufFlux;
import reactor.util.annotation.Nullable;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.auxiliary.AuxiliaryStickerSet;
import telegram4j.core.event.UpdatesManager;
import telegram4j.core.event.domain.Event;
import telegram4j.core.internal.AuxiliaryEntityFactory;
import telegram4j.core.internal.EntityFactory;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.internal.Preconditions;
import telegram4j.core.object.Document;
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
import telegram4j.mtproto.MTProtoClientGroup;
import telegram4j.mtproto.MTProtoOptions;
import telegram4j.mtproto.file.*;
import telegram4j.mtproto.service.ServiceHolder;
import telegram4j.mtproto.service.UploadOptions;
import telegram4j.mtproto.service.UploadService;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;
import telegram4j.tl.messages.AffectedMessages;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;

public final class MTProtoTelegramClient implements EntityRetriever {
    private final AuthorizationResources authResources;
    private final MTProtoClientGroup mtProtoClientGroup;
    private final MTProtoResources mtProtoResources;
    private final UpdatesManager updatesManager;
    private final Id[] selfIdHolder;
    private final ServiceHolder serviceHolder;
    private final EntityRetriever entityRetriever;
    private final Mono<Void> onDisconnect;

    MTProtoTelegramClient(AuthorizationResources authResources,
                          MTProtoClientGroup mtProtoClientGroup, MTProtoResources mtProtoResources,
                          Function<MTProtoTelegramClient, UpdatesManager> updatesManager,
                          Id[] selfIdHolder, ServiceHolder serviceHolder,
                          EntityRetrievalStrategy entityRetriever,
                          Mono<Void> onDisconnect) {
        this.authResources = authResources;
        this.mtProtoClientGroup = mtProtoClientGroup;
        this.mtProtoResources = mtProtoResources;
        this.serviceHolder = serviceHolder;
        this.selfIdHolder = selfIdHolder;
        this.entityRetriever = entityRetriever.apply(this);
        this.updatesManager = updatesManager.apply(this);
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
    public static MTProtoBootstrap<MTProtoOptions> create(int apiId, String apiHash, String botAuthToken) {
        return new MTProtoBootstrap<>(Function.identity(), new AuthorizationResources(apiId, apiHash, botAuthToken));
    }

    /**
     * Creates client builder with user authorization schematic.
     *
     * @see <a href="https://core.telegram.org/api/auth">User authorization</a>
     * @see <a href="https://core.telegram.org/api/obtaining_api_id#obtaining-api-id">Obtaining Api Id</a>
     * @param apiId The api id.
     * @param apiHash The api hash.
     * @param authHandler The user authorization implementation. QR or code handler.
     * @return The new {@link MTProtoBootstrap} client builder.
     */
    public static MTProtoBootstrap<MTProtoOptions> create(int apiId, String apiHash,
                                                          Function<MTProtoTelegramClient, Publisher<?>> authHandler) {
        return new MTProtoBootstrap<>(Function.identity(), new AuthorizationResources(apiId, apiHash, authHandler));
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
     * Gets id of <i>current</i> user.
     *
     * @return The id of <i>current</i> user.
     */
    public Id getSelfId() {
        return Objects.requireNonNull(selfIdHolder[0]);
    }

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
            if (type == Chat.Type.CHANNEL)
                return serviceHolder.getBotService().setBotBroadcastDefaultAdminRights(chatAdminRights);
            return serviceHolder.getBotService().setBotGroupDefaultAdminRights(chatAdminRights);
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
                .publishOn(Schedulers.boundedElastic())
                .flatMap(size -> {
                    int ps = UploadService.suggestPartSize(size, partSize);
                    return serviceHolder.getUploadService()
                            .saveFile(UploadOptions.builder()
                                    .data(ByteBufFlux.fromPath(path, ps))
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
     * @param fileReferenceId The serialized {@link FileReferenceId} of file.
     * @return A {@link Flux} emitting full or parts of downloading file.
     */
    public Flux<FilePart> downloadFile(String fileReferenceId) {
        return Mono.fromCallable(() -> FileReferenceId.deserialize(fileReferenceId))
                .flatMapMany(this::downloadFile);
    }

    /**
     * Request to download file by their reference from Telegram Media DC or
     * if file is {@link Document#isWeb() web} and haven't telegram-proxying or download is invoked on bot account
     * emit {@link IllegalStateException} exception.
     *
     * @param loc The location of file.
     * @return A {@link Flux} emitting full or parts of downloading file.
     */
    public Flux<FilePart> downloadFile(FileReferenceId loc) {
        return Flux.defer(() -> {
            if (loc.getFileType() == FileReferenceId.Type.WEB_DOCUMENT) {
                if (loc.getAccessHash() == -1) {
                    return Flux.error(new IllegalStateException("Web document without access hash"));
                }

                if (authResources.isBot()) {
                    return Flux.error(new IllegalStateException("Bot accounts can't download web files"));
                }
                return serviceHolder.getUploadService()
                        .getWebFile(loc.asWebLocation().orElseThrow())
                        .map(FilePart::ofWebFile);
            }

            return serviceHolder.getUploadService()
                    .getFile(loc.asLocation().orElseThrow())
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
        return serviceHolder.getChatService().deleteMessages(revoke, ids);
    }

    /**
     * Request to delete messages in channel.
     *
     * @param channelId The id of channel where need to delete messages.
     * @param ids An {@link Iterable} of message ids.
     * @return A {@link Mono} emitting on successful completion {@link AffectedMessages} with range of affected <b>channel</b> events.
     */
    public Mono<AffectedMessages> deleteChannelMessages(Id channelId, Iterable<Integer> ids) {
        return asInputChannel(channelId) // contains check of id type
                .switchIfEmpty(MappingUtil.unresolvedPeer(channelId))
                .flatMap(p -> serviceHolder.getChatService()
                        .deleteMessages(p, ids));
    }

    /**
     * Request to get all commands by specified scope and language.
     *
     * @param spec The specification which represents a command scope.
     * @param langCode The ISO 639-1 language code for commands
     * @return A {@link Mono} emitting on successful completion immutable list of bot commands.
     */
    public Mono<List<BotCommand>> getCommands(BotCommandScopeSpec spec, String langCode) {
        return spec.asData(this)
                .flatMap(scope -> serviceHolder.getBotService()
                        .getBotCommands(scope, langCode));
    }

    /**
     * Request to delete all commands by specified scope and language.
     *
     * @param spec The specification which represents a command scope.
     * @param langCode The ISO 639-1 language code for commands
     * @return A {@link Mono} emitting on successful completion boolean result state.
     */
    public Mono<Boolean> resetCommands(BotCommandScopeSpec spec, String langCode) {
        return spec.asData(this)
                .flatMap(scope -> serviceHolder.getBotService()
                        .resetBotCommands(scope, langCode));
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
        return spec.asData(this)
                .flatMap(scope -> serviceHolder.getBotService()
                        .setBotCommands(scope, langCode, commands));
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
        return Mono.defer(() -> {
            switch (fileRefId.getFileType()) {
                case CHAT_PHOTO: {
                    var chatCtx = (ProfilePhotoContext) fileRefId.getContext();

                    Id peerId = Id.of(chatCtx.getPeer(), getSelfId());
                    return asInputPeer(peerId)
                            .switchIfEmpty(MappingUtil.unresolvedPeer(peerId))
                            .flatMap(peer -> {
                                switch (peerId.getType()) {
                                    case USER:
                                        return serviceHolder.getUserService()
                                                .getUserPhotos(TlEntityUtil.toInputUser(peer),
                                                        0, -fileRefId.getDocumentId(), 1)
                                                .mapNotNull(p -> p.photos().isEmpty() ? null : p.photos().get(0))
                                                .ofType(BasePhoto.class)
                                                .map(p -> FileReferenceId.ofChatPhoto(p,
                                                        Context.createUserPhotoContext(peer)));
                                    case CHANNEL:
                                    case CHAT:
                                        var messageChatCtx = (ChatPhotoContext) chatCtx;

                                        var msgId = messageChatCtx.getMessageId()
                                                .map(i -> List.of(ImmutableInputMessageID.of(i)))
                                                .orElse(null);

                                        // TODO: check this
                                        if (msgId == null) { // e.g. from GroupChat#getPhoto()
                                            return Mono.just(fileRefId.withContext(
                                                    Context.createChatPhotoContext(peer, -1)));
                                        }

                                        return withRetrievalStrategy(EntityRetrievalStrategy.RPC)
                                                .getMessages(Id.of(peer, getSelfId()), msgId)
                                                .flatMap(c -> findChatPhoto(c, peer, messageChatCtx.getMessageId().orElseThrow()));
                                    default:
                                        return Mono.error(new IllegalStateException());
                                }
                            });
                }
                case WEB_DOCUMENT:
                case DOCUMENT:
                    var docType = fileRefId.getDocumentType().orElseThrow();
                    if (docType == FileReferenceId.DocumentType.EMOJI) {
                        return getCustomEmoji(fileRefId.getDocumentId())
                                .map(Sticker::getFileReferenceId);
                    }

                case PHOTO: // message id must be present
                    switch (fileRefId.getContext().getType()) {
                        case MESSAGE_MEDIA: {
                            var ctx = (MessageMediaContext) fileRefId.getContext();
                            Id chatPeer = Id.of(ctx.getChatPeer());
                            return withRetrievalStrategy(EntityRetrievalStrategy.RPC)
                                    .getMessages(chatPeer, List.of(ImmutableInputMessageID.of(ctx.getMessageId())))
                                    .flatMap(b -> findMessageMedia(b, fileRefId, ctx));
                        }
                        case BOT_INFO: {
                            var ctx = (BotInfoContext) fileRefId.getContext();
                            Id chatPeer = Id.of(ctx.getChatPeer());
                            switch (chatPeer.getType()) {
                                case CHANNEL:
                                case CHAT:
                                    Id botId = Id.ofUser(ctx.getBotId());
                                    return withRetrievalStrategy(EntityRetrievalStrategy.RPC)
                                            .getChatFullById(chatPeer)
                                            .map(c -> {
                                                if (c instanceof SupergroupChat) { // TODO: common interface
                                                    var sg = (SupergroupChat) c;
                                                    return sg.getBotInfo();
                                                }
                                                return ((GroupChat) c).getBotInfo();
                                            })
                                            .flatMap(u -> Mono.justOrEmpty(u.flatMap(list -> list.stream()
                                                    .filter(b -> botId.equals(b.getBotId()))
                                                    .findFirst())))
                                            .flatMap(b -> Mono.justOrEmpty(b.getDescriptionDocument()))
                                            .map(Document::getFileReferenceId);
                                case USER:
                                    return withRetrievalStrategy(EntityRetrievalStrategy.RPC)
                                            .getUserFullById(chatPeer)
                                            .flatMap(u -> Mono.justOrEmpty(u.getBotInfo()))
                                            .flatMap(b -> Mono.justOrEmpty(b.getDescriptionDocument()))
                                            .map(Document::getFileReferenceId);
                                default:
                                    return Mono.error(new IllegalStateException());
                            }
                        }
                        case STICKER_SET:
                            var ctx = (StickerSetContext) fileRefId.getContext();
                            return getStickerSet(ctx.getStickerSet(), 0)
                                    .flatMap(st -> Mono.justOrEmpty(st.getSticker(fileRefId.getDocumentId())))
                                    .map(Document::getFileReferenceId);
                        case PROFILE_PHOTO:
                        case CHAT_PHOTO:
                            return Mono.error(new IllegalStateException()); // handled above
                    }
                // No need refresh
                case STICKER_SET_THUMB: return Mono.just(fileRefId);
                default: return Mono.error(new IllegalStateException());
            }
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
     * @param chatId The id of user/chat/channel to converting.
     * @return A {@link Mono}, emitting on successful completion resolved {@link InputPeer} object.
     */
    public Mono<InputPeer> asInputPeer(Id chatId) {
        switch (chatId.getType()) {
            case USER: return asInputUser(chatId).map(TlEntityUtil::toInputPeer);
            case CHAT: return Mono.just(ImmutableInputPeerChat.of(chatId.asLong()));
            case CHANNEL: return asInputChannel(chatId).map(TlEntityUtil::toInputPeer);
            default: throw new IllegalStateException();
        }
    }

    /**
     * Converts specified {@link Id} into the low-leveled {@link InputChannel} object and
     * assigns access hash from cache if present.
     *
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

            return asInputPeer(min.getPeerId())
                    .switchIfEmpty(MappingUtil.unresolvedPeer(min.getPeerId()))
                    .map(p -> ImmutableInputChannelFromMessage.of(p, min.getMessageId(), channelId.asLong()));
        }

        return Mono.justOrEmpty(channelId.getAccessHash())
                .<InputChannel>map(acc -> ImmutableBaseInputChannel.of(channelId.asLong(), acc))
                .switchIfEmpty(mtProtoResources.getStoreLayout().resolveChannel(channelId.asLong()));
    }

    /**
     * Converts specified {@link Id} into the low-leveled {@link InputUser} object and
     * assigns access hash from cache if present.
     *
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

            return asInputPeer(min.getPeerId())
                    .switchIfEmpty(MappingUtil.unresolvedPeer(min.getPeerId()))
                    .map(p -> ImmutableInputUserFromMessage.of(p, min.getMessageId(), userId.asLong()));
        }

        return Mono.justOrEmpty(userId.getAccessHash())
                .<InputUser>map(acc -> ImmutableBaseInputUser.of(userId.asLong(), acc))
                .switchIfEmpty(mtProtoResources.getStoreLayout().resolveUser(userId.asLong()));
    }

    /**
     * Converts specified {@link Id} into the low-leveled {@link InputPeer} object without access hash resolving.
     *
     * @see #asInputPeer(Id)
     * @throws NoSuchElementException If access hash is needed and absent.
     * @param peerId The id of the peer to converting.
     * @return The new {@link InputPeer} from specified {@link Id}.
     */
    public InputPeer asResolvedInputPeer(Id peerId) {
        switch (peerId.getType()) {
            case USER:
                if (getSelfId().equals(peerId)) {
                    return InputPeerSelf.instance();
                }

                return peerId.getMinInformation()
                        .<InputPeer>map(min -> {
                            Preconditions.requireArgument(!authResources.isBot(), "Min ids can not be used for bots");

                            InputPeer p = asResolvedInputPeer(min.getPeerId());
                            return ImmutableInputPeerUserFromMessage.of(p, min.getMessageId(), peerId.asLong());
                        })
                        .or(() -> peerId.getAccessHash().map(acc -> ImmutableInputPeerUser.of(peerId.asLong(), acc)))
                        .orElseThrow(() -> new IllegalArgumentException("No access hash present for id: " + peerId));
            case CHAT: return ImmutableInputPeerChat.of(peerId.asLong());
            case CHANNEL:
                return peerId.getMinInformation()
                        .<InputPeer>map(min -> {
                            Preconditions.requireArgument(!authResources.isBot(), "Min ids can not be used for bots");

                            InputPeer p = asResolvedInputPeer(min.getPeerId());
                            return ImmutableInputPeerChannelFromMessage.of(p, min.getMessageId(), peerId.asLong());
                        })
                        .or(() -> peerId.getAccessHash().map(acc -> ImmutableInputPeerChannel.of(peerId.asLong(), acc)))
                        .orElseThrow(() -> new IllegalArgumentException("No access hash present for id: " + peerId));
            default: throw new IllegalStateException();
        }
    }

    /**
     * Converts specified {@link Id} into the low-leveled {@link InputUser} object without access hash resolving.
     *
     * @throws NoSuchElementException If access hash is needed and absent.
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
     * @throws NoSuchElementException If access hash is needed and absent.
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
     * @return The id of bot that used for anonymous admins.
     */
    public Id getGroupAnonymousBotId() {
        return Id.ofUser(mtProtoClientGroup.main().getDatacenter().isTest() ? 552888 : 1087968824, 0L);
    }

    /**
     * Gets id of special bot that helps keep track of replies to your comments in channels.
     *
     * @return The id of bot that used to keep track of replies to your comments in channels.
     */
    public Id getRepliesBotId() {
        return Id.ofUser(mtProtoClientGroup.main().getDatacenter().isTest() ? 708513 : 1271266957, 0L);
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

    private Mono<FileReferenceId> findChatPhoto(AuxiliaryMessages messages, InputPeer resolvedPeer, int messageId) {
        return Mono.justOrEmpty(messages.getMessages().stream()
                        .filter(msg -> msg.getId() == messageId)
                        .findFirst())
                .flatMap(msg -> Mono.justOrEmpty(msg.getAction()))
                .flatMap(action -> {
                    if (action instanceof MessageAction.UpdateChatPhoto) {
                        var cast = (MessageAction.UpdateChatPhoto) action;
                        return Mono.justOrEmpty(cast.getCurrentPhoto());
                    } else {
                        return Mono.error(new IllegalStateException("Unexpected MessageAction type: " + action));
                    }
                })
                .map(Document::getFileReferenceId)
                .map(f -> f.withContext(Context.createChatPhotoContext(resolvedPeer, messageId)));
    }

    private Mono<FileReferenceId> findMessageMedia(AuxiliaryMessages messages, FileReferenceId original,
                                                   MessageMediaContext context) {
        return Mono.justOrEmpty(messages.getMessages().stream()
                .filter(msg -> msg.getId() == context.getMessageId())
                .findFirst())
                .flatMap(msg -> Mono.justOrEmpty(msg.getMedia()))
                .flatMap(media -> {
                    if (media instanceof MessageMedia.Document) {
                        var cast = (MessageMedia.Document) media;
                        return Mono.justOrEmpty(cast.getDocument());
                    } else if (media instanceof MessageMedia.Invoice) {
                        var cast = (MessageMedia.Invoice) media;
                        return Mono.justOrEmpty(cast.getPhoto());
                    } else if (media instanceof MessageMedia.Game) {
                        var cast = (MessageMedia.Game) media;
                        if (original.getFileType() == FileReferenceId.Type.PHOTO) {
                            return Mono.just(cast.getGame().getPhoto());
                        }
                        return Mono.justOrEmpty(cast.getGame().getDocument());
                    } else {
                        return Mono.error(new IllegalStateException("Unexpected MessageMedia type: " + media));
                    }
                })
                .map(Document::getFileReferenceId);
    }
}
