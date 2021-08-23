package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.LoginUrlData;

import java.util.Objects;
import java.util.Optional;

public class LoginUrl implements TelegramObject {

    private final TelegramClient client;
    private final LoginUrlData data;

    public LoginUrl(TelegramClient client, LoginUrlData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public LoginUrlData getData() {
        return data;
    }

    public String getUrl() {
        return data.url();
    }

    public Optional<String> getForwardText() {
        return data.forwardText();
    }

    public Optional<String> getBotUsername() {
        return data.botUsername();
    }

    public boolean isRequestWriteAccess() {
        return data.requestWriteAccess();
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoginUrl that = (LoginUrl) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        return "LoginUrl{data=" + data + '}';
    }
}