package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PollType {

    REGULAR,
    QUIZ;

    @JsonValue
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
