package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.ChatMemberData;
import telegram4j.json.ChatMemberStatus;

import java.util.Objects;

public class ChatMember implements TelegramObject {

    private final TelegramClient client;
    private final ChatMemberData data;

    public ChatMember(TelegramClient client, ChatMemberData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public ChatMemberStatus getStatus() {
        return data.status();
    }

    public User getUser() {
        return new User(client, data.user());
    }

    public ChatMemberData getData() {
        return data;
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMember that = (ChatMember) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        return "ChatMember{data=" + data + '}';
    }
}
