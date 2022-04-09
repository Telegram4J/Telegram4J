package telegram4j.core.object.action;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.util.Id;

import java.util.Objects;

public class MessageActionChatDeleteUser extends BaseMessageAction {

    private final telegram4j.tl.MessageActionChatDeleteUser data;

    public MessageActionChatDeleteUser(MTProtoTelegramClient client, telegram4j.tl.MessageActionChatDeleteUser data) {
        super(client, Type.CHAT_DELETE_USER);
        this.data = Objects.requireNonNull(data, "data");
    }

    public Id getUserId() {
        return Id.ofUser(data.userId(), null);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageActionChatDeleteUser that = (MessageActionChatDeleteUser) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageActionChatDeleteUser{" +
                "data=" + data +
                '}';
    }
}
