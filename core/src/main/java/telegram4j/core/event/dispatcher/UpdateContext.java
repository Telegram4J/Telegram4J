package telegram4j.core.event.dispatcher;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.chat.PrivateChat;
import telegram4j.tl.*;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static telegram4j.mtproto.util.TlEntityUtil.getRawPeerId;

public class UpdateContext<U extends Update> {
    private final MTProtoTelegramClient client;
    private final Map<Long, Chat> chats;
    private final Map<Long, User> users;
    private final U update;

    protected UpdateContext(MTProtoTelegramClient client, Map<Long, Chat> chats, Map<Long, User> users, U update) {
        this.client = Objects.requireNonNull(client);
        this.chats = Objects.requireNonNull(chats);
        this.users = Objects.requireNonNull(users);
        this.update = Objects.requireNonNull(update);
    }

    public static <U extends Update> UpdateContext<U> create(MTProtoTelegramClient client, U update) {
        return new UpdateContext<>(client, Map.of(), Map.of(), update);
    }

    public static <U extends Update> UpdateContext<U> create(MTProtoTelegramClient client,
                                                             Map<Long, Chat> chatsMap,
                                                             Map<Long, User> usersMap, U update) {
        return new UpdateContext<>(client, chatsMap, usersMap, update);
    }

    public MTProtoTelegramClient getClient() {
        return client;
    }

    public Map<Long, Chat> getChats() {
        return chats;
    }

    public Map<Long, User> getUsers() {
        return users;
    }

    public Optional<Chat> getChatEntity(Peer peer) {
        long rawId = getRawPeerId(peer);
        switch (peer.identifier()) {
            case PeerUser.ID:
                return Optional.ofNullable(users.get(rawId))
                        .map(u -> new PrivateChat(client, u,
                                users.get(client.getSelfId().asLong())));
            case PeerChat.ID:
            case PeerChannel.ID:
                return Optional.ofNullable(chats.get(rawId));
            default:
                throw new IllegalArgumentException("Unknown peer type: " + peer);
        }
    }

    public Optional<PeerEntity> getPeerEntity(Peer peer) {
        long rawId = getRawPeerId(peer);
        switch (peer.identifier()) {
            case PeerUser.ID:
                return Optional.ofNullable(users.get(rawId));
            case PeerChat.ID:
            case PeerChannel.ID:
                return Optional.ofNullable(chats.get(rawId));
            default:
                throw new IllegalArgumentException("Unknown peer type: " + peer);
        }
    }

    public U getUpdate() {
        return update;
    }
}
