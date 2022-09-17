package telegram4j.core.object.action;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.SecureValueType;

import java.util.List;
import java.util.Objects;

public class MessageActionSecureValuesSent extends BaseMessageAction {

    private final telegram4j.tl.MessageActionSecureValuesSent data;

    public MessageActionSecureValuesSent(MTProtoTelegramClient client, telegram4j.tl.MessageActionSecureValuesSent data) {
        super(client, Type.SECURE_VALUES_SENT);
        this.data = Objects.requireNonNull(data);
    }

    public List<SecureValueType> getTypes() {
        return data.types();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageActionSecureValuesSent that = (MessageActionSecureValuesSent) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageActionSecureValuesSent{" +
                "data=" + data +
                '}';
    }
}
