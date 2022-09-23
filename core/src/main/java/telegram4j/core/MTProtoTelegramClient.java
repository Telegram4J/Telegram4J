package telegram4j.core;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.ByteBufFlux;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.event.UpdatesManager;
import telegram4j.core.event.domain.Event;
import telegram4j.core.object.Document;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.retriever.EntityRetriever;
import telegram4j.core.util.Id;
import telegram4j.core.util.PeerId;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.MTProtoOptions;
import telegram4j.mtproto.file.FilePart;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.mtproto.service.ServiceHolder;
import telegram4j.mtproto.service.UploadService;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;
import telegram4j.tl.api.TlEncodingUtil;
import telegram4j.tl.messages.AffectedMessages;
import telegram4j.tl.messages.BaseMessages;
import telegram4j.tl.messages.ChannelMessages;
import telegram4j.tl.messages.Messages;
import telegram4j.tl.storage.FileType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;

public final class MTProtoTelegramClient implements EntityRetriever {
    private final AuthorizationResources authResources;
    private final MTProtoClient mtProtoClient;
    private final MTProtoResources mtProtoResources;
    private final UpdatesManager updatesManager;
    private final Id[] selfIdHolder;
    private final ServiceHolder serviceHolder;
    private final EntityRetriever entityRetriever;
    private final Mono<Void> onDisconnect;

    MTProtoTelegramClient(AuthorizationResources authResources,
                          MTProtoClient mtProtoClient, MTProtoResources mtProtoResources,
                          Function<MTProtoTelegramClient, UpdatesManager> updatesManager,
                          Id[] selfIdHolder, ServiceHolder serviceHolder,
                          Function<MTProtoTelegramClient, EntityRetriever> entityRetriever,
                          Mono<Void> onDisconnect) {
        this.authResources = authResources;
        this.mtProtoClient = mtProtoClient;
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
     * @see <a href="https://core.telegram.org/bots#3-how-do-i-create-a-bot">Bots</a>
     * @see <a href="https://core.telegram.org/api/obtaining_api_id#obtaining-api-id">Obtaining Api Id</a>
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

    public MTProtoClient getMtProtoClient() {
        return mtProtoClient;
    }

    public ServiceHolder getServiceHolder() {
        return serviceHolder;
    }

    public Mono<Void> disconnect() {
        return mtProtoClient.close();
    }

    public Mono<Void> onDisconnect() {
        return onDisconnect;
    }

    public <E extends Event> Flux<E> on(Class<E> type) {
        return mtProtoResources.getEventDispatcher().on(type);
    }

    // Interaction methods
    // ===========================

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
                .map(Math::toIntExact)
                .flatMap(size -> uploadFile(ByteBufFlux.fromPath(path, partSize), filename, size, partSize));
    }

    /**
     * Request to upload file to Telegram Media DC.
     *
     * @param data The flux of {@link ByteBuf} to upload.
     * @param filename The name of remote file.
     * @param size The exact size of file.
     * @param partSize The part size for uploading, must be divisible by
     * {@link UploadService#MIN_PART_SIZE} and {@link UploadService#MAX_PART_SIZE} must be evenly divisible by part_size
     * @return A {@link Mono} emitting on successful completion {@link InputFile} with file id and MD5 hash if applicable.
     */
    public Mono<InputFile> uploadFile(ByteBufFlux data, String filename, int size, int partSize) {
        int ps = UploadService.suggestPartSize(size, partSize);
        return serviceHolder.getUploadService().saveFile(data, size, ps, filename);
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
     * if file {@link Document#isWeb()} and haven't telegram-proxying try to directly download file by url.
     * Method will return {@link Flux#empty()} when web-file id passed on bot accounts.
     *
     * @param loc The location of file.
     * @return A {@link Flux} emitting full or parts of downloading file.
     */
    public Flux<FilePart> downloadFile(FileReferenceId loc) {
        return Flux.defer(() -> {
            if (loc.getFileType() == FileReferenceId.Type.WEB_DOCUMENT) {
                if (authResources.isBot()) {
                    return Flux.empty();
                }

                if (loc.getAccessHash() == -1) { // Non-proxied file, just download via netty's HttpClient
                    return mtProtoResources.getHttpClient().orElseThrow()
                            .get().uri(loc.getUrl().orElseThrow())
                            .responseSingle((res, buf) -> buf
                                    .map(TlEncodingUtil::copyAsUnpooled)
                                    .map(bytes -> {
                                        String mimeType = res.responseHeaders().getAsString(HttpHeaderNames.CONTENT_TYPE);
                                        int size = res.responseHeaders().getInt(HttpHeaderNames.CONTENT_LENGTH, -1);
                                        FileType type = TlEntityUtil.suggestFileType(mimeType);
                                        return new FilePart(type, -1, bytes, size, mimeType);
                                    }))
                            .flux();
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
        if (channelId.getType() != Id.Type.CHANNEL) {
            return Mono.error(new IllegalArgumentException("Channel id type must be CHANNEL"));
        }

        return asInputChannel(channelId)
                .flatMap(p -> serviceHolder.getChatService()
                        .deleteMessages(p, ids));
    }

    // Utility methods
    // ===========================

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
                .flatMap(f -> {
                    switch (f.getFileType()) {
                        case CHAT_PHOTO: {
                            InputPeer peer = f.getPeer().orElseThrow();
                            switch (peer.identifier()) {
                                case InputPeerChannel.ID:
                                case InputPeerChannelFromMessage.ID:
                                    InputChannel channel = TlEntityUtil.toInputChannel(peer);
                                    return serviceHolder.getChatService()
                                            .getMessages(channel, List.of(ImmutableInputMessageID.of(f.getMessageId())))
                                            .ofType(ChannelMessages.class)
                                            .flatMap(b -> findMessageAction(b, f));
                                case InputPeerChat.ID:
                                    return serviceHolder.getChatService()
                                            .getMessages(List.of(ImmutableInputMessageID.of(f.getMessageId())))
                                            .ofType(BaseMessages.class)
                                            .flatMap(b -> findMessageAction(b, f));
                                case InputPeerSelf.ID:
                                case InputPeerUser.ID:
                                case InputPeerUserFromMessage.ID:
                                    return serviceHolder.getUserService()
                                            .getUserPhotos(TlEntityUtil.toInputUser(peer),
                                                    0, -f.getDocumentId(), 1)
                                            .map(p -> p.photos().get(0))
                                            .ofType(BasePhoto.class)
                                            .map(p -> FileReferenceId.ofChatPhoto(p, '\0', -1, peer));
                                default:
                                    return Mono.error(new IllegalArgumentException("Unknown input peer type: " + peer));
                            }
                        }
                        case WEB_DOCUMENT:
                        case DOCUMENT:
                        case PHOTO: // message id must be present
                            InputPeer peer = f.getPeer().orElseThrow();
                            switch (peer.identifier()) {
                                case InputPeerChannel.ID:
                                case InputPeerChannelFromMessage.ID:
                                    InputChannel channel = TlEntityUtil.toInputChannel(peer);
                                    return serviceHolder.getChatService()
                                            .getMessages(channel, List.of(ImmutableInputMessageID.of(f.getMessageId())))
                                            .ofType(ChannelMessages.class)
                                            .flatMap(b -> findMessageMedia(b, f));
                                case InputPeerSelf.ID:
                                case InputPeerUser.ID:
                                case InputPeerUserFromMessage.ID:
                                case InputPeerChat.ID:
                                    return serviceHolder.getChatService()
                                            .getMessages(List.of(ImmutableInputMessageID.of(f.getMessageId())))
                                            .ofType(BaseMessages.class)
                                            .flatMap(b -> findMessageMedia(b, f));
                                default:
                                    return Mono.error(new IllegalArgumentException("Unknown input peer type: " + peer));
                            }
                            // No need refresh
                        case STICKER_SET_THUMB: return Mono.just(f);
                        default: return Mono.error(new IllegalStateException());
                    }
                });
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
                    .map(p -> ImmutableInputChannelFromMessage.of(p, min.getMessageId(), channelId.asLong()));
        }

        if (channelId.getAccessHash().isEmpty()) {
            return mtProtoResources.getStoreLayout().resolveChannel(channelId.asLong());
        }
        return Mono.just(ImmutableBaseInputChannel.of(channelId.asLong(), channelId.getAccessHash().orElseThrow()));
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

        if (userId.equals(getSelfId())) { // Possible optimisation
            return Mono.just(InputUserSelf.instance());
        }

        var min = userId.getMinInformation().orElse(null);
        if (min != null) {
            if (authResources.isBot()) {
                return Mono.error(new IllegalArgumentException("Min ids can not be used for bots"));
            }

            return asInputPeer(min.getPeerId())
                    .map(p -> ImmutableInputUserFromMessage.of(p, min.getMessageId(), userId.asLong()));
        }

        if (userId.getAccessHash().isEmpty()) {
            return mtProtoResources.getStoreLayout().resolveUser(userId.asLong());
        }
        return Mono.just(ImmutableBaseInputUser.of(userId.asLong(), userId.getAccessHash().orElseThrow()));
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
            case USER: {
                if (getSelfId().equals(peerId)) {
                    return InputPeerSelf.instance();
                }

                var min = peerId.getMinInformation().orElse(null);
                if (min != null) {
                    if (authResources.isBot()) {
                        throw new IllegalArgumentException("Min ids can not be used for bots");
                    }

                    InputPeer p = asResolvedInputPeer(min.getPeerId());
                    return ImmutableInputPeerUserFromMessage.of(p, min.getMessageId(), peerId.asLong());
                }

                return ImmutableInputPeerUser.of(peerId.asLong(), peerId.getAccessHash()
                        .orElseThrow(() -> new IllegalArgumentException("No access hash present")));
            }
            case CHAT: return ImmutableInputPeerChat.of(peerId.asLong());
            case CHANNEL: {

                var min = peerId.getMinInformation().orElse(null);
                if (min != null) {
                    if (authResources.isBot()) {
                        throw new IllegalArgumentException("Min ids can not be used for bots");
                    }

                    InputPeer p = asResolvedInputPeer(min.getPeerId());
                    return ImmutableInputPeerChannelFromMessage.of(p, min.getMessageId(), peerId.asLong());
                }

                return ImmutableInputPeerChannel.of(peerId.asLong(), peerId.getAccessHash()
                        .orElseThrow(() -> new IllegalArgumentException("No access hash present")));
            }
            default: throw new IllegalStateException();
        }
    }

    // EntityRetriever methods
    // ===========================

    @Override
    public Mono<PeerEntity> resolvePeer(PeerId peerId) {
        return entityRetriever.resolvePeer(peerId);
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
    public Mono<Chat> getChatMinById(Id chatId) {
        return entityRetriever.getChatMinById(chatId);
    }

    @Override
    public Mono<Chat> getChatFullById(Id chatId) {
        return entityRetriever.getChatFullById(chatId);
    }

    @Override
    public Mono<AuxiliaryMessages> getMessagesById(Iterable<? extends InputMessage> messageIds) {
        return entityRetriever.getMessagesById(messageIds);
    }

    @Override
    public Mono<AuxiliaryMessages> getMessagesById(Id channelId, Iterable<? extends InputMessage> messageIds) {
        return entityRetriever.getMessagesById(channelId, messageIds);
    }

    // Internal methods
    // ===========================

    private Mono<FileReferenceId> findMessageAction(Messages messages, FileReferenceId orig) {
        var list = messages.identifier() == BaseMessages.ID
                ? ((BaseMessages) messages).messages()
                : ((ChannelMessages) messages).messages();

        var service = list.stream()
                .filter(m -> m.id() == orig.getMessageId() &&
                        m.identifier() == MessageService.ID)
                .map(m -> (MessageService) m)
                .findFirst()
                .orElseThrow();

        if (service.action().identifier() == MessageActionChatEditPhoto.ID) {
            MessageActionChatEditPhoto a = (MessageActionChatEditPhoto) service.action();
            return Mono.justOrEmpty(TlEntityUtil.unmapEmpty(a.photo(), BasePhoto.class))
                    .map(p -> FileReferenceId.ofChatPhoto(p, '\0', orig.getMessageId(), orig.getPeer().orElseThrow()));
        }
        return Mono.error(new IllegalStateException("Unexpected MessageAction type: " + service.action()));
    }

    private Mono<FileReferenceId> findMessageMedia(Messages messages, FileReferenceId orig) {
        var list = messages.identifier() == BaseMessages.ID
                ? ((BaseMessages) messages).messages()
                : ((ChannelMessages) messages).messages();
        var message = list.stream()
                .filter(m -> m.id() == orig.getMessageId() &&
                        m.identifier() != MessageEmpty.ID)
                .map(m -> (BaseMessage) m)
                .findFirst()
                .orElseThrow();

        var media = message.media();
        if (media == null) {
            return Mono.empty();
        }

        switch (media.identifier()) {
            case MessageMediaDocument.ID:
                return Mono.justOrEmpty(((MessageMediaDocument) media).document())
                        .ofType(BaseDocument.class)
                        .map(d -> FileReferenceId.ofDocument(d, '\0',
                                orig.getMessageId(), orig.getPeer().orElseThrow()));
            case MessageMediaPhoto.ID:
                return Mono.justOrEmpty(((MessageMediaPhoto) media).photo())
                        .ofType(BasePhoto.class)
                        .map(d -> FileReferenceId.ofPhoto(d, '\0',
                                orig.getMessageId(), orig.getPeer().orElseThrow()));
            case MessageMediaInvoice.ID:
                return Mono.justOrEmpty(((MessageMediaInvoice) media).photo())
                        .map(d -> FileReferenceId.ofDocument(d, orig.getMessageId(), orig.getPeer().orElseThrow()));
            default:
                return Mono.error(new IllegalStateException("Unexpected MessageMedia type: " + media));
        }
    }
}
