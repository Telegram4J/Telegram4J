package telegram4j.core.retriever;

import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.internal.AuxiliaryEntityFactory;
import telegram4j.core.internal.EntityFactory;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.util.Id;
import telegram4j.core.util.PeerId;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.InputMessage;
import telegram4j.tl.PeerChannel;
import telegram4j.tl.PeerUser;

import java.util.Objects;

/** Implementation of {@code EntityRetriever} which uses {@link StoreLayout storage}. */
public class StoreEntityRetriever implements EntityRetriever {

    private final MTProtoTelegramClient client;
    private final StoreLayout storeLayout;

    public StoreEntityRetriever(MTProtoTelegramClient client) {
        this.client = Objects.requireNonNull(client);
        this.storeLayout = client.getMtProtoResources().getStoreLayout();
    }

    @Override
    public Mono<PeerEntity> resolvePeer(PeerId peerId) {
        Mono<PeerEntity> resolveById = Mono.justOrEmpty(peerId.asId())
                .flatMap(id -> {
                    switch (id.getType()) {
                        case CHAT:
                        case CHANNEL: return getChatMinById(id);
                        case USER: return getUserMinById(id);
                        default: return Mono.error(new IllegalStateException());
                    }
                });

        return Mono.justOrEmpty(peerId.asUsername())
                .flatMap(storeLayout::resolvePeer)
                .flatMap(p -> {
                    switch (p.peer().identifier()) {
                        case PeerChannel.ID: return getUserFullById(client.getSelfId())
                                .switchIfEmpty(Mono.error(IllegalStateException::new))
                                .mapNotNull(selfUser -> EntityFactory.createChat(client, p.chats().get(0), selfUser));
                        case PeerUser.ID: return Mono.justOrEmpty(EntityFactory.createUser(client, p.users().get(0)));
                        default: return Mono.error(new IllegalStateException("Unknown Peer type: " + p.peer()));
                    }
                })
                .switchIfEmpty(resolveById);
    }

    @Override
    public Mono<User> getUserById(Id userId) {
        return getUserMinById(userId);
    }

    @Override
    public Mono<User> getUserMinById(Id userId) {
        if (userId.getType() != Id.Type.USER) {
            return Mono.error(new IllegalArgumentException("Incorrect id type, expected: "
                    + Id.Type.USER + ", but given: " + userId.getType()));
        }

        return storeLayout.getUserMinById(userId.asLong())
                .mapNotNull(u -> EntityFactory.createUser(client, u));
    }

    @Override
    public Mono<User> getUserFullById(Id userId) {
        if (userId.getType() != Id.Type.USER) {
            return Mono.error(new IllegalArgumentException("Incorrect id type, expected: "
                    + Id.Type.USER + ", but given: " + userId.getType()));
        }

        return storeLayout.getUserFullById(userId.asLong())
                .mapNotNull(u -> EntityFactory.createUser(client, u));
    }

    @Override
    public Mono<Chat> getChatById(Id chatId) {
        return getChatMinById(chatId);
    }

    @Override
    public Mono<Chat> getChatMinById(Id chatId) {
        return Mono.defer(() -> {
                    switch (chatId.getType()) {
                        case CHAT: return storeLayout.getChatMinById(chatId.asLong())
                                .mapNotNull(c -> EntityFactory.createChat(client, c, null));
                        case CHANNEL: return storeLayout.getChannelMinById(chatId.asLong())
                                .mapNotNull(c -> EntityFactory.createChat(client, c, null));
                        case USER: return storeLayout.getUserMinById(chatId.asLong())
                                .zipWith(getUserFullById(client.getSelfId())
                                        .switchIfEmpty(Mono.error(IllegalStateException::new)))
                                .mapNotNull(TupleUtils.function((c, selfUser) -> EntityFactory.createChat(client, c, selfUser)));
                        default: return Mono.error(new IllegalStateException());
                    }
                });
    }

    @Override
    public Mono<Chat> getChatFullById(Id chatId) {
        return Mono.defer(() -> {
                    switch (chatId.getType()) {
                        case CHAT: return storeLayout.getChatFullById(chatId.asLong())
                                .mapNotNull(c -> EntityFactory.createChat(client, c, null));
                        case CHANNEL: return storeLayout.getChannelFullById(chatId.asLong())
                                .mapNotNull(c -> EntityFactory.createChat(client, c, null));
                        case USER: return storeLayout.getUserFullById(chatId.asLong())
                                .zipWith(getUserFullById(client.getSelfId())
                                        .switchIfEmpty(Mono.error(IllegalStateException::new)))
                                .mapNotNull(TupleUtils.function((c, selfUser) -> EntityFactory.createChat(client, c, selfUser)));
                        default: return Mono.error(new IllegalStateException());
                    }
                });
    }

    @Override
    public Mono<AuxiliaryMessages> getMessagesById(Iterable<? extends InputMessage> messageIds) {
        return storeLayout.getMessages(messageIds)
                .map(d -> AuxiliaryEntityFactory.createMessages(client, d));
    }

    @Override
    public Mono<AuxiliaryMessages> getMessagesById(Id channelId, Iterable<? extends InputMessage> messageIds) {
        if (channelId.getType() != Id.Type.CHANNEL) {
            return Mono.error(new IllegalArgumentException("Incorrect id type, expected: "
                    + Id.Type.CHANNEL + ", but given: " + channelId.getType()));
        }

        return storeLayout.getMessages(channelId.asLong(), messageIds)
                .map(d -> AuxiliaryEntityFactory.createMessages(client, d));
    }
}
