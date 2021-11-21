package telegram4j.core;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.MTProtoOptions;
import telegram4j.mtproto.MTProtoSession;

import java.util.Objects;
import java.util.function.Function;

public final class MTProtoTelegramClient {
    public static final int LAYER = 133;

    private final TelegramResources telegramResources;
    private final Mono<Void> onDisconnect;
    private final MTProtoSession session;

    MTProtoTelegramClient(TelegramResources telegramResources, Mono<Void> onDisconnect, MTProtoSession session) {
        this.telegramResources = telegramResources;
        this.onDisconnect = onDisconnect;
        this.session = session;
    }

    public static MTProtoBootstrap<MTProtoOptions> create(int appId, String appHash, String botAuthToken) {
        Objects.requireNonNull(botAuthToken, "botAuthToken");
        return new MTProtoBootstrap<>(Function.identity(),
                new TelegramResources(appId, appHash, botAuthToken, AuthorizationType.BOT));
    }

    public static MTProtoBootstrap<MTProtoOptions> create(int appId, String appHash) {
        return new MTProtoBootstrap<>(Function.identity(),
                new TelegramResources(appId, appHash, null, AuthorizationType.USER));
    }

    public TelegramResources getTelegramResources() {
        return telegramResources;
    }

    public MTProtoSession getSession() {
        return session;
    }

    public Mono<Void> onDisconnect() {
        return onDisconnect;
    }
}
