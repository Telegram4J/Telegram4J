package telegram4j.core.event.domain.user;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.mtproto.store.UserNameFields;

import java.util.Optional;

public class UpdateUserNameEvent extends UserEvent {
    private final long userId;
    private final String currentFirstName;
    private final String currentLastName;
    private final String currentUsername;
    @Nullable
    private final UserNameFields oldFields;

    public UpdateUserNameEvent(MTProtoTelegramClient client, long userId,
                               String currentFirstName, String currentLastName, String currentUsername,
                               @Nullable UserNameFields oldFields) {
        super(client);
        this.userId = userId;
        this.currentFirstName = currentFirstName;
        this.currentLastName = currentLastName;
        this.currentUsername = currentUsername;
        this.oldFields = oldFields;
    }

    public long getUserId() {
        return userId;
    }

    public String getCurrentFirstName() {
        return currentFirstName;
    }

    public String getCurrentLastName() {
        return currentLastName;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public Optional<String> getOldFirstName() {
        return Optional.ofNullable(oldFields).map(UserNameFields::getFirstName);
    }

    public Optional<String> getOldLastName() {
        return Optional.ofNullable(oldFields).map(UserNameFields::getLastName);
    }

    public Optional<String> getOldUsername() {
        return Optional.ofNullable(oldFields).map(UserNameFields::getUserName);
    }

    @Override
    public String toString() {
        return "UpdateUserNameEvent{" +
                "userId=" + userId +
                ", currentFirstName='" + currentFirstName + '\'' +
                ", currentLastName='" + currentLastName + '\'' +
                ", currentUsername='" + currentUsername + '\'' +
                ", oldFields=" + oldFields +
                "} " + super.toString();
    }
}
