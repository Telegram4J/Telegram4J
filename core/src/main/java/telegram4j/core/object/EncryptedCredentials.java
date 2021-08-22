package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.EncryptedCredentialsData;

import java.util.Objects;

public class EncryptedCredentials implements TelegramObject {

    private final TelegramClient client;
    private final EncryptedCredentialsData data;

    public EncryptedCredentials(TelegramClient client, EncryptedCredentialsData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public EncryptedCredentialsData getData() {
        return data;
    }

    // TODO: rename this method
    public String getJson() {
        return data.data();
    }

    public String getHash() {
        return data.hash();
    }

    public String getSecret() {
        return data.secret();
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EncryptedCredentials that = (EncryptedCredentials) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        return "EncryptedCredentials{data=" + data + '}';
    }
}
