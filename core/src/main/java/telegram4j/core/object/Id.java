package telegram4j.core.object;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Id id1 = (Id) o;
        return id == id1.id;
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
