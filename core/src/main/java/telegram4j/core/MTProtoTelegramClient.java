package telegram4j.core;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.event.dispatcher.UpdatesHandlers;
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
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.mtproto.service.ServiceHolder;
import telegram4j.tl.ImmutableBaseInputChannel;
import telegram4j.tl.messages.AffectedMessages;
import telegram4j.tl.upload.BaseFile;

import java.util.function.Function;

public final class MTProtoTelegramClient implements EntityRetriever {
    /** The supported api scheme version. */
    public static final int LAYER = 137;

    private final AuthorizationResources authResources;
    private final MTProtoClient mtProtoClient;
    private final MTProtoResources mtProtoResources;
    private final UpdatesManager updatesManager;
    private final ServiceHolder serviceHolder;
    private final EntityRetriever entityRetriever;
    private final Mono<Void> onDisconnect;

    MTProtoTelegramClient(AuthorizationResources authResources,
                          MTProtoClient mtProtoClient, MTProtoResources mtProtoResources,
                          UpdatesHandlers updatesHandlers, ServiceHolder serviceHolder,
                          Function<MTProtoTelegramClient, EntityRetriever> entityRetriever,
                          Mono<Void> onDisconnect) {
        this.authResources = authResources;
        this.mtProtoClient = mtProtoClient;
        this.mtProtoResources = mtProtoResources;
        this.serviceHolder = serviceHolder;
        this.entityRetriever = entityRetriever.apply(this);
        this.onDisconnect = onDisconnect;

        this.updatesManager = new UpdatesManager(this, updatesHandlers);
    }

    public static MTProtoBootstrap<MTProtoOptions> create(int appId, String appHash, String botAuthToken) {
        return new MTProtoBootstrap<>(Function.identity(), new AuthorizationResources(appId, appHash, botAuthToken));
    }

    public static MTProtoBootstrap<MTProtoOptions> create(int appId, String appHash,
                                                          Function<MTProtoTelegramClient, Publisher<?>> authHandler) {
        return new MTProtoBootstrap<>(Function.identity(), new AuthorizationResources(appId, appHash, authHandler));
    }

    public UpdatesManager getUpdatesManager() {
        return updatesManager;
    }

    public boolean isBot() {
        return authResources.getType() == AuthorizationResources.Type.BOT;
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

    public Mono<Void> getFile(String fileReferenceId, Function<BaseFile, ? extends Publisher<?>> progressor) {
        return Mono.fromCallable(() -> FileReferenceId.deserialize(fileReferenceId))
                .flatMap(loc -> serviceHolder.getMessageService()
                        .getFile(loc, progressor));
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
    public Mono<AuxiliaryMessages> getMessageById(Id chatId, IdFields.MessageId messageId) {
        return entityRetriever.getMessageById(chatId, messageId);
    }
}
