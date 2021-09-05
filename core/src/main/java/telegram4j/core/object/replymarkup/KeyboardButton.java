package telegram4j.core.object.replymarkup;

import telegram4j.json.KeyboardButtonData;
import telegram4j.json.KeyboardButtonPollType;

import java.util.Objects;
import java.util.Optional;

public final class KeyboardButton {

    private final KeyboardButtonData data;

    public KeyboardButton(KeyboardButtonData data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    public static KeyboardButton fromData(KeyboardButtonData data) {
        return new KeyboardButton(data);
    }

    public KeyboardButtonData getData() {
        return data;
    }

    public String getText() {
        return data.text();
    }

    public Optional<Boolean> isRequestContact() {
        return data.requestContact();
    }

    public Optional<Boolean> isRequestLocation() {
        return data.requestLocation();
    }

    public Optional<KeyboardButtonPollType> getRequestPoll() {
        return data.requestPoll();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyboardButton that = (KeyboardButton) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        return "KeyboardButton{data=" + data + '}';
    }
}
