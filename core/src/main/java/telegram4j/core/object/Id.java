package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.tl.Peer;
import telegram4j.tl.PeerChannel;
import telegram4j.tl.PeerChat;
import telegram4j.tl.PeerUser;

import java.util.Objects;
import java.util.OptionalLong;

/** The {@link PeerEntity} identifier with optional access hash. */
public final class Id {
    private static final long ACCESS_HASH_UNAVAILABLE = 0;

    private final Type type;
    private final long value;
    private final long accessHash;

    private Id(Type type, long value, long accessHash) {
        this.type = Objects.requireNonNull(type, "type");
        this.value = value;
        this.accessHash = accessHash;
    }

    /**
     * Create new id with {@link Type#CHAT} type and zero access hash.
     *
     * @param value The id of chat.
     * @return New {@link Id} of chat.
     */
    public static Id ofChat(long value) {
        return new Id(Type.CHAT, value, ACCESS_HASH_UNAVAILABLE);
    }

    /**
     * Create new id with {@link Type#CHANNEL} type and given access hash.
     *
     * @param value The id of channel.
     * @param accessHash The access hash of channel.
     * @return New {@link Id} of channel.
     */
    public static Id ofChannel(long value, @Nullable Long accessHash) {
        return of(Type.CHANNEL, value, accessHash);
    }

    /**
     * Create new id with {@link Type#USER} type and given access hash.
     *
     * @param value The id of user.
     * @param accessHash The access hash of user.
     * @return New {@link Id} of user.
     */
    public static Id ofUser(long value, @Nullable Long accessHash) {
        return of(Type.USER, value, accessHash);
    }

    /**
     * Create new id from {@link Peer} object with zero access hash.
     *
     * @param peer The {@link Peer} identifier.
     * @return New {@link Id} from given {@link Peer}.
     */
    public static Id of(Peer peer) {
        switch (peer.identifier()) {
            case PeerChannel.ID: return new Id(Type.CHANNEL, ((PeerChannel) peer).channelId(), ACCESS_HASH_UNAVAILABLE);
            case PeerChat.ID: return new Id(Type.CHAT, ((PeerChat) peer).chatId(), ACCESS_HASH_UNAVAILABLE);
            case PeerUser.ID: return new Id(Type.USER, ((PeerUser) peer).userId(), ACCESS_HASH_UNAVAILABLE);
            default: throw new IllegalArgumentException("Unknown peer type: " + peer);
        }
    }

    private static Id of(Type type, long value, @Nullable Long accessHash) {
        return new Id(type, value, accessHash != null ? accessHash : ACCESS_HASH_UNAVAILABLE);
    }

    /**
     * Gets a raw value of id.
     *
     * @return The raw value of id.
     */
    public long asLong() {
        return value;
    }

    /**
     * Gets a string representation of {@link #asLong} method.
     *
     * @return The string representation of raw id.
     */
    public String asString() {
        return Long.toString(value);
    }

    /**
     * Gets the {@link Type} of id.
     *
     * @return The {@link Type} of id.
     */
    public Type getType() {
        return type;
    }

    /**
     * Gets the access hash of this id, if present and applicable.
     *
     * @return The access hash of this id, if present and applicable.
     */
    public OptionalLong getAccessHash() {
        if (accessHash != ACCESS_HASH_UNAVAILABLE) {
            return OptionalLong.of(accessHash);
        }
        return OptionalLong.empty();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Id id = (Id) o;
        return value == id.value && type == id.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, type);
    }

    @Override
    public String toString() {
        return "Id{" + value + '}';
    }

    public enum Type {
        CHAT,
        CHANNEL,
        USER
    }
}
