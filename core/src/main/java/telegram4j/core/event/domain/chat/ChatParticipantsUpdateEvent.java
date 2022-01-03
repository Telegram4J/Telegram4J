package telegram4j.core.event.domain.chat;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.ChatParticipants;

public class ChatParticipantsUpdateEvent extends ChatEvent {
    private final ChatParticipants participants;

    public ChatParticipantsUpdateEvent(MTProtoTelegramClient client, ChatParticipants participants) {
        super(client);
        this.participants = participants;
    }

    public ChatParticipants getParticipants() {
        return participants;
    }

    @Override
    public String toString() {
        return "ChatParticipantsUpdateEvent{" +
                "participants=" + participants +
                '}';
    }
}
