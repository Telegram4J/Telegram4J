package telegram4j.core.event.domain.user;

import telegram4j.core.MTProtoTelegramClient;

public class UpdateUserNameEvent extends UserEvent {
    private final long userId;
    private final String firstName;
    private final String lastName;
    private final String username;

    public UpdateUserNameEvent(MTProtoTelegramClient client, long userId, String firstName, String lastName, String username) {
        super(client);
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
    }

    public long getUserId() {
        return userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String toString() {
        return "UpdateUserTypingEvent{" +
                "user_id=" + userId +
                ", first_name=" + firstName +
                ", last_name=" + lastName +
                ", username=" + username +
                "} " + super.toString();
    }
}
