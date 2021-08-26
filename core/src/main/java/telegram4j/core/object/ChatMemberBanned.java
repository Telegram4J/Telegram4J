package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.ChatMemberData;

import java.time.Instant;

public final class ChatMemberBanned extends ChatMember {

    public ChatMemberBanned(TelegramClient client, ChatMemberData data) {
        super(client, data);
    }

    public Instant getUntilTimestamp() {
        return getData().untilDate().filter(i -> i > 0).map(Instant::ofEpochSecond).orElse(Instant.MIN);
    }

    @Override
    public String toString() {
        return "ChatMemberBanned{} " + super.toString();
    }
}
