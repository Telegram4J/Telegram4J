package telegram4j.core;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import telegram4j.core.event.dispatcher.UpdatesHandlers;
import telegram4j.core.event.domain.Event;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.MTProtoOptions;
import telegram4j.mtproto.service.MessageService;

import java.util.Objects;
import java.util.function.Function;

public final class MTProtoTelegramClient {
    public static final int LAYER = 133;

    private final AuthorizationResources authorizationResources;
    private final MTProtoClient mtProtoClient;
    private final MTProtoResources mtProtoResources;
    private final UpdatesManager updatesManager;
    private final MessageService messageService;
    private final Mono<Void> onDisconnect;

    MTProtoTelegramClient(AuthorizationResources authorizationResources,
                          MTProtoClient mtProtoClient, MTProtoResources mtProtoResources,
                          UpdatesHandlers updatesHandlers, Mono<Void> onDisconnect) {
        this.authorizationResources = authorizationResources;
        this.mtProtoClient = mtProtoClient;
        this.mtProtoResources = mtProtoResources;
        this.onDisconnect = onDisconnect;

        this.updatesManager = new UpdatesManager(this, updatesHandlers);
        this.messageService = new MessageService(mtProtoClient, mtProtoResources.getStoreLayout());
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

    public MessageService getMessageService() {
        return messageService;
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
}
