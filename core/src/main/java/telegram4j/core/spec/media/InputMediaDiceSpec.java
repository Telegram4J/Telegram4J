package telegram4j.core.spec.media;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.ImmutableInputMediaDice;
import telegram4j.tl.InputMedia;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class InputMediaDiceSpec implements InputMediaSpec {
    private final String emoticon;

    private InputMediaDiceSpec(String emoticon) {
        this.emoticon = emoticon;
    }

    public String emoticon() {
        return emoticon;
    }

    @Override
    public Mono<ImmutableInputMediaDice> resolve(MTProtoTelegramClient client) {
        return Mono.just(ImmutableInputMediaDice.of(emoticon()));
    }

    public InputMediaDiceSpec withEmoticon(String value) {
        Objects.requireNonNull(value);
        if (this.emoticon.equals(value)) return this;
        return new InputMediaDiceSpec(value);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InputMediaDiceSpec that = (InputMediaDiceSpec) o;
        return emoticon.equals(that.emoticon);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + emoticon.hashCode();
        return h;
    }

    @Override
    public String toString() {
        return "InputMediaDiceSpec{" +
                "emoticon='" + emoticon + '\'' +
                '}';
    }

    public static InputMediaDiceSpec of(String emoticon) {
        Objects.requireNonNull(emoticon);
        return new InputMediaDiceSpec(emoticon);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private static final byte INIT_BIT_EMOTICON = 0x1;
        private byte initBits = 0x1;

        private String emoticon;

        private Builder() {
        }

        public Builder from(InputMediaDiceSpec instance) {
            Objects.requireNonNull(instance);
            emoticon(instance.emoticon);
            return this;
        }

        public Builder emoticon(String emoticon) {
            this.emoticon = Objects.requireNonNull(emoticon);
            initBits &= ~INIT_BIT_EMOTICON;
            return this;
        }

        public InputMediaDiceSpec build() {
            if (initBits != 0) {
                throw incompleteInitialization();
            }
            return new InputMediaDiceSpec(emoticon);
        }

        private IllegalStateException incompleteInitialization() {
            List<String> attributes = new ArrayList<>();
            if ((initBits & INIT_BIT_EMOTICON) != 0) attributes.add("emoticon");
            return new IllegalStateException("Cannot build InputMediaDiceSpec, some of required attributes are not set " + attributes);
        }
    }
}
