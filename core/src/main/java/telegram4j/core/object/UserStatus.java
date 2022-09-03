package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Simplified version of the {@link telegram4j.tl.UserStatus user status}. */
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
        this.client = Objects.requireNonNull(client);
        this.type = Objects.requireNonNull(type);
        this.expiresTimestamp = expiresTimestamp;
        this.wasOnlineTimestamp = wasOnlineTimestamp;
    }

    /**
     * Gets the type of status.
     *
     * @return The {@link Type} of status.
     */
    public Type getType() {
        return type;
    }

    /**
     * Gets the timestamp when this status will be expired, if {@link #getType()} is {@link Type#ONLINE}.
     *
     * @return The timestamp when this status will be expired, if {@link #getType()} is {@link Type#ONLINE}.
     */
    public Optional<Instant> getExpiresTimestamp() {
        return Optional.ofNullable(expiresTimestamp);
    }

    /**
     * Gets the timestamp of the last user online status, if {@link #getType()} is {@link Type#OFFLINE}.
     *
     * @return The timestamp of the last user online status, if {@link #getType()} is {@link Type#OFFLINE}.
     */
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

    /** Available types of user status. */
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
        LAST_MONTH
    }
}
