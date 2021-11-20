package telegram4j.mtproto;

import reactor.core.publisher.Mono;

public final class MTProtoTelegramClient {
    private final Mono<Void> onDisconnect;
    private final MTProtoSession session;

    public MTProtoTelegramClient(Mono<Void> onDisconnect, MTProtoSession session) {
        this.onDisconnect = onDisconnect;
        this.session = session;
    }

    public MTProtoSession getSession() {
        return session;
    }

    public Mono<Void> onDisconnect() {
        return onDisconnect;
    }
}
