package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ChatType {
    PRIVATE,
    GROUP,
    SUPERGROUP,
    CHANNEL;

    @JsonValue
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
