package telegram4j.core.object.action;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Id;

import java.util.Objects;

public class MessageActionChannelMigrateFrom extends BaseMessageAction {

    private final telegram4j.tl.MessageActionChannelMigrateFrom data;

    public MessageActionChannelMigrateFrom(MTProtoTelegramClient client, telegram4j.tl.MessageActionChannelMigrateFrom data) {
        super(client, Type.CHANNEL_MIGRATE_FROM);
        this.data = Objects.requireNonNull(data, "data");
    }

    public String getTitle() {
        return data.title();
    }

    public Id getChatId() {
        return Id.ofChat(data.chatId());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageActionChannelMigrateFrom that = (MessageActionChannelMigrateFrom) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageActionChannelMigrateFrom{" +
                "data=" + data +
                '}';
    }
}
