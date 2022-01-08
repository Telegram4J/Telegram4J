package telegram4j.core.event.domain.chat;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Id;

public class ChatParticipantDeleteEvent extends ChatEvent {
    private final Id chatId;
    private final Id userId;
    private final int version;

    public ChatParticipantDeleteEvent(MTProtoTelegramClient client, Id chatId, Id userId, int version) {
        super(client);
        this.chatId = chatId;
        this.userId = userId;
        this.version = version;
    }

    public Id getChatId() {
        return chatId;
    }

    public Id getUserId() {
        return userId;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "ChatParticipantDeleteEvent{" +
                "chatId=" + chatId +
                ", userId=" + userId +
                ", version=" + version +
                '}';
    }
}
