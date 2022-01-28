package telegram4j.core.object.action;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Id;

import java.util.Objects;

public class MessageActionChatMigrateTo extends BaseMessageAction {

    private final telegram4j.tl.MessageActionChatMigrateTo data;

    public MessageActionChatMigrateTo(MTProtoTelegramClient client, telegram4j.tl.MessageActionChatMigrateTo data) {
        super(client, Type.CHAT_MIGRATE_TO);
        this.data = Objects.requireNonNull(data, "data");
    }

    public Id getChannelId() {
        return Id.ofChannel(data.channelId(), null);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageActionChatMigrateTo that = (MessageActionChatMigrateTo) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageActionChatMigrateTo{" +
                "data=" + data +
                '}';
    }
}
