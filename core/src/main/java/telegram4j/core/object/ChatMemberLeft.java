package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.ChatMemberData;

public final class ChatMemberLeft extends ChatMember {

    public ChatMemberLeft(TelegramClient client, ChatMemberData data) {
        super(client, data);
    }

    @Override
    public String toString() {
        return "ChatMemberLeft{} " + super.toString();
    }
}
