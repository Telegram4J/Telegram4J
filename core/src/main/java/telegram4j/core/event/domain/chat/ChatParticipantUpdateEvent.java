package telegram4j.core.event.domain.chat;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.ExportedChatInvite;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.chat.ChatParticipant;

import java.time.Instant;
import java.util.Optional;

public class ChatParticipantUpdateEvent extends ChatEvent {
    private final Instant timestamp;
    @Nullable
    private final ChatParticipant oldParticipant;
    @Nullable
    private final ChatParticipant currentParticipant;
    @Nullable
    private final ExportedChatInvite invite;
    private final int qts;
    private final Chat chat;
    private final User actor;
    private final User user;

    public ChatParticipantUpdateEvent(MTProtoTelegramClient client, Instant timestamp,
                                      @Nullable ChatParticipant oldParticipant,
                                      @Nullable ChatParticipant currentParticipant,
                                      @Nullable ExportedChatInvite invite, int qts,
                                      Chat chat, User actor, User user) {
        super(client);
        this.timestamp = timestamp;
        this.oldParticipant = oldParticipant;
        this.currentParticipant = currentParticipant;
        this.invite = invite;
        this.qts = qts;
        this.chat = chat;
        this.actor = actor;
        this.user = user;
    }

    public boolean isLeftEvent() {
        return currentParticipant == null;
    }

    public boolean isJointEvent() {
        return oldParticipant == null;
    }

    public Instant getTimestamp() {
        return timestamp;
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

    public Chat getChat() {
        return chat;
    }

    public User getActor() {
        return actor;
    }

    public User getUser() {
        return user;
    }

    @Override
    public String toString() {
        return "ChatParticipantUpdateEvent{" +
                "timestamp=" + timestamp +
                ", oldParticipant=" + oldParticipant +
                ", currentParticipant=" + currentParticipant +
                ", invite=" + invite +
                ", qts=" + qts +
                ", chat=" + chat +
                ", actor=" + actor +
                ", user=" + user +
                "} " + super.toString();
    }
}
