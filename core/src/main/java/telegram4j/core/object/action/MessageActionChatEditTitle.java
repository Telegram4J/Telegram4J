package telegram4j.core.object.action;

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
}
