package telegram4j.core.event.domain.chat;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;

import java.time.Instant;

public class ChatParticipantAddEvent extends ChatEvent {
    private final Chat chat;
    private final User user;
    private final User inviter;
    private final Instant timestamp;
    private final int version;

    public ChatParticipantAddEvent(MTProtoTelegramClient client, Chat chat, User user,
                                   User inviter, Instant timestamp, int version) {
        super(client);
        this.chat = chat;
        this.user = user;
        this.inviter = inviter;
        this.timestamp = timestamp;
        this.version = version;
    }

    public Chat getChat() {
        return chat;
    }

    public User getUser() {
        return user;
    }

    public User getInviter() {
        return inviter;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "ChatParticipantAddEvent{" +
                "chat=" + chat +
                ", user=" + user +
                ", inviter=" + inviter +
                ", timestamp=" + timestamp +
                ", version=" + version +
                "} " + super.toString();
    }
}
