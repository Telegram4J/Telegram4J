package telegram4j.core.object.action;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;

public class MessageActionChatEditTitle extends BaseMessageAction {

    private final telegram4j.tl.MessageActionChatEditTitle data;

    public MessageActionChatEditTitle(MTProtoTelegramClient client, telegram4j.tl.MessageActionChatEditTitle data) {
        super(client, Type.CHAT_EDIT_TITLE);
        this.data = Objects.requireNonNull(data, "data");
    }

    public String getTitle() {
        return data.title();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageActionChatEditTitle that = (MessageActionChatEditTitle) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageActionChatEditTitle{" +
                "data=" + data +
                '}';
    }
}
