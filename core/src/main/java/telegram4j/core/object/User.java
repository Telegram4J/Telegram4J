package telegram4j.core.object;

import telegram4j.TelegramClient;
import telegram4j.json.UserData;

import java.util.Objects;
import java.util.Optional;

public class User implements TelegramObject {

    private final TelegramClient client;
    private final UserData data;

    public User(TelegramClient client, UserData data) {
        this.client = client;
        this.data = data;
    }

    public UserData getData() {
        return data;
    }

    public Id getId() {
        return Id.of(data.id());
    }

    public boolean isBot() {
        return data.isBot();
    }

    public Optional<String> getFirstName() {
        return data.firstName();
    }

    public Optional<String> getLastName() {
        return data.lastName();
    }

    public Optional<String> getUsername() {
        return data.username();
    }

    public Optional<String> getLanguageCode() {
        return data.languageCode();
    }

    public boolean isCanJoinGroups() {
        return data.canJoinGroups().orElse(false);
    }

    public boolean isCanReadAllGroupMessages() {
        return data.canReadAllGroupMessages().orElse(false);
    }

    public boolean isSupportsInlineQueries() {
        return data.supportsInlineQueries().orElse(false);
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return getId().equals(user.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "User{data=" + data + '}';
    }
}
