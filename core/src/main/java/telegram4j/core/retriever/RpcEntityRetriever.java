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
import telegram4j.mtproto.service.ServiceHolder;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.InputMessage;
import telegram4j.tl.messages.MessagesNotModified;

import java.util.Objects;

/**
 * Default implementation of {@code EntityRetriever}, which
 * first tries to get entity from {@link StoreLayout} and secondary retrieves from Telegram RPC.
 */
public class RpcEntityRetriever implements EntityRetriever {

    private final MTProtoTelegramClient client;
    private final ServiceHolder serviceHolder;
    private final StoreLayout storeLayout;

    public RpcEntityRetriever(MTProtoTelegramClient client) {
        this.client = Objects.requireNonNull(client);

        this.serviceHolder = client.getServiceHolder();
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
                .map(TlEntityUtil::stripUsername)
                .flatMap(username -> storeLayout.resolvePeer(username)
                        .switchIfEmpty(serviceHolder.getUserService()
                                .resolveUsername(username)))
                .mapNotNull(p -> EntityFactory.createPeerEntity(client, p))
                .switchIfEmpty(resolveById);
    }

    @Override
    public Mono<User> getUserMinById(Id userId) {
        if (userId.getType() != Id.Type.USER) {
            return Mono.error(new IllegalArgumentException("Incorrect id type, expected: "
                    + Id.Type.USER + ", but given: " + userId.getType()));
        }

        return storeLayout.getUserMinById(userId.asLong())
                .map(u -> (telegram4j.tl.User) u)
                .switchIfEmpty(client.asInputUser(userId).flatMap(serviceHolder.getUserService()::getUser))
                .mapNotNull(u -> EntityFactory.createUser(client, u));
    }

    @Override
    public Mono<User> getUserFullById(Id userId) {
        if (userId.getType() != Id.Type.USER) {
            return Mono.error(new IllegalArgumentException("Incorrect id type, expected: "
                    + Id.Type.USER + ", but given: " + userId.getType()));
        }

        return storeLayout.getUserFullById(userId.asLong())
                .switchIfEmpty(client.asInputUser(userId).flatMap(serviceHolder.getUserService()::getFullUser))
                .mapNotNull(u -> EntityFactory.createUser(client, u));
    }

    @Override
    public Mono<Chat> getChatMinById(Id chatId) {
        return Mono.defer(() -> {
            switch (chatId.getType()) {
                case CHAT: return storeLayout.getChatMinById(chatId.asLong())
                        .map(u -> (telegram4j.tl.Chat) u)
                        .switchIfEmpty(client.getServiceHolder().getChatService().getChat(chatId.asLong()));
                case CHANNEL: return storeLayout.getChannelMinById(chatId.asLong())
                        .map(u -> (telegram4j.tl.Chat) u)
                        .switchIfEmpty(client.asInputChannel(chatId).flatMap(serviceHolder.getChatService()::getChannel));
                case USER: return storeLayout.getUserMinById(chatId.asLong())
                        .map(u -> (telegram4j.tl.User) u)
                        .switchIfEmpty(client.asInputUser(chatId).flatMap(serviceHolder.getUserService()::getUser));
                default: return Mono.error(new IllegalStateException());
            }
        })
        .mapNotNull(c -> EntityFactory.createChat(client, c, null));
    }

    @Override
    public Mono<Chat> getChatFullById(Id chatId) {
        return Mono.defer(() -> {
            switch (chatId.getType()) {
                case CHAT: return storeLayout.getChatFullById(chatId.asLong())
                        .switchIfEmpty(client.getServiceHolder().getChatService()
                                .getFullChat(chatId.asLong()));
                case CHANNEL: return storeLayout.getChannelFullById(chatId.asLong())
                        .switchIfEmpty(client.asInputChannel(chatId)
                                .flatMap(serviceHolder.getChatService()::getFullChannel));
                case USER: return storeLayout.getUserFullById(chatId.asLong())
                        .switchIfEmpty(client.asInputUser(chatId)
                                .flatMap(serviceHolder.getUserService()::getFullUser));
                default: return Mono.error(new IllegalStateException());
            }
        })
        .mapNotNull(c -> EntityFactory.createChat(client, c, null));
    }

    @Override
    public Mono<AuxiliaryMessages> getMessagesById(Iterable<? extends InputMessage> messageIds) {
        return storeLayout.getMessages(messageIds)
                .switchIfEmpty(client.getServiceHolder()
                        .getMessageService()
                        .getMessages(messageIds))
                .filter(m -> m.identifier() != MessagesNotModified.ID) // just ignore
                .map(d -> AuxiliaryEntityFactory.createMessages(client, d));
    }

    @Override
    public Mono<AuxiliaryMessages> getMessagesById(Id channelId, Iterable<? extends InputMessage> messageIds) {
        if (channelId.getType() != Id.Type.CHANNEL) {
            return Mono.error(new IllegalArgumentException("Incorrect id type, expected: "
                    + Id.Type.CHANNEL + ", but given: " + channelId.getType()));
        }

        return storeLayout.getMessages(channelId.asLong(), messageIds)
                .switchIfEmpty(client.asInputChannel(channelId)
                        .flatMap(c -> client.getServiceHolder()
                                .getMessageService()
                                .getMessages(c, messageIds)))
                .filter(m -> m.identifier() != MessagesNotModified.ID) // just ignore
                .map(d -> AuxiliaryEntityFactory.createMessages(client, d));
    }
}
