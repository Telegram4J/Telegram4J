package telegram4j.core.event.dispatcher;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.Chat;
import telegram4j.tl.Update;
import telegram4j.tl.User;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class UpdateContext<U extends Update> {
    private final MTProtoTelegramClient client;
    private final List<Chat> chats;
    private final List<User> users;
    private final U update;

    protected UpdateContext(MTProtoTelegramClient client, List<Chat> chats, List<User> users, U update) {
        this.client = Objects.requireNonNull(client, "client");
        this.chats = Objects.requireNonNull(chats, "chats");
        this.users = Objects.requireNonNull(users, "users");
        this.update = Objects.requireNonNull(update, "update");
    }

    public static <U extends Update> UpdateContext<U> create(MTProtoTelegramClient client, U update) {
        return new UpdateContext<>(client, Collections.emptyList(), Collections.emptyList(), update);
    }

    public static <U extends Update> UpdateContext<U> create(MTProtoTelegramClient client, List<Chat> chats,
                                                             List<User> users, U update) {
        return new UpdateContext<>(client, chats, users, update);
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
