package telegram4j.core.event.domain.user;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.UserProfilePhoto;

import java.time.Instant;

public class UpdateUserPhotoEvent extends UserEvent {
    private final long userId;
    private final Instant timestamp;
    private final UserProfilePhoto photo;
    private final boolean previous;

    public UpdateUserPhotoEvent(MTProtoTelegramClient client, long userId, int timestamp, UserProfilePhoto photo, boolean previous) {
        super(client);
        this.userId = userId;
        this.timestamp = Instant.ofEpochSecond(timestamp);
        this.photo = photo;
        this.previous = previous;
    }

    public long getUserId() {
        return userId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public UserProfilePhoto getPhoto() {
        return photo;
    }

    public boolean isPrevious() {
        return previous;
    }

    @Override
    public String toString() {
        return "UpdateUserPhotoEvent{" +
                "userId=" + userId +
                ", timestamp=" + timestamp +
                ", photo=" + photo +
                ", previous=" + previous +
                "} " + super.toString();
    }
}
