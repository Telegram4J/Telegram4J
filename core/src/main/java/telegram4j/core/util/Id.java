package telegram4j.core.util;

import reactor.util.annotation.Nullable;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Channel;
import telegram4j.core.object.chat.GroupChat;
import telegram4j.core.object.chat.PrivateChat;
import telegram4j.tl.*;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * The unsigned 64-bot identifier for {@link PeerEntity} objects which
 * can contains access hash or min information about channel/user.
 */
public final class Id implements Comparable<Id> {

    private final Type type;
    private final long value;
    @Nullable
    private final Object context; // Long/MinInformation

    private Id(Type type, long value, @Nullable Object context) {
        this.type = Objects.requireNonNull(type);
        this.value = value;
        this.context = context;
    }

    /**
     * Create new id with {@link Type#CHAT} type and zero access hash.
     *
     * @param value The id of chat.
     * @return New {@link Id} of chat.
     */
    public static Id ofChat(long value) {
        return new Id(Type.CHAT, value, null);
    }

    /**
     * Create new id with {@link Type#CHANNEL} type and given access hash.
     *
     * @param value The id of channel.
     * @param accessHash The access hash of channel.
     * @return New {@link Id} of channel.
     */
    public static Id ofChannel(long value, @Nullable Long accessHash) {
        return new Id(Type.CHANNEL, value, accessHash);
    }

    /**
     * Create new id with {@link Type#CHANNEL} type and min information.
     *
     * @param value The id of channel.
     * @param peerId The id of chat/channel where have seen this channel.
     * @param messageId The id of message where have seen this channel.
     * @see <a href="https://core.telegram.org/api/min">Min Constructors</a>
     * @return New {@link Id} of min channel.
     */
    public static Id ofChannel(long value, Id peerId, int messageId) {
        return new Id(Type.CHANNEL, value, new MinInformation(peerId, messageId));
    }

    /**
     * Create new id with {@link Type#USER} type and given access hash.
     *
     * @param value The id of user.
     * @param accessHash The access hash of user.
     * @return New {@link Id} of user.
     */
    public static Id ofUser(long value, @Nullable Long accessHash) {
        return new Id(Type.USER, value, accessHash);
    }

    /**
     * Create new id with {@link Type#USER} type and min information.
     *
     * @param value The id of user.
     * @param peerId The id of chat/channel where have seen this user.
     * @param messageId The id of message where have seen this user.
     * @see <a href="https://core.telegram.org/api/min">Min Constructors</a>
     * @return New {@link Id} of min user.
     */
    public static Id ofUser(long value, Id peerId, int messageId) {
        return new Id(Type.USER, value, new MinInformation(peerId, messageId));
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
            case PeerChannel.ID: return new Id(Type.CHANNEL, ((PeerChannel) peer).channelId(), null);
            case PeerChat.ID: return new Id(Type.CHAT, ((PeerChat) peer).chatId(), null);
            case PeerUser.ID: return new Id(Type.USER, ((PeerUser) peer).userId(), null);
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

                return ofUser(d.userId(), of(d.peer(), selfId), d.msgId());
            }
            case InputUserSelf.ID: return selfId;
            default: throw new IllegalArgumentException("Unknown input user type: " + inputUser);
        }
    }

    /**
     * Create new id from {@link InputChannel} object.
     * If {@code inputChannel} parameter is {@link InputChannelFromMessage} id will be without access hash and min information.
     *
     * @throws IllegalArgumentException If specified input user identifier is unknown.
     * @param inputChannel The {@link InputChannel} identifier.
     * @param selfId The id of <i>current</i> user, used for {@link InputChannelFromMessage} handling.
     * @return New {@link Id} from given {@link InputChannel}.
     */
    public static Id of(InputChannel inputChannel, Id selfId) {
        Objects.requireNonNull(selfId);
        switch (inputChannel.identifier()) {
            case BaseInputChannel.ID: {
                BaseInputChannel d = (BaseInputChannel) inputChannel;

                return ofChannel(d.channelId(), d.accessHash());
            }
            case InputChannelFromMessage.ID: {
                InputChannelFromMessage d = (InputChannelFromMessage) inputChannel;

                return ofChannel(d.channelId(), of(d.peer(), selfId), d.msgId());
            }
            default: throw new IllegalArgumentException("Unknown input channel type: " + inputChannel);
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
        Objects.requireNonNull(selfId);
        switch (inputPeer.identifier()) {
            case InputPeerChannel.ID: {
                InputPeerChannel d = (InputPeerChannel) inputPeer;

                return ofChannel(d.channelId(), d.accessHash());
            }
            case InputPeerChannelFromMessage.ID: {
                InputPeerChannelFromMessage d = (InputPeerChannelFromMessage) inputPeer;

                return ofChannel(d.channelId(), of(d.peer(), selfId), d.msgId());
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

                return ofUser(d.userId(), of(d.peer(), selfId), d.msgId());
            }
            default: throw new IllegalArgumentException("Unknown input peer type: " + inputPeer);
        }
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
     * Gets a string representation of {@link #asLong()} method.
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
     * Gets the access hash of this id, if present and {@link #getType() type} is not {@link Type#CHAT}.
     *
     * @return The access hash of this id, if present and applicable.
     */
    public OptionalLong getAccessHash() {
        if (context instanceof Long) {
            return OptionalLong.of((long) context);
        }
        return OptionalLong.empty();
    }

    /**
     * Gets the min information for id, if present and applicable.
     *
     * @return The {@link MinInformation} for id, if present and applicable.
     */
    public Optional<MinInformation> getMinInformation() {
        if (context instanceof MinInformation) {
            return Optional.of((MinInformation) context);
        }
        return Optional.empty();
    }

    /**
     * Gets whether {@link #getMinInformation()} or {@link #getAccessHash()} is present.
     *
     * @return {@code true} if {@link #getMinInformation()} or {@link #getAccessHash()} is present.
     */
    public boolean isWithAccessInfo() {
        return context != null;
    }

    /**
     * Compares this id with the specified id.
     * <p>
     * The comparison is based on the {@link #getType()} and after {@link #asLong()}.
     *
     * @param o The other id to be compared.
     * @return The comparator value, negative if less, positive if greater.
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
        return type.hashCode() ^ Long.hashCode(value);
    }

    @Override
    public String toString() {
        String prefix;
        switch (type) {
            case USER:
                prefix = "User";
                break;
            case CHAT:
                prefix = "Chat";
                break;
            case CHANNEL:
                prefix = "Channel";
                break;
            default: throw new IllegalStateException();
        }
        return prefix + "Id{" + value + '}';
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

    /**
     * Context for accessing to user/channel which haven't access hash.
     * <b>Available to use only from users accounts.</b>
     *
     * @see <a href="https://core.telegram.org/api/min">Min Constructors</a>
     */
    public static final class MinInformation {
        private final Id peerId;
        private final int messageId;

        private MinInformation(Id peerId, int messageId) {
            this.peerId = Objects.requireNonNull(peerId);
            this.messageId = messageId;
        }

        /**
         * Gets id of peer where this user/channel was seen.
         *
         * @return The id of peer where this user/channel was seen.
         */
        public Id getPeerId() {
            return peerId;
        }

        /**
         * Gets id of message where this user/channel was seen.
         *
         * @return The id of message where this user/channel was seen.
         */
        public int getMessageId() {
            return messageId;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MinInformation that = (MinInformation) o;
            return messageId == that.messageId && peerId.equals(that.peerId);
        }

        @Override
        public int hashCode() {
            return peerId.hashCode() + 51 * messageId;
        }

        @Override
        public String toString() {
            return "MinInformation{" +
                    "peerId=" + peerId +
                    ", messageId=" + messageId +
                    '}';
        }
    }
}
