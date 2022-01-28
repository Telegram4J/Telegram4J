package telegram4j.core.object.action;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.InputGroupCall;

import java.time.Instant;
import java.util.Objects;

public class MessageActionGroupCallScheduled extends BaseMessageAction {

    private final telegram4j.tl.MessageActionGroupCallScheduled data;

    public MessageActionGroupCallScheduled(MTProtoTelegramClient client, telegram4j.tl.MessageActionGroupCallScheduled data) {
        super(client, Type.GROUP_CALL_SCHEDULED);
        this.data = Objects.requireNonNull(data, "data");
    }

    // TODO: mapping
    public InputGroupCall getCall() {
        return data.call();
    }

    public Instant getScheduleTimestamp() {
        return Instant.ofEpochSecond(data.scheduleDate());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageActionGroupCallScheduled that = (MessageActionGroupCallScheduled) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageActionGroupCallScheduled{" +
                "data=" + data +
                '}';
    }
}
