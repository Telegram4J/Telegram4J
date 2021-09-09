package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.json.UserData;
import telegram4j.json.api.Id;

import java.util.Objects;
import java.util.Optional;

public class User implements TelegramObject {

    private final TelegramClient client;
    private final UserData data;

    public User(TelegramClient client, UserData data) {
        this.client = client;
        this.data = data;
    }

    public Id getId() {
        return data.id();
    }

    public UserData getData() {
        return data;
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

    public Optional<Boolean> isCanJoinGroups() {
        return data.canJoinGroups();
    }

    public Optional<Boolean> isCanReadAllGroupMessages() {
        return data.canReadAllGroupMessages();
    }

    public Optional<Boolean> isSupportsInlineQueries() {
        return data.supportsInlineQueries();
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(@Nullable Object o) {
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
