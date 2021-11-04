package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.json.ContactData;
import telegram4j.json.api.Id;

import java.util.Objects;
import java.util.Optional;

public class Contact implements TelegramObject {

    private final TelegramClient client;
    private final ContactData data;

    public Contact(TelegramClient client, ContactData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public ContactData getData() {
        return data;
    }

    public String getPhoneNumber() {
        return data.phoneNumber();
    }

    public String getFirstName() {
        return data.firstName();
    }

    public Optional<String> getLastName() {
        return data.lastName();
    }

    public Optional<Id> getUserId() {
        return data.userId();
    }

    public Optional<String> getVcard() {
        return data.vcard();
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Contact contact = (Contact) o;
        return data.equals(contact.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "Contact{data=" + data + '}';
    }
}
