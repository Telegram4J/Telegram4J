package telegram4j.core.event.domain.chat;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Id;

public class ChatParticipantAdminEvent extends ChatEvent {
    private final Id chatId;
    private final Id userId;
    private final boolean isAdmin;
    private final int version;

    public ChatParticipantAdminEvent(MTProtoTelegramClient client, Id chatId, Id userId, boolean isAdmin, int version) {
        super(client);
        this.chatId = chatId;
        this.userId = userId;
        this.isAdmin = isAdmin;
        this.version = version;
    }

    public Id getChatId() {
        return chatId;
    }

    public Id getUserId() {
        return userId;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "ChatParticipantAdminEvent{" +
                "chatId=" + chatId +
                ", userId=" + userId +
                ", isAdmin=" + isAdmin +
                ", version=" + version +
                '}';
    }
}
