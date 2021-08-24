package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonValue;

public enum KeyboardButtonPollType {
    QUIZ,
    REGULAR;

    @JsonValue
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
