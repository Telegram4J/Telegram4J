package telegram4j.core.event.domain.chat;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.GroupChat;

public class ChatParticipantDeleteEvent extends ChatEvent {
    private final GroupChat chat;
    private final User user;
    private final int version;

    public ChatParticipantDeleteEvent(MTProtoTelegramClient client, GroupChat chat, User user, int version) {
        super(client);
        this.chat = chat;
        this.user = user;
        this.version = version;
    }

    public GroupChat getChat() {
        return chat;
    }

    public User getUser() {
        return user;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "ChatParticipantDeleteEvent{" +
                "chat=" + chat +
                ", user=" + user +
                ", version=" + version +
                '}';
    }
}
