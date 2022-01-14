package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.tl.Peer;
import telegram4j.tl.PeerChannel;
import telegram4j.tl.PeerChat;
import telegram4j.tl.PeerUser;

import java.util.Objects;
import java.util.OptionalLong;

public final class Id {
    // TODO: create access hash resolving
    public static final long ACCESS_HASH_NOOP = 0;
    public static final long ACCESS_HASH_UNAVAILABLE = -1;
    public static final long ACCESS_HASH_UNRESOLVED = -2;

    private final Type type;
    private final long value;
    private final long accessHash;

    private Id(Type type, long value, long accessHash) {
        this.type = Objects.requireNonNull(type, "type");
        this.value = value;
        this.accessHash = accessHash;
    }

    public static Id ofChat(long value) {
        return new Id(Type.CHAT, value, ACCESS_HASH_NOOP);
    }

    public static Id ofChannel(long value, @Nullable Long accessHash) {
        return of(Type.CHANNEL, value, accessHash);
    }

    public static Id ofUser(long value, @Nullable Long accessHash) {
        return of(Type.USER, value, accessHash);
    }

    public static Id of(Peer peer) {
        switch (peer.identifier()) {
            case PeerChannel.ID: return of(Type.CHANNEL, ((PeerChannel) peer).channelId(), ACCESS_HASH_UNRESOLVED);
            case PeerChat.ID: return of(Type.CHAT, ((PeerChat) peer).chatId(), ACCESS_HASH_UNRESOLVED);
            case PeerUser.ID: return of(Type.USER, ((PeerUser) peer).userId(), ACCESS_HASH_UNRESOLVED);
            default: throw new IllegalArgumentException("Unknown peer type: " + peer);
        }
    }

    public static Id of(Type type, long value) {
        return new Id(type, value, ACCESS_HASH_UNAVAILABLE);
    }

    public static Id of(Type type, String value) {
        return new Id(type, Long.parseLong(value), ACCESS_HASH_UNAVAILABLE);
    }

    public static Id of(Type type, long value, @Nullable Long accessHash) {
        return new Id(type, value, accessHash != null ? accessHash : ACCESS_HASH_UNAVAILABLE);
    }

    public long asLong() {
        return value;
    }

    public String asString() {
        return Long.toString(value);
    }

    public Type getType() {
        return type;
    }

    public OptionalLong getAccessHash() {
        if (accessHash != ACCESS_HASH_UNAVAILABLE &&
                accessHash != ACCESS_HASH_UNRESOLVED &&
                accessHash != ACCESS_HASH_NOOP) {
            return OptionalLong.of(accessHash);
        }
        return OptionalLong.empty();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Id id = (Id) o;
        return value == id.value && accessHash == id.accessHash && type == id.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value, accessHash);
    }

    @Override
    public String toString() {
        return "Id{" + value + '}';
    }

    public enum Type {
        CHAT,
        CHANNEL,
        USER;
    }
}
