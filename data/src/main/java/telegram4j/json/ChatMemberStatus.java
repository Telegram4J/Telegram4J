package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ChatMemberStatus {
    CREATOR,
    ADMINISTRATOR,
    MEMBER,
    RESTRICTED,
    LEFT,
    KICKED;

    @JsonValue
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
