package telegram4j.core;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.MTProtoOptions;
import telegram4j.mtproto.MTProtoSession;

import java.util.function.Function;

public final class MTProtoTelegramClient {
    private final TelegramResources telegramResources;
    private final Mono<Void> onDisconnect;
    private final MTProtoSession session;

    MTProtoTelegramClient(TelegramResources telegramResources, Mono<Void> onDisconnect, MTProtoSession session) {
        this.telegramResources = telegramResources;
        this.onDisconnect = onDisconnect;
        this.session = session;
    }

    public static MTProtoBootstrap<MTProtoOptions> create(int appId, String appHash, String botAuthToken) {
        return new MTProtoBootstrap<>(Function.identity(), new TelegramResources(appId, appHash, botAuthToken));
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
