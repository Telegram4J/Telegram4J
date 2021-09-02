package telegram4j.json.encoding;

import org.immutables.encode.Encoding;
import telegram4j.json.api.Id;

import java.util.Objects;

@Encoding
public class IdEncoding {

    @Encoding.Impl(virtual = true)
    private Id id;

    private final long value = id.asLong();

    @Encoding.Expose
    Id get() {
        return Id.of(value);
    }

    @Encoding.Copy
    public Id withLong(long value) {
        return Id.of(value);
    }

    @Encoding.Copy
    public Id withString(String value) {
        return Id.of(value);
    }

    @Override
    public String toString() {
        return Objects.toString(value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    public boolean equals(IdEncoding obj) {
        return Objects.equals(value, obj.value);
    }

    @Encoding.Builder
    static class Builder {

        private Id id = null;

        @Encoding.Init
        public void setStringValue(String value) {
            this.id = Id.of(value);
        }

        @Encoding.Init
        public void setLongValue(long value) {
            this.id = Id.of(value);
        }

        @Encoding.Copy
        public void copyId(Id value) {
            this.id = value;
        }

        @Encoding.Build
        Id build() {
            return this.id;
        }
    }
}
