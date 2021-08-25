package telegram4j.core.util;

import telegram4j.core.TelegramClient;
import telegram4j.core.object.*;
import telegram4j.json.ChatMemberData;

public final class EntityUtil {

    private EntityUtil() {}

    public static ChatMember getChatMember(TelegramClient client, ChatMemberData data) {
        switch (data.status()) {
            case LEFT: return new ChatMemberLeft(client, data);
            case CREATOR: return new ChatMemberOwner(client, data);
            case KICKED: return new ChatMemberBanned(client, data);
            case MEMBER:  return new ChatMember(client, data);
            case RESTRICTED: return new ChatMemberRestricted(client, data);
            case ADMINISTRATOR: return new ChatMemberAdministrator(client, data);
            default: throw new IllegalStateException("Unknown chat member status: " + data.status());
        }
    }
}
