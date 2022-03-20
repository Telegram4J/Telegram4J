package telegram4j.core;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.event.dispatcher.UpdatesMapper;
import telegram4j.core.event.domain.Event;
import telegram4j.core.object.Id;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.PeerId;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.retriever.EntityRetriever;
import telegram4j.core.spec.IdFields;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.MTProtoOptions;
import telegram4j.mtproto.file.FilePart;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.mtproto.service.ServiceHolder;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;
import telegram4j.tl.messages.AffectedMessages;
import telegram4j.tl.storage.FileType;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public final class MTProtoTelegramClient implements EntityRetriever {
    /** The supported api scheme version. */
    public static final int LAYER = 137;

    private final AuthorizationResources authResources;
    private final MTProtoClient mtProtoClient;
    private final MTProtoResources mtProtoResources;
    private final UpdatesManager updatesManager;
    private final FileReferenceManager fileReferenceManager;
    private final AtomicReference<Id> selfId;
    private final ServiceHolder serviceHolder;
    private final EntityRetriever entityRetriever;
    private final Mono<Void> onDisconnect;

    MTProtoTelegramClient(AuthorizationResources authResources,
                          MTProtoClient mtProtoClient, MTProtoResources mtProtoResources,
                          UpdatesMapper updatesMapper, AtomicReference<Id> selfId, ServiceHolder serviceHolder,
                          Function<MTProtoTelegramClient, EntityRetriever> entityRetriever,
                          Mono<Void> onDisconnect) {
        this.authResources = authResources;
        this.mtProtoClient = mtProtoClient;
        this.mtProtoResources = mtProtoResources;
        this.serviceHolder = serviceHolder;
        this.selfId = selfId;
        this.entityRetriever = entityRetriever.apply(this);
        this.onDisconnect = onDisconnect;

        this.updatesManager = new UpdatesManager(this, updatesMapper);
        this.fileReferenceManager = new FileReferenceManager(this);
    }

    public static MTProtoBootstrap<MTProtoOptions> create(int apiId, String apiHash, String botAuthToken) {
        return new MTProtoBootstrap<>(Function.identity(), new AuthorizationResources(apiId, apiHash, botAuthToken));
    }

    public static MTProtoBootstrap<MTProtoOptions> create(int apiId, String apiHash,
                                                          Function<MTProtoTelegramClient, Publisher<?>> authHandler) {
        return new MTProtoBootstrap<>(Function.identity(), new AuthorizationResources(apiId, apiHash, authHandler));
    }

    public UpdatesManager getUpdatesManager() {
        return updatesManager;
    }

    public FileReferenceManager getFileReferenceManager() {
        return fileReferenceManager;
    }

    public boolean isBot() {
        return authResources.getType() == AuthorizationResources.Type.BOT;
    }

    public Id getSelfId() {
        return Objects.requireNonNull(selfId.get(), "Self id has not yet resolved.");
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

    public Flux<FilePart> getFile(String fileReferenceId) {
        return Mono.fromCallable(() -> FileReferenceId.deserialize(fileReferenceId))
                .flatMapMany(loc -> {
                    if (loc.getFileType() == FileReferenceId.Type.WEB_DOCUMENT) {
                        if (loc.getAccessHash() == -1) {
                            return getFile0(loc);
                        }

                        return serviceHolder.getUploadService().getWebFile(loc)
                                .map(FilePart::ofWebFile);
                    }

                    return serviceHolder.getUploadService().getFile(loc)
                            .map(FilePart::ofFile);
                });
    }

    public Flux<FilePart> getFile0(FileReferenceId loc) {
        return mtProtoResources.getHttpClient()
                .get().uri(loc.getUrl())
                .responseSingle((res, buf) -> buf.asByteArray()
                        .map(bytes -> {
                            String mimeType = res.responseHeaders().getAsString(HttpHeaderNames.CONTENT_TYPE);
                            int size = res.responseHeaders().getInt(HttpHeaderNames.CONTENT_LENGTH, -1);
                            FileType type = TlEntityUtil.suggestFileType(mimeType);
                            return new FilePart(type, -1, bytes, size, mimeType);
                        }))
                .flux();
    }

    public Mono<AffectedMessages> deleteMessages(boolean revoke, Iterable<Integer> ids) {
        return serviceHolder.getMessageService().deleteMessages(revoke, ids);
    }

    public Mono<AffectedMessages> deleteChannelMessages(Id channelId, Iterable<Integer> ids) {
        if (channelId.getType() != Id.Type.CHANNEL) {
            return Mono.error(new IllegalArgumentException("Channel id type must be CHANNEL"));
        }

        return Mono.defer(() -> {
            if (channelId.getAccessHash().isEmpty()) {
                return mtProtoResources.getStoreLayout()
                        .resolveChannel(channelId.asLong());
            }
            return Mono.just(ImmutableBaseInputChannel.of(channelId.asLong(),
                    channelId.getAccessHash().orElseThrow()));
        })
        .flatMap(p -> serviceHolder.getMessageService()
                .deleteMessages(p, ids));
    }

    private Mono<InputPeer> asInputPeer(Id chatId) {
        switch (chatId.getType()) {
            case USER: return asInputUser(chatId).map(TlEntityUtil::toInputPeer);
            case CHAT: return Mono.just(ImmutableInputPeerChat.of(chatId.asLong()));
            case CHANNEL: return asInputChannel(chatId).map(TlEntityUtil::toInputPeer);
            default: throw new IllegalStateException();
        }
    }

    private Mono<InputChannel> asInputChannel(Id channelId) {
        if (channelId.getAccessHash().isEmpty()) {
            return mtProtoResources.getStoreLayout().resolveChannel(channelId.asLong());
        }
        return Mono.just(ImmutableBaseInputChannel.of(channelId.asLong(), channelId.getAccessHash().orElseThrow()));
    }

    private Mono<InputUser> asInputUser(Id userId) {
        if (userId.getAccessHash().isEmpty()) {
            return mtProtoResources.getStoreLayout().resolveUser(userId.asLong());
        }
        return Mono.just(ImmutableBaseInputUser.of(userId.asLong(), userId.getAccessHash().orElseThrow()));
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
    public Mono<AuxiliaryMessages> getMessagesById(Iterable<? extends IdFields.MessageId> messageIds) {
        return entityRetriever.getMessagesById(messageIds);
    }

    @Override
    public Mono<AuxiliaryMessages> getMessagesById(Id channelId, Iterable<? extends IdFields.MessageId> messageIds) {
        return entityRetriever.getMessagesById(channelId, messageIds);
    }
}
