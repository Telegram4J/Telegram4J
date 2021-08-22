package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.PassportData;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Passport implements TelegramObject {

    private final TelegramClient client;
    private final PassportData data;

    public Passport(TelegramClient client, PassportData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public PassportData getData() {
        return data;
    }

    public List<EncryptedPassportElement> getElements() {
        return data.data().stream()
                .map(data -> new EncryptedPassportElement(client, data))
                .collect(Collectors.toList());
    }

    public EncryptedCredentials getCredentials() {
        return new EncryptedCredentials(client, data.credentials());
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Passport that = (Passport) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        return "Passport{data=" + data + '}';
    }
}
