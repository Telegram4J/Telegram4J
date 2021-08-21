package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.json.MessageData;

import java.util.Objects;
import java.util.Optional;

public class Message implements TelegramObject {

    private final TelegramClient client;
    private final MessageData data;

    public Message(TelegramClient client, MessageData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public Id getId() {
        return Id.of(data.messageId());
    }

    public Optional<User> getAuthor() {
        return data.fromUser().map(data -> new User(client, data));
    }

    public Chat getChat() {
        return new Chat(client, data.chat());
    }

    public MessageData getData() {
        return data;
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message that = (Message) o;
        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "Message{data=" + data + '}';
    }
}
