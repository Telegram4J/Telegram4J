package telegram4j.json.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;
import java.util.Optional;

public final class ChatId {

    private final String id;

    private ChatId(String id) {
        this.id = id;
    }

    public static ChatId of(Id id) {
        return new ChatId(id.asString());
    }

    @JsonCreator
    public static ChatId of(long id) {
        return new ChatId(Long.toString(id));
    }

    @JsonCreator
    public static ChatId of(String id) {
        if (!id.startsWith("@")) {
            throw new IllegalArgumentException("Incorrect chat id format for value: '" + id + "'");
        }
        return new ChatId(id);
    }

    public Optional<String> asUsername() {
        return id.startsWith("@") ? Optional.of(id) : Optional.empty();
    }

    public Optional<Id> asId() {
        return id.startsWith("@") ? Optional.empty() : Optional.of(Id.of(id));
    }

    @JsonValue
    public String asString() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatId chatId = (ChatId) o;
        return id.equals(chatId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "ChatId{" + id + '}';
    }
}
