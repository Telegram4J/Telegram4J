package telegram4j.core.object.action;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.PhoneCallDiscardReason;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public class MessageActionPhoneCall extends BaseMessageAction {

    private final telegram4j.tl.MessageActionPhoneCall data;

    public MessageActionPhoneCall(MTProtoTelegramClient client, telegram4j.tl.MessageActionPhoneCall data) {
        super(client, Type.PHONE_CALL);
        this.data = Objects.requireNonNull(data, "data");
    }

    public boolean isVideo() {
        return data.video();
    }

    public long getCallId() {
        return data.callId();
    }

    public Optional<PhoneCallDiscardReason> reason() {
        return Optional.ofNullable(data.reason());
    }

    public Optional<Duration> getDuration() {
        return Optional.ofNullable(data.duration()).map(Duration::ofSeconds);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageActionPhoneCall that = (MessageActionPhoneCall) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageActionPhoneCall{" +
                "data=" + data +
                '}';
    }
}
