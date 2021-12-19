package telegram4j.core.event.domain.user;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.UserStatus;

public class UpdateUserStatusEvent extends UserEvent {
    private final long userId;
    private final UserStatus status;

    public UpdateUserStatusEvent(MTProtoTelegramClient client, long userId, UserStatus status) {
        super(client);
        this.userId = userId;
        this.status = status;
    }

    public long getUserId() {
        return userId;
    }

    public UserStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "UpdateUserTypingEvent{" +
                "user_id=" + userId +
                ", status=" + status +
                "} " + super.toString();
    }
}
