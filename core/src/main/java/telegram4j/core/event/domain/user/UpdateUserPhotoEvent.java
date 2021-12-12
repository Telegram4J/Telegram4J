package telegram4j.core.event.domain.user;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.UserProfilePhoto;

public class UpdateUserPhotoEvent extends UserEvent {
    private final long user_id;
    private final int date;
    private final UserProfilePhoto photo;
    private final boolean previous;

    public UpdateUserPhotoEvent(MTProtoTelegramClient client, long user_id, int date, UserProfilePhoto photo, boolean previous) {
        super(client);
        this.user_id = user_id;
        this.date = date;
        this.photo = photo;
        this.previous = previous;
    }

    public long getUser_id() {
        return user_id;
    }

    public int date() {
        return date;
    }

    public UserProfilePhoto getPhoto() {
        return photo;
    }

    public boolean isPrevious() {
        return previous;
    }

    @Override
    public String toString() {
        return "UpdateUserTypingEvent{" +
                "user_id=" + user_id +
                ", date=" + date +
                ", photo=" + photo +
                ", previous=" + previous +
                "} " + super.toString();
    }
}
