package telegram4j.core;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.event.dispatcher.UpdatesHandlers;
import telegram4j.core.event.domain.Event;
import telegram4j.core.object.Id;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.retriever.EntityRetriever;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.MTProtoOptions;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.mtproto.service.ServiceHolder;
import telegram4j.tl.upload.BaseFile;

import java.util.Objects;
import java.util.function.Function;

public final class MTProtoTelegramClient implements EntityRetriever {
    /** The supported api scheme version. */
    public static final int LAYER = 137;

    private final AuthorizationResources authorizationResources;
    private final MTProtoClient mtProtoClient;
    private final MTProtoResources mtProtoResources;
    private final UpdatesManager updatesManager;
    private final ServiceHolder serviceHolder;
    private final EntityRetriever entityRetriever;
    private final Mono<Void> onDisconnect;

    MTProtoTelegramClient(AuthorizationResources authorizationResources,
                          MTProtoClient mtProtoClient, MTProtoResources mtProtoResources,
                          UpdatesHandlers updatesHandlers, ServiceHolder serviceHolder,
                          Function<MTProtoTelegramClient, EntityRetriever> entityRetriever,
                          Mono<Void> onDisconnect) {
        this.authorizationResources = authorizationResources;
        this.mtProtoClient = mtProtoClient;
        this.mtProtoResources = mtProtoResources;
        this.serviceHolder = serviceHolder;
        this.entityRetriever = entityRetriever.apply(this);
        this.onDisconnect = onDisconnect;

        this.updatesManager = new UpdatesManager(this, updatesHandlers);
    }

    public static MTProtoBootstrap<MTProtoOptions> create(int appId, String appHash, String botAuthToken) {
        Objects.requireNonNull(botAuthToken, "botAuthToken");
        return new MTProtoBootstrap<>(Function.identity(),
                new AuthorizationResources(appId, appHash, botAuthToken, AuthorizationResources.Type.BOT));
    }

    public static MTProtoBootstrap<MTProtoOptions> create(int appId, String appHash) {
        return new MTProtoBootstrap<>(Function.identity(),
                new AuthorizationResources(appId, appHash, null, AuthorizationResources.Type.USER));
    }

    public UpdatesManager getUpdatesManager() {
        return updatesManager;
    }

    public AuthorizationResources getAuthorizationResources() {
        return authorizationResources;
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
        return mtProtoResources.getClientManager().close();
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

    // EntityRetriever methods
    // ==========================


    @Override
    public Mono<PeerEntity> resolveUsername(String username) {
        return entityRetriever.resolveUsername(username);
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
}
