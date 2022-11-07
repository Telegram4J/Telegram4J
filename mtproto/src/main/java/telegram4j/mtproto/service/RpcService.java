package telegram4j.mtproto.service;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.MTProtoClientGroup;
import telegram4j.mtproto.MTProtoClientGroupManager;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.*;
import telegram4j.tl.api.TlMethod;

import java.util.Objects;

public abstract class RpcService {
    protected final MTProtoClientGroupManager groupManager;
    protected final StoreLayout storeLayout;

    public RpcService(MTProtoClientGroupManager groupManager, StoreLayout storeLayout) {
        this.groupManager = Objects.requireNonNull(groupManager);
        this.storeLayout = Objects.requireNonNull(storeLayout);
    }

    public final MTProtoClientGroup getClientGroup() {
        return groupManager;
    }

    public final StoreLayout getStoreLayout() {
        return storeLayout;
    }

    protected <R, M extends TlMethod<R>> Mono<R> sendMain(M method) {
        return groupManager.send(groupManager.mainId(), method);
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

    // TODO: I want to develop the following approach
    // ==============================================

    // protected <R, M extends TlMethod<R>> MTProtoResponseMono<R, M> send(M method) {
    //     return MTProtoMono.create(groupManager, groupManager.mainId(), method);
    // }

    // protected <T, R, M extends TlMethod<R>> MTProtoMono<T, R, M> send(M method, Function<Mono<R>, Mono<T>> mapper) {
    //     return MTProtoMono.create(groupManager, groupManager.mainId(), method, mapper);
    // }

    // public class MTProtoMono<T, R, M extends TlMethod<R>> extends Mono<T> {
    //     protected final MTProtoClientGroup clientGroup;
    //     protected final DcId dcId;
    //     protected final M method;
    //     protected final Mono<T> inner;
    //
    //     protected MTProtoMono(MTProtoClientGroup clientGroup, DcId dcId, M method, Mono<T> inner) {
    //         this.clientGroup = clientGroup;
    //         this.dcId = dcId;
    //         this.method = method;
    //         this.inner = inner;
    //     }
    //
    //     public MTProtoClientGroup clientGroup() {
    //         return clientGroup;
    //     }
    //
    //     public DcId dcId() {
    //         return dcId;
    //     }
    //
    //     public M method() {
    //         return method;
    //     }
    //
    //     public MTProtoMono<T, R, M> withDcId(DcId value) {
    //         Objects.requireNonNull(value);
    //         if (dcId.equals(value)) return this;
    //         return new MTProtoMono<>(clientGroup, value, method, inner);
    //     }
    //
    //     public static <T, R, M extends TlMethod<R>> MTProtoMono<T, R, M> create(MTProtoClientGroup clientGroup, DcId dcId, M method,
    //                                                                             Function<Mono<R>, Mono<T>> mapper) {
    //         Objects.requireNonNull(clientGroup);
    //         Objects.requireNonNull(dcId);
    //         Objects.requireNonNull(method);
    //         return new MTProtoMono<>(clientGroup, dcId, method, clientGroup.send(dcId, method).transform(mapper));
    //     }
    //
    //     public static <R, M extends TlMethod<R>> MTProtoResponseMono<R, M> create(MTProtoClientGroup clientGroup, DcId dcId, M method) {
    //         Objects.requireNonNull(clientGroup);
    //         Objects.requireNonNull(dcId);
    //         Objects.requireNonNull(method);
    //         return new MTProtoResponseMono<>(clientGroup, dcId, method, clientGroup.send(dcId, method));
    //     }
    //
    //     @Override
    //     public void subscribe(CoreSubscriber<? super T> actual) {
    //         inner.subscribe(actual);
    //     }
    // }

    // public class MTProtoResponseMono<R, M extends TlMethod<R>> extends MTProtoMono<R, R, M> {
    //
    //     protected MTProtoResponseMono(MTProtoClientGroup clientGroup, DcId dcId, M method, Mono<R> inner) {
    //         super(clientGroup, dcId, method, inner);
    //     }
    //
    //     @Override
    //     public MTProtoResponseMono<R, M> withDcId(DcId value) {
    //         Objects.requireNonNull(value);
    //         if (dcId.equals(value)) return this;
    //         return new MTProtoResponseMono<>(clientGroup, value, method, inner);
    //     }
    // }
}
