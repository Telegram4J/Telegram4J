package telegram4j.core.event.domain.chat;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Id;
import telegram4j.tl.ChatParticipant;

import java.util.List;
import java.util.Optional;

public class ChatParticipantsUpdateEvent extends ChatEvent {

    private final Id chatId;
    @Nullable
    private final ChatParticipant selfParticipant;
    @Nullable
    private final Integer version;
    @Nullable
    private final List<ChatParticipant> participants;

    public ChatParticipantsUpdateEvent(MTProtoTelegramClient client, Id chatId,
                                       @Nullable ChatParticipant selfParticipant, @Nullable Integer version,
                                       @Nullable List<ChatParticipant> participants) {
        super(client);
        this.chatId = chatId;
        this.selfParticipant = selfParticipant;
        this.version = version;
        this.participants = participants;
    }

    public boolean isForbidden() {
        return version == null;
    }

    public Id getChatId() {
        return chatId;
    }

    public Optional<ChatParticipant> getSelfParticipant() {
        return Optional.ofNullable(selfParticipant);
    }

    public Optional<Integer> getVersion() {
        return Optional.ofNullable(version);
    }

    public Optional<List<ChatParticipant>> getParticipants() {
        return Optional.ofNullable(participants);
    }

    @Override
    public String toString() {
        return "ChatParticipantsUpdateEvent{" +
                "chatId=" + chatId +
                ", selfParticipant=" + selfParticipant +
                ", version=" + version +
                ", participants=" + participants +
                '}';
    }
}
