package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.json.ChatType;
import telegram4j.json.InlineQueryData;

import java.util.Objects;
import java.util.Optional;

public class InlineQuery implements TelegramObject {

    private final TelegramClient client;
    private final InlineQueryData data;

    public InlineQuery(TelegramClient client, InlineQueryData data) {
        this.client = client;
        this.data = data;
    }

    public InlineQueryData getData() {
        return data;
    }

    public String id() {
        return data.id();
    }

    public User getFromUser() {
        return new User(client, data.fromUser());
    }

    public String getQuery() {
        return data.query();
    }

    public String getOffset() {
        return data.offset();
    }

    public Optional<ChatType> getChatType() {
        return data.chatType();
    }

    public Optional<Location> getLocation() {
        return data.location().map(data -> new Location(client, data));
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InlineQuery that = (InlineQuery) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "InlineQuery{" +
                "data=" + data +
                '}';
    }
}
