package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonValue;

public enum InputMediaType {
    ANIMATION,
    DOCUMENT,
    AUDIO,
    PHOTO,
    VIDEO;

    @JsonValue
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
