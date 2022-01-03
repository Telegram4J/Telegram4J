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

    public ChatParticipantUpdateEvent(MTProtoTelegramClient client, long chat_id, Instant date, long actor_id, long user_id, @Nullable ChatParticipant prev_participant,@Nullable ChatParticipant new_participant,@Nullable ExportedChatInvite invite, int qts){
        super(client);
        this.chatId = chat_id;
        this.date = date;
        this.actorId = actor_id;
        this.userId = user_id;
        this.prevParticipant = prev_participant;
        this.newParticipant = new_participant;
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
                "chat_id=" + chatId +
                ", date=" + date +
                ", actor_id=" + actorId +
                ", user_id=" + userId +
                ", prev_participant=" + prevParticipant +
                ", new_participant=" + newParticipant +
                ", invite=" + invite +
                ", qts=" + qts +
                '}';
    }
}
