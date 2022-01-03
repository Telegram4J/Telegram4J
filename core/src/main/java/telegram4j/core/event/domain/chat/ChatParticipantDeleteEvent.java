package telegram4j.core.event.domain.chat;

import telegram4j.core.MTProtoTelegramClient;

public class ChatParticipantDeleteEvent extends ChatEvent {
    private final long chatId;
    private final long userId;
    private final int version;

    public ChatParticipantDeleteEvent(MTProtoTelegramClient client, long chat, long user, int version) {
        super(client);
        chatId = chat;
        userId = user;
        this.version = version;
    }

    public long getChatId() {
        return chatId;
    }

    public long getUserId() {
        return userId;
    }

    public int getVersion(){
        return version;
    }

    @Override
    public String toString() {
        return "ChatParticipantDeleteEvent{" +
                "chat_id=" + chatId +
                ", user_id=" + userId +
                ", version=" + version +
                '}';
    }
}
