package telegram4j.core;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.event.Event;
import telegram4j.core.event.dispatcher.EventDispatcher;
import telegram4j.mtproto.MTProtoOptions;
import telegram4j.mtproto.MTProtoSession;
import telegram4j.mtproto.util.CryptoUtil;
import telegram4j.tl.BaseUser;
import telegram4j.tl.InputPeerUser;
import telegram4j.tl.Message;
import telegram4j.tl.request.contacts.ResolveUsername;
import telegram4j.tl.request.messages.SendMessage;

import java.util.Objects;
import java.util.function.Function;

public final class MTProtoTelegramClient {
    public static final int LAYER = 133;

    private final AuthorizationResources authorizationResources;
    private final EventDispatcher eventDispatcher;
    private final Mono<Void> onDisconnect;
    private final MTProtoSession session;

    MTProtoTelegramClient(AuthorizationResources authorizationResources, EventDispatcher eventDispatcher,
                          Mono<Void> onDisconnect, MTProtoSession session) {
        this.authorizationResources = authorizationResources;
        this.eventDispatcher = eventDispatcher;
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

    public AuthorizationResources getAuthorizationResources() {
        return authorizationResources;
    }

    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    public MTProtoSession getSession() {
        return session;
    }

    public Mono<Void> onDisconnect() {
        return onDisconnect;
    }

    public <E extends Event> Flux<E> on(Class<E> type) {
        return eventDispatcher.on(type);
    }

    public Mono<Message> sendMessage() {
//        return session.sendEncrypted(ResolveUsername.builder().username("skat_ina").build())
//                .map(peer -> peer.users().get(0))
//                .ofType(BaseUser.class)
//                .flatMap(user -> client.getSession()
//                        .withPayloadMapper(PayloadMapperStrategy.ENCRYPTED)
//                        .send(SendMessage.builder()
//                                .randomId(CryptoUtil.random.nextLong())
//                                .message("sus")
//                                .peer(InputPeerUser.builder()
//                                        .userId(user.id())
//                                        .accessHash(Objects.requireNonNull(user.accessHash()))
//                                        .build())
//                                .build()))
//                .log()
//                .block();
        return Mono.empty();
    }
}
