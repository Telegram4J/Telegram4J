/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package telegram4j.core.util;

import reactor.util.annotation.Nullable;
import telegram4j.mtproto.internal.Preconditions;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Channel;
import telegram4j.core.object.chat.GroupChat;
import telegram4j.core.object.chat.PrivateChat;
import telegram4j.tl.*;

import java.util.Objects;
import java.util.Optional;

/**
 * The unsigned 64-bit identifier for {@link PeerEntity} objects which
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
     * Create new id with {@link Type#CHANNEL} type without access hash.
     *
     * @param value The id of channel.
     * @return New {@link Id} of channel.
     */
    public static Id ofChannel(long value) {
        return new Id(Type.CHANNEL, value, null);
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
     * @throws IllegalArgumentException if {@code messageId} is negative.
     * @param value The id of channel.
     * @param peerId The id of chat/channel where have seen this channel.
     * @param messageId The id of message where have seen this channel.
     * @see <a href="https://core.telegram.org/api/min">Min Constructors</a>
     * @return New {@link Id} of min channel.
     */
    public static Id ofChannel(long value, Id peerId, int messageId) {
        return ofChannel(value, new MinInformation(peerId, messageId));
    }

    /**
     * Create new id with {@link Type#CHANNEL} type and min information.
     *
     * @param value The id of channel.
     * @param minInformation The min information for this channel.
     * @see <a href="https://core.telegram.org/api/min">Min Constructors</a>
     * @return New {@link Id} of min channel.
     */
    public static Id ofChannel(long value, @Nullable MinInformation minInformation) {
        return new Id(Type.CHANNEL, value, minInformation);
    }

    /**
     * Create new id with {@link Type#USER} type without access hash.
     *
     * @param value The id of user.
     * @return New {@link Id} of user.
     */
    public static Id ofUser(long value) {
        return new Id(Type.USER, value, null);
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
     * @throws IllegalArgumentException if {@code messageId} is negative.
     * @param value The id of user.
     * @param peerId The id of chat/channel where have seen this user.
     * @param messageId The id of message where have seen this user.
     * @see <a href="https://core.telegram.org/api/min">Min Constructors</a>
     * @return New {@link Id} of min user.
     */
    public static Id ofUser(long value, Id peerId, int messageId) {
        return ofUser(value, new MinInformation(peerId, messageId));
    }

    /**
     * Create new id with {@link Type#USER} type and min information.
     *
     * @param value The id of user.
     * @param minInformation The min information for this user.
     * @see <a href="https://core.telegram.org/api/min">Min Constructors</a>
     * @return New {@link Id} of min user.
     */
    public static Id ofUser(long value, @Nullable MinInformation minInformation) {
        return new Id(Type.USER, value, minInformation);
    }

    /**
     * Create new id from {@link Peer} object without access hash.
     *
     * @throws IllegalArgumentException If specified peer identifier is unknown.
     * @param peer The {@link Peer} identifier.
     * @return New {@link Id} from given {@link Peer}.
     */
    public static Id of(Peer peer) {
        return switch (peer.identifier()) {
            case PeerChannel.ID -> new Id(Type.CHANNEL, ((PeerChannel) peer).channelId(), null);
            case PeerChat.ID -> new Id(Type.CHAT, ((PeerChat) peer).chatId(), null);
            case PeerUser.ID -> new Id(Type.USER, ((PeerUser) peer).userId(), null);
            default -> throw new IllegalArgumentException("Unknown peer type: " + peer);
        };
    }

    /**
     * Create new id with specified type and id.
     *
     * @param type The type of id.
     * @param value The value of id.
     * @return New {@link Id} from with specified type and id.
     */
    public static Id of(Type type, long value) {
        return new Id(type, value, null);
    }

    /**
     * Create new id from {@link InputUser} object.
     *
     * @throws IllegalArgumentException If specified input user identifier is unknown.
     * @param inputUser The {@link InputUser} identifier.
     * @param selfId The id of <i>current</i> user, used for {@link InputUserSelf} mapping.
     * @return New {@link Id} from given {@link InputUser}.
     */
    public static Id of(InputUser inputUser, Id selfId) {
        return switch (inputUser.identifier()) {
            case BaseInputUser.ID -> {
                var d = (BaseInputUser) inputUser;

                yield ofUser(d.userId(), d.accessHash());
            }
            case InputUserFromMessage.ID -> {
                var d = (InputUserFromMessage) inputUser;

                yield ofUser(d.userId(), of(d.peer(), selfId), d.msgId());
            }
            case InputUserSelf.ID -> selfId;
            default -> throw new IllegalArgumentException("Unknown input user type: " + inputUser);
        };
    }

    /**
     * Create new id from {@link InputChannel} object.
     *
     * @throws IllegalArgumentException If specified input channel identifier is unknown.
     * @param inputChannel The {@link InputChannel} identifier.
     * @param selfId The id of <i>current</i> user, used for {@link InputChannelFromMessage} handling.
     * @return New {@link Id} from given {@link InputChannel}.
     */
    public static Id of(InputChannel inputChannel, Id selfId) {
        return switch (inputChannel.identifier()) {
            case BaseInputChannel.ID -> {
                var d = (BaseInputChannel) inputChannel;

                yield ofChannel(d.channelId(), d.accessHash());
            }
            case InputChannelFromMessage.ID -> {
                var d = (InputChannelFromMessage) inputChannel;

                yield ofChannel(d.channelId(), of(d.peer(), selfId), d.msgId());
            }
            default -> throw new IllegalArgumentException("Unknown input channel type: " + inputChannel);
        };
    }

    /**
     * Create new id from {@link InputPeer} object.
     *
     * @throws IllegalArgumentException If specified input peer identifier is unknown.
     * @param inputPeer The {@link InputPeer} identifier.
     * @param selfId The id of <i>current</i> user, used for {@link InputPeerSelf} mapping.
     * @return New {@link Id} from given {@link InputPeer}.
     */
    public static Id of(InputPeer inputPeer, Id selfId) {
        return switch (inputPeer.identifier()) {
            case InputPeerChannel.ID -> {
                var d = (InputPeerChannel) inputPeer;

                yield ofChannel(d.channelId(), d.accessHash());
            }
            case InputPeerChannelFromMessage.ID -> {
                var d = (InputPeerChannelFromMessage) inputPeer;

                yield ofChannel(d.channelId(), of(d.peer(), selfId), d.msgId());
            }
            case InputPeerChat.ID -> {
                var d = (InputPeerChat) inputPeer;

                yield ofChat(d.chatId());
            }
            case InputPeerSelf.ID -> selfId;
            case InputPeerUser.ID -> {
                var d = (InputPeerUser) inputPeer;

                yield ofUser(d.userId(), d.accessHash());
            }
            case InputPeerUserFromMessage.ID -> {
                var d = (InputPeerUserFromMessage) inputPeer;

                yield ofUser(d.userId(), of(d.peer(), selfId), d.msgId());
            }
            default -> throw new IllegalArgumentException("Unknown input peer type: " + inputPeer);
        };
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
    public Optional<Long> getAccessHash() {
        return context instanceof Long l ? Optional.of(l) : Optional.empty();
    }

    /**
     * Gets the min information for id, if present and applicable.
     *
     * @return The {@link MinInformation} for id, if present and applicable.
     */
    public Optional<MinInformation> getMinInformation() {
        return context instanceof MinInformation m ? Optional.of(m) : Optional.empty();
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
     * Creates {@link Peer} identifier from this id.
     *
     * @return The {@code Peer} identifier from this id.
     */
    public Peer asPeer() {
        return switch (type) {
            case CHANNEL -> ImmutablePeerChannel.of(value);
            case USER -> ImmutablePeerUser.of(value);
            case CHAT -> ImmutablePeerChat.of(value);
        };
    }

    /**
     * Creates a new {@code Id} with specified access hash, or if
     * it equals to current context info returns this object.
     *
     * @param accessHash The new access hash for this id.
     * @return A new {@code Id} with specified access hash or if it
     * equals to current returns this object.
     */
    public Id withAccessHash(@Nullable Long accessHash) {
        if (Objects.equals(context, accessHash)) return this;
        return new Id(type, value, accessHash);
    }

    /**
     * Creates a new {@code Id} with specified min information, or if
     * it equals to current context info returns this object.
     *
     * @param minInformation The new min information for this id.
     * @return A new {@code Id} with specified min information or if it
     * equals to current returns this object.
     */
    public Id withMinInformation(@Nullable MinInformation minInformation) {
        if (Objects.equals(context, minInformation)) return this;
        return new Id(type, value, minInformation);
    }

    /**
     * Compares this id with the specified id.
     *
     * <p> The comparison is based on the {@link #getType() type}
     * and after by {@link #asLong() raw value} in natural order.
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
        if (!(o instanceof Id i)) return false;
        return type == i.type && value == i.value;
    }

    @Override
    public int hashCode() {
        return type.hashCode() ^ Long.hashCode(value);
    }

    @Override
    public String toString() {
        String prefix = switch (type) {
            case USER -> "User";
            case CHAT -> "Chat";
            case CHANNEL -> "Channel";
        };
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

        /**
         * Creates a new {@code MinInformation} with specified peer and message ids.
         *
         * @throws IllegalArgumentException if {@code messageId} is negative.
         * @param peerId The id of chat where this peer was seen.
         * @param messageId The id of message where this peer was seen.
         */
        public MinInformation(Id peerId, int messageId) {
            Preconditions.requireArgument(messageId > 0, "messageId must be positive");
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
            if (!(o instanceof MinInformation m)) return false;
            return messageId == m.messageId && peerId.equals(m.peerId);
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
