package telegram4j.core;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.Event;
import telegram4j.core.event.EventDispatcher;
import telegram4j.mtproto.MTProtoOptions;
import telegram4j.mtproto.MTProtoResources;
import telegram4j.mtproto.MTProtoSession;

import java.util.Objects;
import java.util.function.Function;

public final class MTProtoTelegramClient {
    public static final int LAYER = 133;

    private final AuthorizationResources authorizationResources;
    private final MTProtoSession session;
    private final EventDispatcher eventDispatcher;

    MTProtoTelegramClient(AuthorizationResources authorizationResources,
                          MTProtoSession session, EventDispatcher eventDispatcher) {
        this.authorizationResources = authorizationResources;
        this.session = session;
        this.eventDispatcher = eventDispatcher;
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
