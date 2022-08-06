package telegram4j.core;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;
import telegram4j.tl.api.TlEncodingUtil;
import telegram4j.tl.messages.AffectedMessages;
import telegram4j.tl.storage.FileType;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;

public final class MTProtoTelegramClient implements EntityRetriever {
    /** The supported api scheme version. */
    public static final int LAYER = TlInfo.LAYER;

    private final AuthorizationResources authResources;
    private final MTProtoClient mtProtoClient;
    private final MTProtoResources mtProtoResources;
    private final UpdatesManager updatesManager;
    private final FileReferenceManager fileReferenceManager;
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
        this.fileReferenceManager = new FileReferenceManager(this);
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
     * Gets {@link FileReferenceManager} resource to handle and refresh {@link FileReferenceId} for documents and photos.
     *
     * @return The {@link FileReferenceManager} resource to handle and refresh {@link FileReferenceId} for documents and photos.
     */
    public FileReferenceManager getFileReferenceManager() {
        return fileReferenceManager;
    }

    /**
     * Gets whether current client is bot.
     *
     * @return {@code true} if current client is bot.
     */
    public boolean isBot() {
        return authResources.getType() == AuthorizationResources.Type.BOT;
    }

    /**
     * Gets id of <i>current</i> user.
     *
     * @return The id of <i>current</i> user.
     */
    public Id getSelfId() {
        return Objects.requireNonNull(selfIdHolder[0], "Self id has not yet resolved.");
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
    // ==========================

    /**
     * Request to upload file to Telegram Media DC.
     * Method doesn't release incoming {@link ByteBuf}.
     *
     * @param data The {@link ByteBuf} with file data.
     * @param filename The name for file.
     * @return A {@link Mono} emitting on successful completion {@link InputFile} with file id and CRC32 if applicable.
     */
    public Mono<InputFile> uploadFile(ByteBuf data, String filename) {
        return serviceHolder.getUploadService().saveFile(data, filename);
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
     *
     * @param loc The location of file.
     * @return A {@link Flux} emitting full or parts of downloading file.
     */
    public Flux<FilePart> downloadFile(FileReferenceId loc) {
        return Flux.defer(() -> {
            if (loc.getFileType() == FileReferenceId.Type.WEB_DOCUMENT) {
                if (loc.getAccessHash() == -1) { // Non-proxied file, just download via netty's HttpClient
                    return getFile0(loc);
                }

                return serviceHolder.getUploadService().getWebFile(loc)
                        .map(FilePart::ofWebFile);
            }

            return serviceHolder.getUploadService().getFile(loc)
                    .map(FilePart::ofFile);
        });
    }

    private Flux<FilePart> getFile0(FileReferenceId loc) {
        return mtProtoResources.getHttpClient()
                .get().uri(loc.getUrl())
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

    /**
     * Request to delete messages in DM or group chats.
     *
     * @param revoke Whether to delete messages for all participants of the chat.
     * @param ids An {@link Iterable} of message ids.
     * @return A {@link Mono} emitting on successful completion {@link AffectedMessages} with range of affected <b>common</b> events.
     */
    public Mono<AffectedMessages> deleteMessages(boolean revoke, Iterable<Integer> ids) {
        return serviceHolder.getMessageService().deleteMessages(revoke, ids);
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
                .flatMap(p -> serviceHolder.getMessageService()
                        .deleteMessages(p, ids));
    }

    // Utility methods
    // ==========================

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
            if (isBot()) {
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
            if (isBot()) {
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
                    if (isBot()) {
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
                    if (isBot()) {
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
    // ==========================

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
}
