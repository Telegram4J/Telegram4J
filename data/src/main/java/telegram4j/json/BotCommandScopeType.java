package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonValue;

public enum BotCommandScopeType {
    DEFAULT,
    ALL_PRIVATE_CHATS,
    ALL_GROUP_CHATS,
    ALL_CHAT_ADMINISTRATORS,
    CHAT,
    CHAT_ADMINISTRATORS,
    CHAT_MEMBER;

    @JsonValue
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
