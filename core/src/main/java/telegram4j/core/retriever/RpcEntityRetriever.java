package telegram4j.core.retriever;

import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Id;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.util.EntityFactory;
import telegram4j.mtproto.service.ServiceHolder;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.*;

public class RpcEntityRetriever implements EntityRetriever {

    private final MTProtoTelegramClient client;
    private final ServiceHolder serviceHolder;
    private final StoreLayout storeLayout;

    public RpcEntityRetriever(MTProtoTelegramClient client) {
        this.client = client;
        this.serviceHolder = client.getServiceHolder();
        this.storeLayout = client.getMtProtoResources().getStoreLayout();
    }

    @Override
    public Mono<User> getUserMinById(Id userId) {
        if (userId.getType() != Id.Type.USER) {
            return Mono.error(new IllegalArgumentException("Incorrect id type, expected: "
                    + Id.Type.USER + ", but given: " + userId.getType()));
        }

        return storeLayout.getUserMinById(userId.asLong())
                .switchIfEmpty(asInputUser(userId).flatMap(serviceHolder.getUserService()::getUser))
                .map(u -> new User(client, (BaseUser) u));
    }

    @Override
    public Mono<User> getUserFullById(Id userId) {
        if (userId.getType() != Id.Type.USER) {
            return Mono.error(new IllegalArgumentException("Incorrect id type, expected: "
                    + Id.Type.USER + ", but given: " + userId.getType()));
        }

        return storeLayout.getUserFullById(userId.asLong())
                .switchIfEmpty(asInputUser(userId).flatMap(serviceHolder.getUserService()::getFullUser))
                .map(u -> EntityFactory.createUser(client, u));
    }

    @Override
    public Mono<Chat> getChatMinById(Id chatId) {
        if (chatId.getType() == Id.Type.USER) {
            return Mono.error(new IllegalArgumentException("Incorrect id type, expected: [CHANNEL, CHAT], but given: USER"));
        }

        return storeLayout.getChatMinById(chatId.asLong())
                .switchIfEmpty(Mono.defer(() -> {
                    if (chatId.getType() == Id.Type.CHAT) {
                        return serviceHolder.getChatService().getChat(chatId.asLong());
                    }
                    return asInputChannel(chatId).flatMap(serviceHolder.getChatService()::getChannel);
                }))
                .map(c -> EntityFactory.createChat(client, c));
    }

    @Override
    public Mono<Chat> getChatFullById(Id chatId) {
        if (chatId.getType() == Id.Type.USER) {
            return Mono.error(new IllegalArgumentException("Incorrect id type, expected: [CHANNEL, CHAT], but given: USER"));
        }

        return storeLayout.getChatFullById(chatId.asLong())
                .switchIfEmpty(Mono.defer(() -> {
                    if (chatId.getType() == Id.Type.CHAT) {
                        return serviceHolder.getChatService().getFullChat(chatId.asLong());
                    }
                    return asInputChannel(chatId).flatMap(serviceHolder.getChatService()::getFullChannel);
                }))
                .map(c -> EntityFactory.createChat(client, c));
    }

    private Mono<InputChannel> asInputChannel(Id channelId) {
        if (channelId.getAccessHash().isEmpty()) {
            // it may be unnecessary, because there is no channel in the storage
            return storeLayout.resolveChannel(channelId.asLong());
        }
        return Mono.just(ImmutableBaseInputChannel.of(channelId.asLong(), channelId.getAccessHash().orElseThrow()));
    }


    private Mono<InputUser> asInputUser(Id userId) {
        if (userId.getAccessHash().isEmpty()) {
            return storeLayout.resolveUser(userId.asLong());
        }
        return Mono.just(ImmutableBaseInputUser.of(userId.asLong(), userId.getAccessHash().orElseThrow()));
    }
}
