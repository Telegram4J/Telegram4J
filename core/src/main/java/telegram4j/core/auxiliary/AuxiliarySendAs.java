package telegram4j.core.auxiliary;

import reactor.util.function.Tuple2;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.chat.PrivateChat;
import telegram4j.core.util.Id;

import java.util.List;
import java.util.Objects;

public class AuxiliarySendAs {
    private final MTProtoTelegramClient client;
    private final List<Tuple2<Boolean, Id>> peerIds;
    private final List<User> users;
    private final List<Chat> chats;

    public AuxiliarySendAs(MTProtoTelegramClient client, List<Tuple2<Boolean, Id>> peerIds, List<User> users, List<Chat> chats) {
        this.client = Objects.requireNonNull(client);
        this.peerIds = Objects.requireNonNull(peerIds);
        this.users = Objects.requireNonNull(users);
        this.chats = Objects.requireNonNull(chats);
    }

    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets immutable list of {@link Tuple2} containing {@code boolean} which
     * indicates whether premium is required to use peer and {@link Id} of peer.
     *
     * @return The immutable list of {@link Tuple2} with the availability flag and id.
     */
    // TODO: type most likely will changed in future
    public List<Tuple2<Boolean, Id>> getPeerIds() {
        return peerIds;
    }

    /**
     * Gets immutable list of users whose ids contain in {@link #getPeerIds()}.
     *
     * @return The immutable list of users.
     */
    public List<User> getUsers() {
        return users;
    }

    /**
     * Gets immutable list of chats whose ids contain in {@link #getPeerIds()}.
     * This list doesn't contain {@link PrivateChat} objects.
     *
     * @return The immutable list of chats.
     */
    public List<Chat> getChats() {
        return chats;
    }

    @Override
    public String toString() {
        return "AuxiliarySendAs{" +
                "peerIds=" + peerIds +
                ", users=" + users +
                ", chats=" + chats +
                '}';
    }
}
