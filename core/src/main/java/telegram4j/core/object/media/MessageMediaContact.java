package telegram4j.core.object.media;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Id;

import java.util.Objects;

public class MessageMediaContact extends BaseMessageMedia {

    private final telegram4j.tl.MessageMediaContact data;

    public MessageMediaContact(MTProtoTelegramClient client, telegram4j.tl.MessageMediaContact data, int messageId) {
        super(client, Type.CONTACT, messageId);
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
}
