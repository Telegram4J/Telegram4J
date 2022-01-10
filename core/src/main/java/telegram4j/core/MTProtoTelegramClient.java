package telegram4j.core;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.event.dispatcher.UpdatesHandlers;
import telegram4j.core.event.domain.Event;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.MTProtoOptions;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.mtproto.service.ServiceHolder;
import telegram4j.tl.upload.File;

import java.util.Objects;
import java.util.function.Function;

public final class MTProtoTelegramClient {
    /** The supported api scheme version. */
    public static final int LAYER = 137;

    private final AuthorizationResources authorizationResources;
    private final MTProtoClient mtProtoClient;
    private final MTProtoResources mtProtoResources;
    private final UpdatesManager updatesManager;
    private final ServiceHolder serviceHolder;
    private final Mono<Void> onDisconnect;

    MTProtoTelegramClient(AuthorizationResources authorizationResources,
                          MTProtoClient mtProtoClient, MTProtoResources mtProtoResources,
                          UpdatesHandlers updatesHandlers, ServiceHolder serviceHolder,
                          Mono<Void> onDisconnect) {
        this.authorizationResources = authorizationResources;
        this.mtProtoClient = mtProtoClient;
        this.mtProtoResources = mtProtoResources;
        this.serviceHolder = serviceHolder;
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

    public Mono<Void> getFile(String fileReferenceId, Function<File, ? extends Publisher<?>> progressor) {
        return Mono.fromCallable(() -> FileReferenceId.deserialize(fileReferenceId))
                .map(FileReferenceId::asLocation)
                .flatMap(loc -> serviceHolder.getMessageService()
                        .getFile(false, false, loc, progressor));
    }
}
