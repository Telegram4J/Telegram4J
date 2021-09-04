package telegram4j.json.encoding;

import org.immutables.encode.Encoding;
import telegram4j.json.api.Id;

import java.util.Objects;
import java.util.Optional;

@Encoding
public class OptionalIdEncoding {

    @Encoding.Impl(virtual = true)
    private Optional<Id> optional = Optional.empty();

    private final long value = optional.map(Id::asLong).orElse(0L);
    private final boolean present = optional.isPresent();

    @Encoding.Expose
    Optional<Id> get() {
        return present ? Optional.of(Id.of(value)) : Optional.empty();
    }

    @Encoding.Copy
    public Optional<Id> withOptional(Optional<Id> value) {
        return Objects.requireNonNull(value);
    }

    @Encoding.Copy
    public Optional<Id> withId(Id value) {
        return Optional.of(value);
    }

    @Encoding.Copy
    public Optional<Id> withString(String value) {
        return Optional.of(Id.of(value));
    }

    @Encoding.Copy
    public Optional<Id> withLong(long value) {
        return Optional.of(Id.of(value));
    }

    @Encoding.Naming("is*Present")
    boolean isPresent() {
        return present;
    }

    @Encoding.Naming("*OrElse")
    long orElse(long defaultValue) {
        return present ? value : defaultValue;
    }

    @Encoding.Builder
    static class Builder {

        private Optional<Id> optional = Optional.empty();

        @Encoding.Init
        public void setStringValue(String value) {
            this.optional = Optional.of(Id.of(value));
        }

        @Encoding.Init
        public void setLongValue(long value) {
            this.optional = Optional.of(Id.of(value));
        }

        @Encoding.Init
        public void setIdValue(Id value) {
            this.optional = Optional.of(value);
        }

        @Encoding.Init
        @Encoding.Copy
        public void copyOptionalId(Optional<Id> value) {
            this.optional = value;
        }

        @Encoding.Build
        Optional<Id> build() {
            return this.optional;
        }
    }
}
