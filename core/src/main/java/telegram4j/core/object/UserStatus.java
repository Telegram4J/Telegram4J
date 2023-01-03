package telegram4j.core.object;

import reactor.util.annotation.Nullable;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Simplified version of the {@link telegram4j.tl.UserStatus user status}. */
public class UserStatus {

    private final Type type;
    @Nullable
    private final Instant expiresTimestamp;
    @Nullable
    private final Instant wasOnlineTimestamp;

    public UserStatus(Type type) {
        this(type, null, null);
    }

    public UserStatus(Type type, @Nullable Instant expiresTimestamp, @Nullable Instant wasOnlineTimestamp) {
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
    public String toString() {
        return "UserStatus{" +
                "type=" + type +
                ", expiresTimestamp=" + expiresTimestamp +
                ", wasOnlineTimestamp=" + wasOnlineTimestamp +
                '}';
    }

    /** Available types of user status. */
    public enum Type {

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
