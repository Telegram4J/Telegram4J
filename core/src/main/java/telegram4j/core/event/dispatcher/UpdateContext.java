package telegram4j.core.event.dispatcher;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.Chat;
import telegram4j.tl.Update;
import telegram4j.tl.User;

import java.util.Map;
import java.util.Objects;

public class UpdateContext<U extends Update> {
    private final MTProtoTelegramClient client;
    private final Map<Long, Chat> chats;
    private final Map<Long, User> users;
    private final U update;

    protected UpdateContext(MTProtoTelegramClient client, Map<Long, Chat> chats, Map<Long, User> users, U update) {
        this.client = Objects.requireNonNull(client, "client");
        this.chats = Objects.requireNonNull(chats, "chats");
        this.users = Objects.requireNonNull(users, "users");
        this.update = Objects.requireNonNull(update, "update");
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

    public U getUpdate() {
        return update;
    }
}
