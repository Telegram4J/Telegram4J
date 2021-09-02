package telegram4j.json.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import reactor.util.annotation.Nullable;

public final class Id {

    private final long id;

    private Id(long id) {
        this.id = id;
    }

    @JsonCreator
    public static Id of(long id) {
        return new Id(id);
    }

    @JsonCreator
    public static Id of(String id) {
        return new Id(Long.parseLong(id));
    }

    public long asLong() {
        return id;
    }

    @JsonValue
    public String asString() {
        return Long.toString(id);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Id that = (Id) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return "Id{" + id + '}';
    }
}
