package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.Peer;
import telegram4j.tl.PeerChannel;
import telegram4j.tl.PeerChat;
import telegram4j.tl.PeerUser;

import java.util.Objects;
import java.util.OptionalLong;

public final class Id {
    public static final long ZERO_SECRET_CHAT_ID = -2000000000000L;
    public static final long ZERO_CHANNEL_ID = -1000000000000L;

    private static final long MAX_USER_ID = (1L << 40) - 1;
    private static final long MAX_CHANNEL_ID = 1000000000000L - (1L << 31);
    private static final long MAX_CHAT_ID = 999999999999L;

    private final long value;
    private final long accessHash;

    private Id(long value, long accessHash) {
        this.value = value;
        this.accessHash = accessHash;
    }

    public static Id of(Peer peer) {
        return new Id(TlEntityUtil.getPeerId(peer), -1);
    }

    public static Id of(long value, @Nullable Long accessHash) {
        return new Id(value, accessHash != null ? accessHash : -1);
    }

    public static Id of(long value) {
        return new Id(value, -1);
    }

    public static Id of(String value) {
        return new Id(Long.parseLong(value), -1);
    }

    public long asLong() {
        return value;
    }

    public long asLongRaw() {
        switch (getType()) {
            case CHAT: return -value;
            case SECRET_CHAT: return value + ZERO_SECRET_CHAT_ID;
            case CHANNEL: return -(value - ZERO_CHANNEL_ID);
            case USER: return value;
            default: throw new IllegalStateException();
        }
    }

    public String asString() {
        return Long.toString(value);
    }

    public Type getType() {
        return Type.of(value);
    }

    public OptionalLong getAccessHash() {
        return accessHash != -1 ? OptionalLong.of(accessHash) : OptionalLong.empty();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Id id = (Id) o;
        return value == id.value && accessHash == id.accessHash;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, accessHash);
    }

    @Override
    public String toString() {
        return "Id{" + value + '}';
    }

    public enum Type {
        CHANNEL,
        CHAT,
        SECRET_CHAT,
        USER;

        public static Type of(long id) {
            if (id < 0) {
                if (-MAX_CHAT_ID <= id) {
                    return CHAT;
                }
                if (ZERO_CHANNEL_ID - MAX_CHANNEL_ID <= id && id != ZERO_CHANNEL_ID) {
                    return CHANNEL;
                }
                if (ZERO_SECRET_CHAT_ID + Integer.MIN_VALUE <= id && id != ZERO_SECRET_CHAT_ID) {
                    return SECRET_CHAT;
                }
            }

            if (0 < id && id <= MAX_USER_ID) {
                return USER;
            }
            throw new IllegalArgumentException("Failed to detect type for id: " + id);
        }
    }
}
