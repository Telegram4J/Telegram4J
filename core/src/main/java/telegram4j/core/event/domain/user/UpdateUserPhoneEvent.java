package telegram4j.core.event.domain.user;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Id;

import java.util.Optional;

public class UpdateUserPhoneEvent extends UserEvent {
    private final Id userId;
    private final String phone;
    @Nullable
    private final String oldPhone;

    public UpdateUserPhoneEvent(MTProtoTelegramClient client, Id userId, String phone, @Nullable String oldPhone) {
        super(client);
        this.userId = userId;
        this.phone = phone;
        this.oldPhone = oldPhone;
    }

    public Id getUserId() {
        return userId;
    }

    public String getPhone() {
        return phone;
    }

    public Optional<String> getOldPhone() {
        return Optional.ofNullable(oldPhone);
    }

    @Override
    public String toString() {
        return "UpdateUserPhoneEvent{" +
                "userId=" + userId +
                ", phone='" + phone + '\'' +
                ", oldPhone='" + oldPhone + '\'' +
                '}';
    }
}
