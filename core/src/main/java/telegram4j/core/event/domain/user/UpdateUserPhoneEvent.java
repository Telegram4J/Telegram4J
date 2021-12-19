package telegram4j.core.event.domain.user;

import telegram4j.core.MTProtoTelegramClient;

public class UpdateUserPhoneEvent extends UserEvent {
    private final long userId;
    private final String phone;

    public UpdateUserPhoneEvent(MTProtoTelegramClient client, long userId, String phone) {
        super(client);
        this.userId = userId;
        this.phone = phone;
    }

    public long getUserId() {
        return userId;
    }

    public String getPhone() {
        return phone;
    }

    @Override
    public String toString() {
        return "UpdateUserTypingEvent{" +
                "user_id=" + userId +
                ", phone=" + phone +
                "} " + super.toString();
    }
}
