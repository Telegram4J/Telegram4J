package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.json.MessageEntityData;
import telegram4j.json.MessageEntityType;

import java.util.Objects;
import java.util.Optional;

public class MessageEntity implements TelegramObject {

    private final TelegramClient client;
    private final MessageEntityData data;
    private final String content;

    public MessageEntity(TelegramClient client, MessageEntityData data, String content) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
        Objects.requireNonNull(content, "content");
        this.content = content.substring(getOffset(), getOffset() + getLength());
    }

    public MessageEntityData getData() {
        return data;
    }

    public MessageEntityType getType() {
        return data.type();
    }

    public int getOffset() {
        return data.offset();
    }

    public int getLength() {
        return data.length();
    }

    public Optional<String> getUrl() {
        return data.url();
    }

    public Optional<User> getUser() {
        return data.user().map(data -> new User(client, data));
    }

    public Optional<String> getLanguage() {
        return data.language();
    }

    public String getContent() {
        return content;
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageEntity that = (MessageEntity) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageEntity{" +
                "data=" + data +
                ", content='" + content + '\'' +
                '}';
    }
}
