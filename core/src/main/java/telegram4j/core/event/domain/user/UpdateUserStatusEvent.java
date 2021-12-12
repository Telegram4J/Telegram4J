package telegram4j.core.event.domain.user;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.UserStatus;

public class UpdateUserStatusEvent extends UserEvent {
    private final long user_id;
    private final UserStatus status;

    public UpdateUserStatusEvent(MTProtoTelegramClient client, long user_id, UserStatus status) {
        super(client);
        this.user_id = user_id;
        this.status = status;
    }

    public long getUser_id() {
        return user_id;
    }

    public UserStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "UpdateUserTypingEvent{" +
                "user_id=" + user_id +
                ", status=" + status +
                "} " + super.toString();
    }
}
