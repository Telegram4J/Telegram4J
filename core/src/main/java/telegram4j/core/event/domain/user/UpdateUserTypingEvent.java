package telegram4j.core.event.domain.user;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.SendMessageAction;

public class UpdateUserTypingEvent extends UserEvent {
    private final long user_id;
    private final SendMessageAction action;

    public UpdateUserTypingEvent(MTProtoTelegramClient client, long user_id, SendMessageAction action) {
        super(client);
        this.user_id = user_id;
        this.action = action;
    }

    public long getUser_id() {
        return user_id;
    }

    public SendMessageAction getAction() {
        return action;
    }

    @Override
    public String toString() {
        return "UpdateUserTypingEvent{" +
                "user_id=" + user_id +
                ", action=" + action +
                "} " + super.toString();
    }
}
