package telegram4j.core;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.event.dispatcher.UpdatesHandlers;
import telegram4j.core.event.domain.Event;
import telegram4j.core.event.EventDispatcher;
import telegram4j.mtproto.MTProtoOptions;
import telegram4j.mtproto.MTProtoSession;

import java.util.Objects;
import java.util.function.Function;

public final class MTProtoTelegramClient {
    public static final int LAYER = 133;

    private final AuthorizationResources authorizationResources;
    private final MTProtoSession session;
    private final EventDispatcher eventDispatcher;
    private final UpdatesManager updatesManager;

    MTProtoTelegramClient(AuthorizationResources authorizationResources,
                          MTProtoSession session, EventDispatcher eventDispatcher,
                          UpdatesHandlers updatesHandlers) {
        this.authorizationResources = authorizationResources;
        this.session = session;
        this.eventDispatcher = eventDispatcher;
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

    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    public UpdatesManager getUpdatesManager() {
        return updatesManager;
    }

    public AuthorizationResources getAuthorizationResources() {
        return authorizationResources;
    }

    public MTProtoSession getSession() {
        return session;
    }

    public Mono<Void> disconnect() {
        return Mono.fromRunnable(session.getConnection()::dispose);
    }

    public Mono<Void> onDisconnect() {
        return session.getConnection().onDispose();
    }

    public <E extends Event> Flux<E> on(Class<E> type) {
        return eventDispatcher.on(type);
    }
}
