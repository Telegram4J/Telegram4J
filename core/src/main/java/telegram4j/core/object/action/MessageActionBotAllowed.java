package telegram4j.core.object.action;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;

public class MessageActionBotAllowed extends BaseMessageAction {

    private final telegram4j.tl.MessageActionBotAllowed data;

    public MessageActionBotAllowed(MTProtoTelegramClient client, telegram4j.tl.MessageActionBotAllowed data) {
        super(client, Type.BOT_ALLOWED);
        this.data = Objects.requireNonNull(data);
    }

    public String getDomain() {
        return data.domain();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageActionBotAllowed that = (MessageActionBotAllowed) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageActionBotAllowed{" +
                "data=" + data +
                '}';
    }
}
