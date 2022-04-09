package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.util.Id;

import java.util.Objects;

public class MessageMediaContact extends BaseMessageMedia {

    private final telegram4j.tl.MessageMediaContact data;

    public MessageMediaContact(MTProtoTelegramClient client, telegram4j.tl.MessageMediaContact data) {
        super(client, Type.CONTACT);
        this.data = Objects.requireNonNull(data, "data");
    }

    public String getPhoneNumber() {
        return data.phoneNumber();
    }

    public String getFirstName() {
        return data.firstName();
    }

    public String getLastName() {
        return data.lastName();
    }

    public String getVcard() {
        return data.vcard();
    }

    public Id getUserId() {
        return Id.ofUser(data.userId(), null);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageMediaContact that = (MessageMediaContact) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageMediaContact{" +
                "data=" + data +
                '}';
    }
}
