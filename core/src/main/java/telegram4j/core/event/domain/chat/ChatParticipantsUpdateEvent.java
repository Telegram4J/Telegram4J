package telegram4j.core.event.domain.chat;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.chat.ChatParticipant;
import telegram4j.core.object.chat.GroupChat;

import java.util.List;
import java.util.Optional;

/** Event of batch modification of group chat participants. */
public final class ChatParticipantsUpdateEvent extends ChatEvent {

    private final GroupChat chat;
    @Nullable
    private final ChatParticipant selfParticipant;
    @Nullable
    private final Integer version;
    @Nullable
    private final List<ChatParticipant> participants;

    public ChatParticipantsUpdateEvent(MTProtoTelegramClient client, GroupChat chat,
                                       @Nullable ChatParticipant selfParticipant, @Nullable Integer version,
                                       @Nullable List<ChatParticipant> participants) {
        super(client);
        this.chat = chat;
        this.selfParticipant = selfParticipant;
        this.version = version;
        this.participants = participants;
    }

    /**
     * Gets whether access to list of participants is forbidden.
     *
     * @return {@code true} if access to list of participants is forbidden.
     */
    public boolean isForbidden() {
        return version == null;
    }

    /**
     * Gets group chat where participants were updated.
     *
     * @return The {@link GroupChat} where participants were updated.
     */
    @Override
    public GroupChat getChat() {
        return chat;
    }

    /**
     * Gets self participant if {@link #isForbidden()} and <i>current</i> user is a chat participant.
     *
     * @return The {@link ChatParticipant} of self user, if present.
     */
    public Optional<ChatParticipant> getSelfParticipant() {
        return Optional.ofNullable(selfParticipant);
    }

    /**
     * Gets current version of group chat participants if {@link #isForbidden()} is {@code false}.
     *
     * @return The current version of group chat participants, if present.
     */
    public Optional<Integer> getVersion() {
        return Optional.ofNullable(version);
    }

    /**
     * Gets list of {@link ChatParticipant}s of this group chat, if {@link #isForbidden()} is {@code false}.
     *
     * @return The {@link List} of {@link ChatParticipant}s of this group chat, if present.
     */
    public Optional<List<ChatParticipant>> getParticipants() {
        return Optional.ofNullable(participants);
    }

    @Override
    public String toString() {
        return "ChatParticipantsUpdateEvent{" +
                "chat=" + chat +
                ", selfParticipant=" + selfParticipant +
                ", version=" + version +
                ", participants=" + participants +
                '}';
    }
}
