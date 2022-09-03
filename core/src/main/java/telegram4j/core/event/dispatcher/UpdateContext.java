package telegram4j.core.event.dispatcher;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.BaseUser;
import telegram4j.tl.Chat;
import telegram4j.tl.Update;

import java.util.Map;
import java.util.Objects;

public class UpdateContext<U extends Update> {
    private final MTProtoTelegramClient client;
    private final Map<Long, Chat> chats;
    private final Map<Long, BaseUser> users;
    private final U update;

    protected UpdateContext(MTProtoTelegramClient client, Map<Long, Chat> chats, Map<Long, BaseUser> users, U update) {
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
                                                             Map<Long, BaseUser> usersMap, U update) {
        return new UpdateContext<>(client, chatsMap, usersMap, update);
    }

    public MTProtoTelegramClient getClient() {
        return client;
    }

    public Map<Long, Chat> getChats() {
        return chats;
    }

    public Map<Long, BaseUser> getUsers() {
        return users;
    }

    public U getUpdate() {
        return update;
    }
}
