package telegram4j.core.spec.markup;

import reactor.util.annotation.Nullable;
import telegram4j.tl.RequestPeerType;
import telegram4j.tl.RequestPeerTypeUser;

import java.util.Objects;
import java.util.Optional;

public final class RequestUserSpec implements RequestPeerSpec {
    // TODO: make singleton?

    @Nullable
    private final Boolean isBot;
    @Nullable
    private final Boolean isPremium;

    RequestUserSpec(@Nullable Boolean isBot, @Nullable Boolean isPremium) {
        this.isBot = isBot;
        this.isPremium = isPremium;
    }

    public Optional<Boolean> isBot() {
        return Optional.ofNullable(isBot);
    }

    public Optional<Boolean> isPremium() {
        return Optional.ofNullable(isPremium);
    }

    public static RequestUserSpec of(@Nullable Boolean isBot, @Nullable Boolean isPremium) {
        return new RequestUserSpec(isBot, isPremium);
    }

    public static RequestUserSpec of() {
        return new RequestUserSpec(null, null);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof RequestUserSpec that)) return false;
        return Objects.equals(isBot, that.isBot) && Objects.equals(isPremium, that.isPremium);
    }

    @Override
    public RequestPeerType asData() {
        return RequestPeerTypeUser.builder()
                .bot(isBot)
                .premium(isPremium)
                .build();
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(isBot);
        h += (h << 5) + Objects.hashCode(isPremium);
        return h;
    }

    @Override
    public String toString() {
        return "RequestUserSpec{" +
                "isBot=" + isBot +
                ", isPremium=" + isPremium +
                '}';
    }

    public static class Builder {
        @Nullable
        private Boolean isBot;
        @Nullable
        private Boolean isPremium;

        private Builder() {}

        public Builder isBot(Optional<Boolean> opt) {
            return isBot(opt.orElse(null));
        }

        public Builder isBot(@Nullable Boolean isBot) {
            this.isBot = isBot;
            return this;
        }

        public Builder isPremium(Optional<Boolean> opt) {
            return isPremium(opt.orElse(null));
        }

        public Builder isPremium(@Nullable Boolean isPremium) {
            this.isPremium = isPremium;
            return this;
        }

        public RequestUserSpec build() {
            return new RequestUserSpec(isBot, isPremium);
        }
    }
}
