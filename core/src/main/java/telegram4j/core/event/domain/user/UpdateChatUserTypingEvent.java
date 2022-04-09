package telegram4j.core.event.domain.user;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.util.Id;
import telegram4j.tl.SendMessageAction;

public class UpdateChatUserTypingEvent extends UserEvent {
    private final Id chatId;
    private final Id fromId;
    private final SendMessageAction action;

    public UpdateChatUserTypingEvent(MTProtoTelegramClient client, Id chatId, Id fromId, SendMessageAction action) {
        super(client);
        this.chatId = chatId;
        this.fromId = fromId;
        this.action = action;
    }

    public Id getChatId() {
        return chatId;
    }

    public Id getFromId() {
        return fromId;
    }

    public SendMessageAction getAction() {
        return action;
    }

    @Override
    public String toString() {
        return "UpdateChatUserTypingEvent{" +
                "chatId=" + chatId +
                ", fromId=" + fromId +
                ", action=" + action +
                '}';
    }
}
