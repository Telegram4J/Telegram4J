package telegram4j.core.event.dispatcher;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.Chat;
import telegram4j.tl.Update;
import telegram4j.tl.User;

import java.util.Collections;
import java.util.List;

public final class UpdateContext<U extends Update> {
    private final MTProtoTelegramClient client;
    private final List<Chat> chats;
    private final List<User> users;
    private final U update;

    public UpdateContext(MTProtoTelegramClient client, U update) {
        this.client = client;
        this.chats = Collections.emptyList();
        this.users = Collections.emptyList();
        this.update = update;
    }

    public UpdateContext(MTProtoTelegramClient client, List<Chat> chats, List<User> users, U update) {
        this.client = client;
        this.chats = Collections.unmodifiableList(chats);
        this.users = Collections.unmodifiableList(users);
        this.update = update;
    }

    public MTProtoTelegramClient getClient() {
        return client;
    }

    public List<Chat> getChats() {
        return chats;
    }

    public List<User> getUsers() {
        return users;
    }

    public U getUpdate() {
        return update;
    }
}
