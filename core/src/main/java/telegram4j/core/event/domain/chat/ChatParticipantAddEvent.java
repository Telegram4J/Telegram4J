package telegram4j.core.event.domain.chat;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Id;

import java.time.Instant;

public class ChatParticipantAddEvent extends ChatEvent {
    private final Id chatId;
    private final Id userId;
    private final Id inviterId;
    private final Instant timestamp;
    private final int version;

    public ChatParticipantAddEvent(MTProtoTelegramClient client, Id chatId, Id userId,
                                   Id inviterId, Instant timestamp, int version) {
        super(client);
        this.chatId = chatId;
        this.userId = userId;
        this.inviterId = inviterId;
        this.timestamp = timestamp;
        this.version = version;
    }

    public Id getChatId() {
        return chatId;
    }

    public Id getUserId() {
        return userId;
    }

    public Id getInviterId() {
        return inviterId;
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
                "chatId=" + chatId +
                ", userId=" + userId +
                ", inviterId=" + inviterId +
                ", timestamp=" + timestamp +
                ", version=" + version +
                '}';
    }
}
