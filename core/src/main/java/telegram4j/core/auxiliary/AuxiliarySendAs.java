package telegram4j.core.auxiliary;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.util.Id;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class AuxiliarySendAs {
    private final MTProtoTelegramClient client;
    private final Set<Id> peerIds;
    private final List<User> users;
    private final List<Chat> chats;

    public AuxiliarySendAs(MTProtoTelegramClient client, Set<Id> peerIds, List<User> users, List<Chat> chats) {
        this.client = Objects.requireNonNull(client, "client");
        this.peerIds = Objects.requireNonNull(peerIds, "peerIds");
        this.users = Objects.requireNonNull(users, "users");
        this.chats = Objects.requireNonNull(chats, "chats");
    }

    public MTProtoTelegramClient getClient() {
        return client;
    }

    public Set<Id> getPeerIds() {
        return peerIds;
    }

    public List<User> getUsers() {
        return users;
    }

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
