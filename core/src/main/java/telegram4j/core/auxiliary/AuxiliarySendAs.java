package telegram4j.core.auxiliary;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Id;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;

import java.util.List;
import java.util.Objects;

public class AuxiliarySendAs {
    private final MTProtoTelegramClient client;
    private final List<Id> peerIds;
    private final List<User> users;
    private final List<Chat> chats;

    public AuxiliarySendAs(MTProtoTelegramClient client, List<Id> peerIds, List<User> users, List<Chat> chats) {
        this.client = Objects.requireNonNull(client, "client");
        this.peerIds = Objects.requireNonNull(peerIds, "peerIds");
        this.users = Objects.requireNonNull(users, "users");
        this.chats = Objects.requireNonNull(chats, "chats");
    }

    public MTProtoTelegramClient getClient() {
        return client;
    }

    public List<Id> getPeerIds() {
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
