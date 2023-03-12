package telegram4j.mtproto.service;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.DcId;
import telegram4j.mtproto.client.MTProtoClientGroup;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.*;
import telegram4j.tl.api.TlMethod;

import java.util.Objects;

public abstract class RpcService {
    protected final MTProtoClientGroup clientGroup;
    protected final StoreLayout storeLayout;

    public RpcService(MTProtoClientGroup clientGroup, StoreLayout storeLayout) {
        this.clientGroup = Objects.requireNonNull(clientGroup);
        this.storeLayout = Objects.requireNonNull(storeLayout);
    }

    public final MTProtoClientGroup getClientGroup() {
        return clientGroup;
    }

    public final StoreLayout getStoreLayout() {
        return storeLayout;
    }

    protected <R, M extends TlMethod<R>> Mono<R> sendMain(M method) {
        return clientGroup.send(DcId.main(), method);
    }

    protected Mono<Peer> toPeer(InputPeer inputPeer) {
        return Mono.defer(() -> switch (inputPeer.identifier()) {
            case InputPeerSelf.ID -> storeLayout.getSelfId()
                    .map(ImmutablePeerUser::of)
                    .switchIfEmpty(Mono.error(() -> new IllegalStateException(
                            "Failed to load self user id from store")));
            case InputPeerChannel.ID -> {
                var inputPeerChannel = (InputPeerChannel) inputPeer;
                yield Mono.just(ImmutablePeerChannel.of(inputPeerChannel.channelId()));
            }
            case InputPeerChannelFromMessage.ID -> {
                var inputPeerChannelFromMessage = (InputPeerChannelFromMessage) inputPeer;
                yield Mono.just(ImmutablePeerChannel.of(inputPeerChannelFromMessage.channelId()));
            }
            case InputPeerChat.ID -> {
                var inputPeerChat = (InputPeerChat) inputPeer;
                yield Mono.just(ImmutablePeerChat.of(inputPeerChat.chatId()));
            }
            case InputPeerUser.ID -> {
                var inputPeerUser = (InputPeerUser) inputPeer;
                yield Mono.just(ImmutablePeerUser.of(inputPeerUser.userId()));
            }
            case InputPeerUserFromMessage.ID -> {
                var inputPeerUserFromMessage = (InputPeerUserFromMessage) inputPeer;
                yield Mono.just(ImmutablePeerUser.of(inputPeerUserFromMessage.userId()));
            }
            default -> Mono.error(new IllegalArgumentException("Unknown input peer type: 0x"
                    + Integer.toHexString(inputPeer.identifier())));
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
