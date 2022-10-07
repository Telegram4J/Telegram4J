package telegram4j.core.retriever;

import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.util.AuxiliaryEntityFactory;
import telegram4j.core.util.EntityFactory;
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
                .mapNotNull(p -> {
                    switch (p.peer().identifier()) {
                        case PeerChannel.ID: return EntityFactory.createChat(client, p.chats().get(0), null);
                        case PeerUser.ID: return EntityFactory.createUser(client, p.users().get(0));
                        default: throw new IllegalArgumentException("Unknown Peer type: " + p.peer());
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
                        case CHAT: return storeLayout.getChatMinById(chatId.asLong());
                        case CHANNEL: return storeLayout.getChannelMinById(chatId.asLong());
                        case USER: return storeLayout.getUserMinById(chatId.asLong());
                        default: return Mono.error(new IllegalStateException());
                    }
                })
                .mapNotNull(c -> EntityFactory.createChat(client, c, null));
    }

    @Override
    public Mono<Chat> getChatFullById(Id chatId) {
        return Mono.defer(() -> {
                    switch (chatId.getType()) {
                        case CHAT: return storeLayout.getChatFullById(chatId.asLong());
                        case CHANNEL: return storeLayout.getChannelFullById(chatId.asLong());
                        case USER: return storeLayout.getUserFullById(chatId.asLong());
                        default: return Mono.error(new IllegalStateException());
                    }
                })
                .mapNotNull(c -> EntityFactory.createChat(client, c, null));
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
