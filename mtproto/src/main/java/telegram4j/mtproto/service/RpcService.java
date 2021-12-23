package telegram4j.mtproto.service;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;

import java.util.Objects;

public abstract class RpcService {
    protected final MTProtoClient client;
    protected final StoreLayout storeLayout;

    public RpcService(MTProtoClient client, StoreLayout storeLayout) {
        this.client = Objects.requireNonNull(client, "client");
        this.storeLayout = Objects.requireNonNull(storeLayout, "storeLayout");
    }

    public final MTProtoClient getClient() {
        return client;
    }

    public final StoreLayout getStoreLayout() {
        return storeLayout;
    }

    public Mono<InputPeer> getInputPeer(Peer peer) {
        return Mono.defer(() -> {
            long id = TlEntityUtil.getPeerId(peer);
            switch (peer.identifier()) {
                case PeerChannel.ID: return getInputPeerChannel(id);
                case PeerChat.ID: return getInputPeerChat(id);
                case PeerUser.ID: return getInputPeerUser(id);
                default: return Mono.error(new IllegalArgumentException("Unknown peer type: " + peer));
            }
        });
    }

    protected Mono<Peer> toPeer(InputPeer inputPeer) {
        return Mono.defer(() -> {
            switch (inputPeer.identifier()) {
                case InputPeerEmpty.ID:
                    return Mono.error(new IllegalArgumentException("Empty input peer: " + inputPeer));
                case InputPeerSelf.ID:
                    return storeLayout.getSelfId()
                            .map(ImmutablePeerUser::of)
                            .switchIfEmpty(Mono.error(new IllegalStateException(
                                    "Failed to load self user id from store.")));
                case InputPeerChannel.ID:
                    InputPeerChannel inputPeerChannel = (InputPeerChannel) inputPeer;
                    return Mono.just(ImmutablePeerChannel.of(inputPeerChannel.channelId()));
                // TODO: test this
                case InputPeerChannelFromMessage.ID:
                    InputPeerChannelFromMessage inputPeerChannelFromMessage = (InputPeerChannelFromMessage) inputPeer;
                    return Mono.just(ImmutablePeerChannel.of(inputPeerChannelFromMessage.channelId()));
                case InputPeerChat.ID:
                    InputPeerChat inputPeerChat = (InputPeerChat) inputPeer;
                    return Mono.just(ImmutablePeerChat.of(inputPeerChat.chatId()));
                case InputPeerUser.ID:
                    InputPeerUser inputPeerUser = (InputPeerUser) inputPeer;
                    return Mono.just(ImmutablePeerUser.of(inputPeerUser.userId()));
                // TODO: test this
                case InputPeerUserFromMessage.ID:
                    InputPeerUserFromMessage inputPeerUserFromMessage = (InputPeerUserFromMessage) inputPeer;
                    return Mono.just(ImmutablePeerUser.of(inputPeerUserFromMessage.userId()));
                default:
                    return Mono.error(new IllegalArgumentException("Unknown input peer type: 0x"
                            + Integer.toHexString(inputPeer.identifier())));
            }
        });
    }

    protected Mono<InputPeer> getInputPeerUser(long id) {
        return storeLayout.getSelfId()
                .filter(selfId -> selfId == id)
                .<InputPeer>map(selfId -> InputPeerSelf.instance())
                .switchIfEmpty(storeLayout.getUserMinById(id)
                        .ofType(BaseUser.class)
                        .flatMap(user -> Mono.justOrEmpty(user.accessHash())
                                .map(accessHash -> ImmutableInputPeerUser.of(user.id(), accessHash))));
    }

    protected Mono<InputPeerChat> getInputPeerChat(long id) {
        return storeLayout.getChatMinById(id)
                .ofType(BaseChat.class)
                .map(chat -> ImmutableInputPeerChat.of(chat.id()));
    }

    protected Mono<InputPeerChannel> getInputPeerChannel(long id) {
        return storeLayout.getChatMinById(id)
                .ofType(Channel.class)
                .flatMap(user -> Mono.justOrEmpty(user.accessHash())
                        .map(accessHash -> ImmutableInputPeerChannel.of(user.id(), accessHash)));
    }
}
