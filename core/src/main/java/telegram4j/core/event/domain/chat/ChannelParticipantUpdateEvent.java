package telegram4j.core.event.domain.chat;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.ExportedChatInvite;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Channel;
import telegram4j.core.object.chat.ChatParticipant;

import java.time.Instant;
import java.util.Optional;

public class ChannelParticipantUpdateEvent extends ChatEvent {

    private final Channel channel;
    private final Instant timestamp;
    private final User actor;
    @Nullable
    private final ChatParticipant oldParticipant;
    @Nullable
    private final ChatParticipant currentParticipant;
    @Nullable
    private final ExportedChatInvite invite;
    private final int qts;

    public ChannelParticipantUpdateEvent(MTProtoTelegramClient client, Channel channel, Instant timestamp,
                                         User actor, @Nullable ChatParticipant oldParticipant,
                                         @Nullable ChatParticipant currentParticipant,
                                         @Nullable ExportedChatInvite invite, int qts) {
        super(client);
        this.channel = channel;
        this.timestamp = timestamp;
        this.actor = actor;
        this.oldParticipant = oldParticipant;
        this.currentParticipant = currentParticipant;
        this.invite = invite;
        this.qts = qts;
    }

    public boolean isLeftEvent() {
        return currentParticipant == null;
    }

    public boolean isJointEvent() {
        return oldParticipant == null;
    }

    public Channel getChannel() {
        return channel;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public User getActor() {
        return actor;
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
        return "ChannelParticipantUpdateEvent{" +
                "channel=" + channel +
                ", timestamp=" + timestamp +
                ", actor=" + actor +
                ", oldParticipant=" + oldParticipant +
                ", currentParticipant=" + currentParticipant +
                ", invite=" + invite +
                ", qts=" + qts +
                '}';
    }
}
