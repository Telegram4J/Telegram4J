package telegram4j.core.object.action;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.InputGroupCall;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public class MessageActionGroupCall extends BaseMessageAction {

    private final telegram4j.tl.MessageActionGroupCall data;

    public MessageActionGroupCall(MTProtoTelegramClient client, telegram4j.tl.MessageActionGroupCall data) {
        super(client, Type.GROUP_CALL);
        this.data = Objects.requireNonNull(data);
    }

    public InputGroupCall getCall() {
        return data.call();
    }

    public Optional<Duration> getDuration() {
        return Optional.ofNullable(data.duration()).map(Duration::ofSeconds);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageActionGroupCall that = (MessageActionGroupCall) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageActionGroupCall{" +
                "data=" + data +
                '}';
    }
}
