package telegram4j.mtproto.service;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.store.StoreLayout;
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

    protected Mono<Peer> toPeer(InputPeer inputPeer) {
        return Mono.defer(() -> {
            switch (inputPeer.identifier()) {
                case InputPeerSelf.ID:
                    return storeLayout.getSelfId()
                            .map(ImmutablePeerUser::of)
                            .switchIfEmpty(Mono.error(new IllegalStateException(
                                    "Failed to load self user id from store.")));
                case InputPeerChannel.ID:
                    InputPeerChannel inputPeerChannel = (InputPeerChannel) inputPeer;
                    return Mono.just(ImmutablePeerChannel.of(inputPeerChannel.channelId()));
                case InputPeerChannelFromMessage.ID:
                    InputPeerChannelFromMessage inputPeerChannelFromMessage = (InputPeerChannelFromMessage) inputPeer;
                    return Mono.just(ImmutablePeerChannel.of(inputPeerChannelFromMessage.channelId()));
                case InputPeerChat.ID:
                    InputPeerChat inputPeerChat = (InputPeerChat) inputPeer;
                    return Mono.just(ImmutablePeerChat.of(inputPeerChat.chatId()));
                case InputPeerUser.ID:
                    InputPeerUser inputPeerUser = (InputPeerUser) inputPeer;
                    return Mono.just(ImmutablePeerUser.of(inputPeerUser.userId()));
                case InputPeerUserFromMessage.ID:
                    InputPeerUserFromMessage inputPeerUserFromMessage = (InputPeerUserFromMessage) inputPeer;
                    return Mono.just(ImmutablePeerUser.of(inputPeerUserFromMessage.userId()));
                default:
                    return Mono.error(new IllegalArgumentException("Unknown input peer type: 0x"
                            + Integer.toHexString(inputPeer.identifier())));
            }
        });
    }

    protected static long calculatePaginationHash(Iterable<Long> ids) {
        long hash = 0;
        for (long id : ids) {
            hash ^= id >> 21;
            hash ^= id << 35;
            hash ^= id >> 4;
            hash += id;
        }
        return hash;
    }
}
