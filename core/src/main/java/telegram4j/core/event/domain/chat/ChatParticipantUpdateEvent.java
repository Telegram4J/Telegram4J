package telegram4j.core.event.domain.chat;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.ExportedChatInvite;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.chat.ChatParticipant;
import telegram4j.core.object.chat.GroupChat;

import java.time.Instant;
import java.util.Optional;

/** Event of group chat participant modification, e.g. made admin, leaving, joining. */
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

    public ChatParticipantUpdateEvent(MTProtoTelegramClient client, Instant timestamp,
                                      @Nullable ChatParticipant oldParticipant,
                                      @Nullable ChatParticipant currentParticipant,
                                      @Nullable ExportedChatInvite invite, int qts,
                                      Chat chat, User actor) {
        super(client);
        this.timestamp = timestamp;
        this.oldParticipant = oldParticipant;
        this.currentParticipant = currentParticipant;
        this.invite = invite;
        this.qts = qts;
        this.chat = chat;
        this.actor = actor;
    }

    /**
     * Gets whether participant has left chat.
     *
     * @return {@code true} if participant left chat.
     */
    public boolean isLeftEvent() {
        return currentParticipant == null;
    }

    /**
     * Gets whether participant has join chat.
     *
     * @return {@code true} if participant has join chat.
     */
    public boolean isJointEvent() {
        return oldParticipant == null;
    }

    /**
     * Gets timestamp of this event occurring.
     *
     * @return The {@link Instant} of this event occurring.
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Gets old state of chat participant, if present.
     *
     * @return The old state of {@link ChatParticipant}, if present.
     */
    public Optional<ChatParticipant> getOldParticipant() {
        return Optional.ofNullable(oldParticipant);
    }

    /**
     * Gets current state of chat participant, if present.
     *
     * @return The current state of {@link ChatParticipant}, if present.
     */
    public Optional<ChatParticipant> getCurrentParticipant() {
        return Optional.ofNullable(currentParticipant);
    }

    /**
     * Gets invite by which the user joined chat, if present.
     *
     * @return The {@link ExportedChatInvite} by which the user joined chat, if present.
     */
    public Optional<ExportedChatInvite> getInvite() {
        return Optional.ofNullable(invite);
    }

    /**
     * Gets qts number of event.
     *
     * @return The qts number of event.
     */
    public int getQts() {
        return qts;
    }

    /**
     * Gets chat where participant was updated.
     *
     * @return The {@link GroupChat} where participant was updated.
     */
    public Chat getChat() {
        return chat;
    }

    /**
     * Gets user which triggered the update, e.g. admin, inviter.
     *
     * @return The user which triggered the update, e.g. admin, inviter.
     */
    public User getActor() {
        return actor;
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
                '}';
    }
}
