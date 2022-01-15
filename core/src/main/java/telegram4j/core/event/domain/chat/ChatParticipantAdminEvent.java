package telegram4j.core.event.domain.chat;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.GroupChat;

public class ChatParticipantAdminEvent extends ChatEvent {
    private final GroupChat chat;
    private final User user;
    private final boolean isAdmin;
    private final int version;

    public ChatParticipantAdminEvent(MTProtoTelegramClient client, GroupChat chat, User user, boolean isAdmin, int version) {
        super(client);
        this.chat = chat;
        this.user = user;
        this.isAdmin = isAdmin;
        this.version = version;
    }

    public GroupChat getChat() {
        return chat;
    }

    public User getUser() {
        return user;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "ChatParticipantAdminEvent{" +
                "chat=" + chat +
                ", user=" + user +
                ", isAdmin=" + isAdmin +
                ", version=" + version +
                "} " + super.toString();
    }
}
