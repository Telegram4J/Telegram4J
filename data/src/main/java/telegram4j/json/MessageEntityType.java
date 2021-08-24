package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MessageEntityType {

    MENTION,
    HASHTAG,
    CASHTAG,
    BOT_COMMAND,
    URL,
    EMAIL,
    PHONE_NUMBER,
    BOLD,
    ITALIC,
    UNDERLINE,
    STRIKETHROUGH,
    CODE,
    PRE,
    TEXT_LINK,
    TEXT_MENTION;

    @JsonValue
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
