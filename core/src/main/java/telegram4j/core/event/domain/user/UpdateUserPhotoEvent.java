package telegram4j.core.event.domain.user;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.ChatPhoto;
import telegram4j.core.object.Id;
import telegram4j.tl.UserProfilePhoto;

import java.time.Instant;
import java.util.Optional;

public class UpdateUserPhotoEvent extends UserEvent {
    private final Id userId;
    private final Instant timestamp;
    private final ChatPhoto currentPhoto;
    @Nullable
    private final ChatPhoto oldPhoto;
    private final boolean previous;

    public UpdateUserPhotoEvent(MTProtoTelegramClient client, Id userId, Instant timestamp,
                                ChatPhoto currentPhoto, @Nullable ChatPhoto oldPhoto, boolean previous) {
        super(client);
        this.userId = userId;
        this.timestamp = timestamp;
        this.currentPhoto = currentPhoto;
        this.oldPhoto = oldPhoto;
        this.previous = previous;
    }

    public Id getUserId() {
        return userId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public ChatPhoto getCurrentPhoto() {
        return currentPhoto;
    }

    public Optional<ChatPhoto> getOldPhoto() {
        return Optional.ofNullable(oldPhoto);
    }

    public boolean isPrevious() {
        return previous;
    }

    @Override
    public String toString() {
        return "UpdateUserPhotoEvent{" +
                "userId=" + userId +
                ", timestamp=" + timestamp +
                ", currentPhoto=" + currentPhoto +
                ", oldPhoto=" + oldPhoto +
                ", previous=" + previous +
                '}';
    }
}
