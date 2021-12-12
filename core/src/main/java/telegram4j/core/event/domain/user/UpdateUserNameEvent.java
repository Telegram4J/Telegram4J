package telegram4j.core.event.domain.user;

import telegram4j.core.MTProtoTelegramClient;

public class UpdateUserNameEvent extends UserEvent {
    private final long user_id;
    private final String first_name;
    private final String last_name;
    private final String username;

    public UpdateUserNameEvent(MTProtoTelegramClient client, long user_id, String first_name, String last_name, String username) {
        super(client);
        this.user_id = user_id;
        this.first_name = first_name;
        this.last_name = last_name;
        this.username = username;
    }

    public long getUser_id() {
        return user_id;
    }

    public String getFirst_name() {
        return first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String toString() {
        return "UpdateUserTypingEvent{" +
                "user_id=" + user_id +
                ", first_name=" + first_name +
                ", last_name=" + last_name +
                ", username=" + username +
                "} " + super.toString();
    }
}
