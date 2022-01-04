package telegram4j.core.event.domain.chat;

import telegram4j.core.MTProtoTelegramClient;

public class ChatParticipantAdminEvent extends ChatEvent{
    private final long chatId;
    private final long userId;
    private final boolean isAdmin;
    private final int version;

    public ChatParticipantAdminEvent(MTProtoTelegramClient client, long chat, long user,boolean isAdmin, int version) {
        super(client);
        chatId = chat;
        userId = user;
        this.isAdmin = isAdmin;
        this.version = version;
    }

    public long getChatId() {
        return chatId;
    }

    public long getUserId() {
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
