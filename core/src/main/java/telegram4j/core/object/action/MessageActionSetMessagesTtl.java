package telegram4j.core.object.action;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.time.Duration;
import java.util.Objects;

public class MessageActionSetMessagesTtl extends BaseMessageAction {

    private final telegram4j.tl.MessageActionSetMessagesTTL data;

    public MessageActionSetMessagesTtl(MTProtoTelegramClient client, telegram4j.tl.MessageActionSetMessagesTTL data) {
        super(client, Type.SET_MESSAGES_TTL);
        this.data = Objects.requireNonNull(data, "data");
    }

    public Duration getCurrentDuration() {
        return Duration.ofSeconds(data.period());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageActionSetMessagesTtl that = (MessageActionSetMessagesTtl) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageActionSetMessagesTTL{" +
                "data=" + data +
                '}';
    }
}
