package telegram4j.core.event.domain.chat;

import telegram4j.core.MTProtoTelegramClient;

import java.time.Instant;

public class ChatParticipantAddEvent extends ChatEvent {
    private final long chatId;
    private final long userId;
    private final long inviterId;
    private final Instant date;
    private final int version;

    public ChatParticipantAddEvent(MTProtoTelegramClient client, long chat, long user, long inviter,Instant date,int version) {
        super(client);
        chatId = chat;
        userId = user;
        inviterId = inviter;
        this.date = date;
        this.version = version;
    }

    public long getChatId() {
        return chatId;
    }

    public long getUserId() {
        return userId;
    }

    public long getInviterId() {
        return inviterId;
    }

    public Instant getDate() {
        return date;
    }

    public int getVersion(){
        return version;
    }

    @Override
    public String toString() {
        return "ChatParticipantAddEvent{" +
                "chatId=" + chatId +
                ", userId=" + userId +
                ", inviterId=" + inviterId +
                ", date=" + date +
                ", version=" + version +
                '}';
    }
}
