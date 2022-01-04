package telegram4j.core.event.domain.chat;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.ChatParticipant;
import telegram4j.tl.ExportedChatInvite;

import java.time.Instant;
import java.util.Optional;

public class ChatParticipantUpdateEvent extends ChatEvent{
    private final long chatId;
    private final Instant date;
    private final long actorId;
    private final long userId;
    @Nullable
    private final ChatParticipant prevParticipant;
    @Nullable
    private final ChatParticipant newParticipant;
    @Nullable
    private final ExportedChatInvite invite;
    private final int qts;

    public ChatParticipantUpdateEvent(MTProtoTelegramClient client, long chatId, Instant date, long actorId, long userId, @Nullable ChatParticipant prevParticipant,@Nullable ChatParticipant newParticipant,@Nullable ExportedChatInvite invite, int qts){
        super(client);
        this.chatId = chatId;
        this.date = date;
        this.actorId = actorId;
        this.userId = userId;
        this.prevParticipant = prevParticipant;
        this.newParticipant = newParticipant;
        this.invite = invite;
        this.qts = qts;
    }

    public long getChatId() {
        return chatId;
    }
    public Instant getDate() {
        return date;
    }

    public long getActorId() {
        return actorId;
    }

    public long getUserId() {
        return userId;
    }

    public Optional<ChatParticipant> getPrevParticipant(){
        return Optional.ofNullable(prevParticipant);
    }

    public Optional<ChatParticipant> getNewParticipant(){
        return Optional.ofNullable(newParticipant);
    }

    public Optional<ExportedChatInvite> getInvite(){
        return Optional.ofNullable(invite);
    }

    public int getQts() {
        return qts;
    }

    @Override
    public String toString() {
        return "ChatParticipantAdminEvent{" +
                "chatId=" + chatId +
                ", date=" + date +
                ", actorId=" + actorId +
                ", userId=" + userId +
                ", prevParticipant=" + prevParticipant +
                ", newParticipant=" + newParticipant +
                ", invite=" + invite +
                ", qts=" + qts +
                '}';
    }
}
