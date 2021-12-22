package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;
import java.util.Optional;

public class UserStatus implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final Type type;
    @Nullable
    private final Integer expires;
    @Nullable
    private final Integer wasOnline;

    public UserStatus(MTProtoTelegramClient client, Type type) {
        this(client, type, null, null);
    }

    public UserStatus(MTProtoTelegramClient client, Type type,
                      @Nullable Integer expires,
                      @Nullable Integer wasOnline) {
        this.client = Objects.requireNonNull(client, "client");
        this.type = Objects.requireNonNull(type, "type");
        this.expires = expires;
        this.wasOnline = wasOnline;
    }

    public Type getType() {
        return type;
    }

    public Optional<Integer> getExpires() {
        return Optional.ofNullable(expires);
    }

    public Optional<Integer> getWasOnline() {
        return Optional.ofNullable(wasOnline);
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserStatus that = (UserStatus) o;
        return type == that.type && Objects.equals(expires, that.expires) && Objects.equals(wasOnline, that.wasOnline);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, expires, wasOnline);
    }

    @Override
    public String toString() {
        return "UserStatus{" +
                "type=" + type +
                ", expires=" + expires +
                ", wasOnline=" + wasOnline +
                '}';
    }

    public enum Type {

        /** User status has not been set yet. */
        EMPTY,

        /** Online status of the user. */
        ONLINE,

        /** The user's offline status. */
        OFFLINE,

        /** Online status: <i>last seen recently</i>. */
        RECENTLY,

        /** Online status: <i>last seen last week</i>. */
        LAST_WEEK,

        /** Online status: <i>last seen last month</i>. */
        LAST_MONTH;
    }
}
