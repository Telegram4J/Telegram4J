package telegram4j.core.object.action;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;

public class MessageActionSetChatTheme extends BaseMessageAction {

    private final telegram4j.tl.MessageActionSetChatTheme data;

    public MessageActionSetChatTheme(MTProtoTelegramClient client, telegram4j.tl.MessageActionSetChatTheme data) {
        super(client, Type.SET_CHAT_THEME);
        this.data = Objects.requireNonNull(data, "data");
    }

    public String getEmoticon() {
        return data.emoticon();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageActionSetChatTheme that = (MessageActionSetChatTheme) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageActionSetChatTheme{" +
                "data=" + data +
                '}';
    }
}
