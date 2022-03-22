package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.object.chat.Channel;
import telegram4j.core.object.chat.GroupChat;
import telegram4j.core.object.chat.PrivateChat;
import telegram4j.tl.*;

import java.util.Objects;
import java.util.OptionalLong;

/** The {@link PeerEntity} identifier with optional access hash. */
public final class Id implements Comparable<Id> {

    /** Number alias for empty user/channel's access hash.  */
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
     * @throws IllegalArgumentException If specified peer identifier is unknown.
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

    /**
     * Create new id from {@link InputUser} object.
     * If {@code inputUser} parameter is {@link InputUserFromMessage} id will be without access hash and min information.
     *
     * @throws IllegalArgumentException If specified input user identifier is unknown.
     * @param inputUser The {@link InputUser} identifier.
     * @param selfId The id of <i>current</i> user, used for {@link InputUserSelf} mapping.
     * @return New {@link Id} from given {@link InputUser}.
     */
    public static Id of(InputUser inputUser, Id selfId) {
        switch (inputUser.identifier()) {
            case BaseInputUser.ID: {
                BaseInputUser d = (BaseInputUser) inputUser;

                return ofUser(d.userId(), d.accessHash());
            }
            case InputUserFromMessage.ID: {
                InputUserFromMessage d = (InputUserFromMessage) inputUser;

                return ofUser(d.userId(), null);
            }
            case InputUserSelf.ID: return selfId;
            default: throw new IllegalArgumentException("Unknown input user type: " + inputUser);
        }
    }

    /**
     * Create new id from {@link InputPeer} object.
     * If {@code inputPeer} parameter is {@link InputPeerChannelFromMessage}/{@link InputPeerUserFromMessage}
     * id will be without access hash and min information.
     *
     * @throws IllegalArgumentException If specified input peer identifier is unknown.
     * @param inputPeer The {@link InputPeer} identifier.
     * @param selfId The id of <i>current</i> user, used for {@link InputPeerSelf} mapping.
     * @return New {@link Id} from given {@link InputPeer}.
     */
    public static Id of(InputPeer inputPeer, Id selfId) {
        switch (inputPeer.identifier()) {
            case InputPeerChannel.ID: {
                InputPeerChannel d = (InputPeerChannel) inputPeer;

                return ofChannel(d.channelId(), d.accessHash());
            }
            case InputPeerChannelFromMessage.ID: {
                InputPeerChannelFromMessage d = (InputPeerChannelFromMessage) inputPeer;

                return ofChannel(d.channelId(), null);
            }
            case InputPeerChat.ID: {
                InputPeerChat d = (InputPeerChat) inputPeer;

                return ofChat(d.chatId());
            }
            case InputPeerSelf.ID: return selfId;
            case InputPeerUser.ID: {
                InputPeerUser d = (InputPeerUser) inputPeer;

                return ofUser(d.userId(), d.accessHash());
            }
            case InputPeerUserFromMessage.ID: {
                InputPeerUserFromMessage d = (InputPeerUserFromMessage) inputPeer;

                return ofUser(d.userId(), null);
            }
            default: throw new IllegalArgumentException("Unknown input peer type: " + inputPeer);
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

    /**
     * Compares this id with the specified id.
     * <p>
     * The comparison is based on the {@link #getType()} and after {@link #asLong()}.
     *
     * @param o The other id to be compared.
     * @return The comparator value, negative if less, positive if greater
     */
    @Override
    public int compareTo(Id o) {
        int typeComp = type.compareTo(o.type);
        if (typeComp != 0) {
            return typeComp;
        }

        return Long.compare(value, o.value);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Id id = (Id) o;
        return type == id.type && value == id.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }

    @Override
    public String toString() {
        return "Id{" + value + '}';
    }

    /** Available types of entities ids. */
    public enum Type {
        /** Represents id for {@link PrivateChat}/{@link User} entity. */
        USER,

        /** Represents id for {@link GroupChat} entity. */
        CHAT,

        /** Represents id for {@link Channel} entity. */
        CHANNEL,
    }
}
