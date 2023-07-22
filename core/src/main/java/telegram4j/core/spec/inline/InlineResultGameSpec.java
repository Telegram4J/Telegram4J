package telegram4j.core.spec.inline;

import reactor.util.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class InlineResultGameSpec implements InlineResultSpec {
    private final String shortName;
    private final String id;
    private final InlineMessageSpec message;

    private InlineResultGameSpec(String shortName, String id, InlineMessageSpec message) {
        this.shortName = shortName;
        this.id = id;
        this.message = message;
    }

    public String shortName() {
        return shortName;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public InlineMessageSpec message() {
        return message;
    }

    public InlineResultGameSpec withShortName(String value) {
        Objects.requireNonNull(value);
        if (this.shortName.equals(value)) return this;
        return new InlineResultGameSpec(value, id, message);
    }

    public InlineResultGameSpec withId(String value) {
        Objects.requireNonNull(value);
        if (this.id.equals(value)) return this;
        return new InlineResultGameSpec(shortName, value, message);
    }

    public InlineResultGameSpec withMessage(InlineMessageSpec value) {
        Objects.requireNonNull(value);
        if (this.message == value) return this;
        return new InlineResultGameSpec(shortName, id, value);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof InlineResultGameSpec that)) return false;
        return shortName.equals(that.shortName) && id.equals(that.id) && message.equals(that.message);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + shortName.hashCode();
        h += (h << 5) + id.hashCode();
        h += (h << 5) + message.hashCode();
        return h;
    }

    @Override
    public String toString() {
        return "InlineResultGameSpec{" +
                "shortName='" + shortName + '\'' +
                ", id='" + id + '\'' +
                ", message=" + message +
                '}';
    }

    public static InlineResultGameSpec of(String shortName, String id, InlineMessageSpec message) {
        Objects.requireNonNull(shortName);
        Objects.requireNonNull(id);
        Objects.requireNonNull(message);
        return new InlineResultGameSpec(shortName, id, message);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private static final byte INIT_BIT_SHORT_NAME = 0x1;
        private static final byte INIT_BIT_ID = 0x2;
        private static final byte INIT_BIT_MESSAGE = 0x4;
        private byte initBits = 0x7;

        private String shortName;
        private String id;
        private InlineMessageSpec message;

        private Builder() {
        }

        public Builder from(InlineResultGameSpec instance) {
            return from((Object) instance);
        }

        public Builder from(InlineResultSpec instance) {
            from((Object) instance);
            return this;
        }

        private Builder from(Object object) {
            Objects.requireNonNull(object);
            if (object instanceof InlineResultGameSpec instance) {
                shortName(instance.shortName);
                message(instance.message);
                id(instance.id);
            } else if (object instanceof InlineResultSpec instance) {
                message(instance.message());
                id(instance.id());
            }
            return this;
        }

        public Builder shortName(String shortName) {
            this.shortName = Objects.requireNonNull(shortName);
            initBits &= ~INIT_BIT_SHORT_NAME;
            return this;
        }

        public Builder id(String id) {
            this.id = Objects.requireNonNull(id);
            initBits &= ~INIT_BIT_ID;
            return this;
        }

        public Builder message(InlineMessageSpec message) {
            this.message = Objects.requireNonNull(message);
            initBits &= ~INIT_BIT_MESSAGE;
            return this;
        }

        public InlineResultGameSpec build() {
            if (initBits != 0) {
                throw incompleteInitialization();
            }
            return new InlineResultGameSpec(shortName, id, message);
        }

        private IllegalStateException incompleteInitialization() {
            List<String> attributes = new ArrayList<>(Integer.bitCount(initBits));
            if ((initBits & INIT_BIT_SHORT_NAME) != 0) attributes.add("shortName");
            if ((initBits & INIT_BIT_ID) != 0) attributes.add("id");
            if ((initBits & INIT_BIT_MESSAGE) != 0) attributes.add("message");
            return new IllegalStateException("Cannot build InlineResultGameSpec, some of required attributes are not set " + attributes);
        }
    }
}
