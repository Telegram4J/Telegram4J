package telegram4j.core.event.domain.user;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.util.Id;
import telegram4j.tl.SendMessageAction;

public class UpdateUserTypingEvent extends UserEvent {
    private final Id userId;
    private final SendMessageAction action;

    public UpdateUserTypingEvent(MTProtoTelegramClient client, Id userId, SendMessageAction action) {
        super(client);
        this.userId = userId;
        this.action = action;
    }

    public Id getUserId() {
        return userId;
    }

    public SendMessageAction getAction() {
        return action;
    }

    @Override
    public String toString() {
        return "UpdateUserTypingEvent{" +
                "userId=" + userId +
                ", action=" + action +
                '}';
    }
}
