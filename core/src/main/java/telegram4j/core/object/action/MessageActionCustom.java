package telegram4j.core.object.action;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;

public class MessageActionCustom extends BaseMessageAction {

    private final telegram4j.tl.MessageActionCustomAction data;

    public MessageActionCustom(MTProtoTelegramClient client, telegram4j.tl.MessageActionCustomAction data) {
        super(client, Type.CUSTOM);
        this.data = Objects.requireNonNull(data, "data");
    }

    public String getMessage() {
        return data.message();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageActionCustom that = (MessageActionCustom) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageActionCustomAction{" +
                "data=" + data +
                '}';
    }
}
