package telegram4j.core.event.domain.user;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Id;
import telegram4j.core.object.UserStatus;

import java.util.Optional;

public class UpdateUserStatusEvent extends UserEvent {
    private final Id userId;
    private final UserStatus currentStatus;
    @Nullable
    private final UserStatus oldStatus;

    public UpdateUserStatusEvent(MTProtoTelegramClient client, Id userId,
                                 UserStatus currentStatus, @Nullable UserStatus oldStatus) {
        super(client);
        this.userId = userId;
        this.currentStatus = currentStatus;
        this.oldStatus = oldStatus;
    }

    public Id getUserId() {
        return userId;
    }

    public UserStatus getCurrentStatus() {
        return currentStatus;
    }

    public Optional<UserStatus> getOldStatus() {
        return Optional.ofNullable(oldStatus);
    }

    @Override
    public String toString() {
        return "UpdateUserStatusEvent{" +
                "userId=" + userId +
                ", currentStatus=" + currentStatus +
                ", oldStatus=" + oldStatus +
                "} " + super.toString();
    }
}
