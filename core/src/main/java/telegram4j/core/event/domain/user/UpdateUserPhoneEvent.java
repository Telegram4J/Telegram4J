package telegram4j.core.event.domain.user;

import telegram4j.core.MTProtoTelegramClient;

public class UpdateUserPhoneEvent extends UserEvent {
    private final long user_id;
    private final String phone;

    public UpdateUserPhoneEvent(MTProtoTelegramClient client, long user_id, String phone) {
        super(client);
        this.user_id = user_id;
        this.phone = phone;
    }

    public long getUser_id() {
        return user_id;
    }

    public String getPhone() {
        return phone;
    }

    @Override
    public String toString() {
        return "UpdateUserTypingEvent{" +
                "user_id=" + user_id +
                ", phone=" + phone +
                "} " + super.toString();
    }
}
