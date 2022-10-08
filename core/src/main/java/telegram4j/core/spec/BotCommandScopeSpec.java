package telegram4j.core.spec;

import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.chat.Channel;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.chat.PrivateChat;
import telegram4j.core.util.Id;
import telegram4j.tl.*;

import java.util.Objects;
import java.util.Optional;

/** Represents a scope where the bot commands will be valid. */
public class BotCommandScopeSpec implements Spec {
    private final Type type;
    @Nullable
    private final Id peerId;
    @Nullable
    private final Id userId;

    private BotCommandScopeSpec(Type type) {
        this.type = Objects.requireNonNull(type);
        this.peerId = null;
        this.userId = null;
    }

    private BotCommandScopeSpec(Type type, @Nullable Id peerId, @Nullable Id userId) {
        this.type = type;
        this.peerId = peerId;
        this.userId = userId;
    }

    /**
     * Gets type of scope.
     *
     * @return The type of scope.
     */
    public Type type() {
        return type;
    }

    /**
     * Gets id of peer where commands will be available, if {@link #type()}
     * is any of {@link Type#PEER}, {@link Type#PEER_ADMINS} or {@link Type#PEER_USER}.
     *
     * @return The id of peer.
     */
    public Optional<Id> peerId() {
        return Optional.ofNullable(peerId);
    }

    /**
     * Gets id of user for which commands will be available, if {@link #type()} is {@link Type#PEER_USER}.
     *
     * @return The id of user.
     */
    public Optional<Id> userId() {
        return Optional.ofNullable(userId);
    }

    public Mono<BotCommandScope> asData(MTProtoTelegramClient client) {
        return Mono.defer(() -> {
            switch (type) {
                case CHAT_ADMINS: return Mono.just(BotCommandScopeChatAdmins.instance());
                case CHATS: return Mono.just(BotCommandScopeChats.instance());
                case DEFAULT: return Mono.just(BotCommandScopeDefault.instance());
                case PEER:
                    return Mono.justOrEmpty(peerId)
                            .flatMap(client::asInputPeer)
                            .map(ImmutableBotCommandScopePeer::of);
                case PEER_ADMINS:
                    return Mono.justOrEmpty(peerId)
                            .flatMap(client::asInputPeer)
                            .map(ImmutableBotCommandScopePeerAdmins::of);
                case PEER_USER:
                    return Mono.justOrEmpty(peerId)
                            .flatMap(client::asInputPeer)
                            .zipWith(Mono.justOrEmpty(userId).flatMap(client::asInputUser))
                            .map(TupleUtils.function(ImmutableBotCommandScopePeerUser::of));
                case USERS: return Mono.just(BotCommandScopeUsers.instance());
                default: throw new IllegalStateException("Unexpected value: " + type);
            }
        });
    }

    /**
     * Creates command scope with specified type.
     *
     * @throws IllegalArgumentException if {@code type} is any of
     * {@link Type#PEER}, {@link Type#PEER_ADMINS} or {@link Type#PEER_USER}.
     * @param type The type of scope.
     * @return A new {@code BotCommandScopeSpec} scope.
     */
    public static BotCommandScopeSpec of(Type type) {
        if (type == Type.PEER_USER || type == Type.PEER || type == Type.PEER_ADMINS)
            throw new IllegalArgumentException("Unexpected scope type: " + type);
        return new BotCommandScopeSpec(type);
    }

    /**
     * Creates commands scope with type {@link Type#PEER_ADMINS}.
     *
     * @param peerId The id of peer where commands will be available.
     * @return A new {@code BotCommandScopeSpec} scope.
     */
    public static BotCommandScopeSpec peerAdmins(Id peerId) {
        Objects.requireNonNull(peerId);
        return new BotCommandScopeSpec(Type.PEER_ADMINS, peerId, null);
    }

    /**
     * Creates commands scope with type {@link Type#PEER}.
     *
     * @param peerId The id of peer where commands will be available.
     * @return A new {@code BotCommandScopeSpec} scope.
     */
    public static BotCommandScopeSpec peer(Id peerId) {
        Objects.requireNonNull(peerId);
        return new BotCommandScopeSpec(Type.PEER, peerId, null);
    }

    /**
     * Creates commands scope with type {@link Type#PEER_USER}.
     *
     * @throws IllegalArgumentException if {@code userId} have incorrect type.
     * @param peerId The id of peer where commands will be available.
     * @param userId The id of user for which commands will be available.
     * @return A new {@code BotCommandScopeSpec} scope.
     */
    public static BotCommandScopeSpec peerUser(Id peerId, Id userId) {
        Objects.requireNonNull(peerId);
        if (userId.getType() != Id.Type.USER)
            throw new IllegalArgumentException("userId is not types as user: " + userId.getType());
        return new BotCommandScopeSpec(Type.PEER_USER, peerId, userId);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BotCommandScopeSpec that = (BotCommandScopeSpec) o;
        return type == that.type && Objects.equals(peerId, that.peerId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + type.hashCode();
        h += (h << 5) + Objects.hashCode(peerId);
        h += (h << 5) + Objects.hashCode(userId);
        return h;
    }

    @Override
    public String toString() {
        return "BotCommandScopeSpec{" +
                "type=" + type +
                ", peerId=" + peerId +
                ", userId=" + userId +
                '}';
    }

    /** Types of commands scopes. */
    public enum Type {
        /** Scope defining admins in all types of {@link Channel channels}. */
        CHAT_ADMINS,

        /** Scope defining all types of {@link Channel channels}. */
        CHATS,

        /** Scope defining all types of {@link Chat chats}. */
        DEFAULT,

        /** Scope defining specified peer. */
        PEER,

        /** Scope defining admins in specified peer. */
        PEER_ADMINS,

        /** Scope defining specified peer and user. */
        PEER_USER,

        /** Scope defining all {@link PrivateChat private chats}. */
        USERS
    }
}
