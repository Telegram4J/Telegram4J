package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class UserStatus implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final Type type;
    @Nullable
    private final Instant expiresTimestamp;
    @Nullable
    private final Instant wasOnlineTimestamp;

    public UserStatus(MTProtoTelegramClient client, Type type) {
        this(client, type, null, null);
    }

    public UserStatus(MTProtoTelegramClient client, Type type,
                      @Nullable Instant expiresTimestamp,
                      @Nullable Instant wasOnlineTimestamp) {
        this.client = Objects.requireNonNull(client, "client");
        this.type = Objects.requireNonNull(type, "type");
        this.expiresTimestamp = expiresTimestamp;
        this.wasOnlineTimestamp = wasOnlineTimestamp;
    }

    public Type getType() {
        return type;
    }

    public Optional<Instant> getExpiresTimestamp() {
        return Optional.ofNullable(expiresTimestamp);
    }

    public Optional<Instant> getWasOnlineTimestamp() {
        return Optional.ofNullable(wasOnlineTimestamp);
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
        return type == that.type && Objects.equals(expiresTimestamp, that.expiresTimestamp) &&
                Objects.equals(wasOnlineTimestamp, that.wasOnlineTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, expiresTimestamp, wasOnlineTimestamp);
    }

    @Override
    public String toString() {
        return "UserStatus{" +
                "type=" + type +
                ", expiresTimestamp=" + expiresTimestamp +
                ", wasOnlineTimestamp=" + wasOnlineTimestamp +
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
