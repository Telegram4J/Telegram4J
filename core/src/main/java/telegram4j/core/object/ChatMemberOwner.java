package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.ChatMemberData;

import java.util.Optional;

public final class ChatMemberOwner extends ChatMember {

    public ChatMemberOwner(TelegramClient client, ChatMemberData data) {
        super(client, data);
    }

    public boolean isAnonymous() {
        return getData().isAnonymous().orElseThrow(IllegalStateException::new); // must be always present
    }

    public Optional<String> getCustomTitle() {
        return getData().customTitle();
    }

    @Override
    public String toString() {
        return "ChatMemberOwner{} " + super.toString();
    }
}
