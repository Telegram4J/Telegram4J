package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ChatMemberType {
    OWNER,
    ADMINISTRATOR,
    MEMBER,
    RESTRICTED,
    LEFT,
    BANNED;

    @JsonValue
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
