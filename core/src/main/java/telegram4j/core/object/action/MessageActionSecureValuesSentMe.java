package telegram4j.core.object.action;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.SecureCredentialsEncrypted;
import telegram4j.tl.SecureValue;

import java.util.List;
import java.util.Objects;

public class MessageActionSecureValuesSentMe extends BaseMessageAction {

    private final telegram4j.tl.MessageActionSecureValuesSentMe data;

    public MessageActionSecureValuesSentMe(MTProtoTelegramClient client, telegram4j.tl.MessageActionSecureValuesSentMe data) {
        super(client, Type.SECURE_VALUES_SENT_ME);
        this.data = Objects.requireNonNull(data);
    }

    public List<SecureValue> getValues() {
        return data.values();
    }

    public SecureCredentialsEncrypted getCredentials() {
        return data.credentials();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageActionSecureValuesSentMe that = (MessageActionSecureValuesSentMe) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageActionSecureValuesSentMe{" +
                "data=" + data +
                '}';
    }
}
