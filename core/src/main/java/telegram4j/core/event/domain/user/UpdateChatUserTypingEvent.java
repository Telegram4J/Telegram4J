package telegram4j.core.event.domain.user;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.Peer;
import telegram4j.tl.SendMessageAction;

public class UpdateChatUserTypingEvent extends UserEvent {
    private final long chat_id;
    private final SendMessageAction action;
    private final Peer from_id;

    public UpdateChatUserTypingEvent(MTProtoTelegramClient client, long chat_id, Peer from_id, SendMessageAction action) {
        super(client);
        this.chat_id = chat_id;
        this.from_id = from_id;
        this.action = action;
    }

    public long getChat_id() {
        return chat_id;
    }

    public Peer getFrom_id() {
        return from_id;
    }

    public SendMessageAction getAction() {
        return action;
    }

    @Override
    public String toString() {
        return "UpdateUserTypingEvent{" +
                "chat_id=" + chat_id +
                "from_id=" + from_id +
                ", action=" + action +
                "} " + super.toString();
    }
}
