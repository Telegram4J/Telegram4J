package telegram4j.core.spec;

import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class AnswerCallbackQuerySpec {
    private final boolean alert;
    private final String message;
    private final String url;
    private final Duration cacheTime;

    private AnswerCallbackQuerySpec(Duration cacheTime) {
        this.cacheTime = Objects.requireNonNull(cacheTime);
        this.message = null;
        this.url = null;
        this.alert = false;
    }

    private AnswerCallbackQuerySpec(Builder builder) {
        this.message = builder.message;
        this.url = builder.url;
        this.cacheTime = builder.cacheTime;
        this.alert = builder.alert;
    }

    private AnswerCallbackQuerySpec(boolean alert, @Nullable String message, @Nullable String url, Duration cacheTime) {
        this.alert = alert;
        this.message = message;
        this.url = url;
        this.cacheTime = cacheTime;
    }

    public boolean alert() {
        return alert;
    }

    public Optional<String> message() {
        return Optional.ofNullable(message);
    }

    public Optional<String> url() {
        return Optional.ofNullable(url);
    }

    public Duration cacheTime() {
        return cacheTime;
    }

    public AnswerCallbackQuerySpec withAlert(boolean value) {
        if (alert == value) return this;
        return new AnswerCallbackQuerySpec(value, message, url, cacheTime);
    }

    public AnswerCallbackQuerySpec withMessage(@Nullable String value) {
        if (Objects.equals(message, value)) return this;
        return new AnswerCallbackQuerySpec(alert, value, url, cacheTime);
    }

    public AnswerCallbackQuerySpec withMessage(Optional<String> opt) {
        return withMessage(opt.orElse(null));
    }

    public AnswerCallbackQuerySpec withUrl(@Nullable String value) {
        if (Objects.equals(url, value)) return this;
        return new AnswerCallbackQuerySpec(alert, message, value, cacheTime);
    }

    public AnswerCallbackQuerySpec withUrl(Optional<String> opt) {
        return withUrl(opt.orElse(null));
    }

    public AnswerCallbackQuerySpec withCacheTime(Duration value) {
        Objects.requireNonNull(value);
        if (cacheTime.equals(value)) return this;
        return new AnswerCallbackQuerySpec(alert, message, url, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnswerCallbackQuerySpec)) return false;
        AnswerCallbackQuerySpec that = (AnswerCallbackQuerySpec) o;
        return alert == that.alert
                && Objects.equals(message, that.message)
                && Objects.equals(url, that.url)
                && cacheTime.equals(that.cacheTime);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Boolean.hashCode(alert);
        h += (h << 5) + Objects.hashCode(message);
        h += (h << 5) + Objects.hashCode(url);
        h += (h << 5) + cacheTime.hashCode();
        return h;
    }

    @Override
    public String toString() {
        return "AnswerCallbackQuerySpec{" +
                "alert=" + alert +
                ", message='" + message + '\'' +
                ", url='" + url + '\'' +
                ", cacheTime=" + cacheTime +
                '}';
    }

    public static AnswerCallbackQuerySpec of(Duration cacheTime) {
        return new AnswerCallbackQuerySpec(cacheTime);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private static final byte INIT_BIT_CACHE_TIME = 0x1;
        private byte initBits = 0x1;

        private boolean alert;
        private String message;
        private String url;
        private Duration cacheTime;

        private Builder() {
        }

        public Builder from(AnswerCallbackQuerySpec instance) {
            Objects.requireNonNull(instance);
            alert(instance.alert);
            message(instance.message);
            url(instance.url);
            cacheTime(instance.cacheTime);
            return this;
        }

        public Builder alert(boolean alert) {
            this.alert = alert;
            return this;
        }

        public Builder message(@Nullable String message) {
            this.message = message;
            return this;
        }

        public Builder message(Optional<String> message) {
            this.message = message.orElse(null);
            return this;
        }

        public Builder url(@Nullable String url) {
            this.url = url;
            return this;
        }

        public Builder url(Optional<String> url) {
            this.url = url.orElse(null);
            return this;
        }

        public Builder cacheTime(Duration cacheTime) {
            this.cacheTime = Objects.requireNonNull(cacheTime);
            initBits &= ~INIT_BIT_CACHE_TIME;
            return this;
        }

        public AnswerCallbackQuerySpec build() {
            if (initBits != 0) {
                throw incompleteInitialization();
            }
            return new AnswerCallbackQuerySpec(this);
        }

        private IllegalStateException incompleteInitialization() {
            List<String> attributes = new ArrayList<>();
            if ((initBits & INIT_BIT_CACHE_TIME) != 0) attributes.add("cacheTime");
            return new IllegalStateException("Cannot build AnswerCallbackQuerySpec, some of required attributes are not set " + attributes);
        }
    }
}
