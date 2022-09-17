package telegram4j.core.object.action;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;

public class MessageActionChannelCreate extends BaseMessageAction {

    private final telegram4j.tl.MessageActionChannelCreate data;

    public MessageActionChannelCreate(MTProtoTelegramClient client, telegram4j.tl.MessageActionChannelCreate data) {
        super(client, Type.CHANNEL_CREATE);
        this.data = Objects.requireNonNull(data);
    }

    public String getChannelTitle() {
        return data.title();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageActionChannelCreate that = (MessageActionChannelCreate) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageActionChannelCreate{" +
                "data=" + data +
                '}';
    }
}
