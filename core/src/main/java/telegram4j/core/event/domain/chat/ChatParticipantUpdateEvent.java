package telegram4j.core.event.domain.chat;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.ExportedChatInvite;
import telegram4j.core.object.Id;
import telegram4j.tl.ChatParticipant;

import java.time.Instant;
import java.util.Optional;

public class ChatParticipantUpdateEvent extends ChatEvent {
    private final Id chatId;
    private final Instant timestamp;
    private final Id actorId;
    private final Id userId;
    @Nullable
    private final ChatParticipant oldParticipant;
    @Nullable
    private final ChatParticipant currentParticipant;
    @Nullable
    private final ExportedChatInvite invite;
    private final int qts;

    public ChatParticipantUpdateEvent(MTProtoTelegramClient client, Id chatId,
                                      Instant timestamp, Id actorId, Id userId,
                                      @Nullable ChatParticipant oldParticipant,
                                      @Nullable ChatParticipant currentParticipant,
                                      @Nullable ExportedChatInvite invite, int qts) {
        super(client);
        this.chatId = chatId;
        this.timestamp = timestamp;
        this.actorId = actorId;
        this.userId = userId;
        this.oldParticipant = oldParticipant;
        this.currentParticipant = currentParticipant;
        this.invite = invite;
        this.qts = qts;
    }

    public Id getChatId() {
        return chatId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Id getActorId() {
        return actorId;
    }

    public Id getUserId() {
        return userId;
    }

    public Optional<ChatParticipant> getOldParticipant() {
        return Optional.ofNullable(oldParticipant);
    }

    public Optional<ChatParticipant> getCurrentParticipant() {
        return Optional.ofNullable(currentParticipant);
    }

    public Optional<ExportedChatInvite> getInvite() {
        return Optional.ofNullable(invite);
    }

    public int getQts() {
        return qts;
    }

    @Override
    public String toString() {
        return "ChatParticipantAdminEvent{" +
                "chatId=" + chatId +
                ", timestamp=" + timestamp +
                ", actorId=" + actorId +
                ", userId=" + userId +
                ", oldParticipant=" + oldParticipant +
                ", currentParticipant=" + currentParticipant +
                ", invite=" + invite +
                ", qts=" + qts +
                '}';
    }
}
