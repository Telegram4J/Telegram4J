package telegram4j.core;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.MTProtoOptions;
import telegram4j.mtproto.MTProtoSession;

import java.util.Objects;
import java.util.function.Function;

public final class MTProtoTelegramClient {
    public static final int LAYER = 133;

    private final AuthorizationResources telegramResources;
    private final Mono<Void> onDisconnect;
    private final MTProtoSession session;

    MTProtoTelegramClient(AuthorizationResources telegramResources, Mono<Void> onDisconnect, MTProtoSession session) {
        this.telegramResources = telegramResources;
        this.onDisconnect = onDisconnect;
        this.session = session;
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

    public AuthorizationResources getTelegramResources() {
        return telegramResources;
    }

    public MTProtoSession getSession() {
        return session;
    }

    public Mono<Void> onDisconnect() {
        return onDisconnect;
    }
}
