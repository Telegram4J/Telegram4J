package telegram4j.core.retriever;

import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.util.AuxiliaryEntityFactory;
import telegram4j.core.util.EntityFactory;
import telegram4j.core.util.Id;
import telegram4j.core.util.PeerId;
import telegram4j.mtproto.service.ServiceHolder;
import telegram4j.tl.InputMessage;
import telegram4j.tl.PeerChannel;
import telegram4j.tl.PeerUser;
import telegram4j.tl.messages.MessagesNotModified;

import java.util.Objects;

/** Implementation of {@code EntityRetriever} which uses Telegram RPC API. */
public class RpcEntityRetriever implements EntityRetriever {

    private final MTProtoTelegramClient client;
    private final ServiceHolder serviceHolder;

    public RpcEntityRetriever(MTProtoTelegramClient client) {
        this.client = Objects.requireNonNull(client);
        this.serviceHolder = client.getServiceHolder();
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
                .flatMap(username -> serviceHolder.getUserService()
                        .resolveUsername(username))
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
    public Mono<User> getUserMinById(Id userId) {
        if (userId.getType() != Id.Type.USER) {
            return Mono.error(new IllegalArgumentException("Incorrect id type, expected: "
                    + Id.Type.USER + ", but given: " + userId.getType()));
        }

        return client.asInputUser(userId)
                .flatMap(serviceHolder.getUserService()::getUser)
                .mapNotNull(u -> EntityFactory.createUser(client, u));
    }

    @Override
    public Mono<User> getUserFullById(Id userId) {
        if (userId.getType() != Id.Type.USER) {
            return Mono.error(new IllegalArgumentException("Incorrect id type, expected: "
                    + Id.Type.USER + ", but given: " + userId.getType()));
        }

        return client.asInputUser(userId)
                .flatMap(serviceHolder.getUserService()::getFullUser)
                .mapNotNull(u -> EntityFactory.createUser(client, u));
    }

    @Override
    public Mono<Chat> getChatMinById(Id chatId) {
        return Mono.defer(() -> {
            switch (chatId.getType()) {
                case CHAT: return serviceHolder.getChatService().getChat(chatId.asLong())
                        .mapNotNull(c -> EntityFactory.createChat(client, c, null));
                case CHANNEL: return client.asInputChannel(chatId)
                        .flatMap(serviceHolder.getChatService()::getChannel)
                        .mapNotNull(c -> EntityFactory.createChat(client, c, null));
                case USER: return client.asInputUser(chatId)
                        .flatMap(serviceHolder.getUserService()::getUser)
                        .zipWith(client.withRetrievalStrategy(EntityRetrievalStrategy.STORE)
                                .getUserFullById(client.getSelfId())
                                .switchIfEmpty(Mono.error(IllegalStateException::new)))
                        .mapNotNull(TupleUtils.function((c, userFull) -> EntityFactory.createChat(client, c, userFull)));
                default: return Mono.error(new IllegalStateException());
            }
        });
    }

    @Override
    public Mono<Chat> getChatFullById(Id chatId) {
        return Mono.defer(() -> {
            switch (chatId.getType()) {
                case CHAT: return serviceHolder.getChatService().getFullChat(chatId.asLong())
                        .mapNotNull(c -> EntityFactory.createChat(client, c, null));
                case CHANNEL: return client.asInputChannel(chatId)
                        .flatMap(serviceHolder.getChatService()::getFullChannel)
                        .mapNotNull(c -> EntityFactory.createChat(client, c, null));
                case USER: return client.asInputUser(chatId)
                        .flatMap(serviceHolder.getUserService()::getFullUser)
                        .zipWith(client.withRetrievalStrategy(EntityRetrievalStrategy.STORE)
                                .getUserFullById(client.getSelfId())
                                .switchIfEmpty(Mono.error(IllegalStateException::new)))
                        .mapNotNull(TupleUtils.function((c, userFull) -> EntityFactory.createChat(client, c, userFull)));
                default: return Mono.error(new IllegalStateException());
            }
        });
    }

    @Override
    public Mono<AuxiliaryMessages> getMessagesById(Iterable<? extends InputMessage> messageIds) {
        return serviceHolder.getChatService().getMessages(messageIds)
                .filter(m -> m.identifier() != MessagesNotModified.ID) // just ignore
                .map(d -> AuxiliaryEntityFactory.createMessages(client, d));
    }

    @Override
    public Mono<AuxiliaryMessages> getMessagesById(Id channelId, Iterable<? extends InputMessage> messageIds) {
        if (channelId.getType() != Id.Type.CHANNEL) {
            return Mono.error(new IllegalArgumentException("Incorrect id type, expected: "
                    + Id.Type.CHANNEL + ", but given: " + channelId.getType()));
        }

        return client.asInputChannel(channelId)
                .flatMap(c -> serviceHolder.getChatService().getMessages(c, messageIds))
                .filter(m -> m.identifier() != MessagesNotModified.ID) // just ignore
                .map(d -> AuxiliaryEntityFactory.createMessages(client, d));
    }
}
