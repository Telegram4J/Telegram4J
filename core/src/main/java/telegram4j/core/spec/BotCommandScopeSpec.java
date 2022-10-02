package telegram4j.core.spec;

import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.util.Id;
import telegram4j.tl.*;

import java.util.Objects;
import java.util.Optional;

public class BotCommandScopeSpec implements Spec {
    private final Type type;
    private final Id peerId;
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

    public Type type() {
        return type;
    }

    public Optional<Id> peerId() {
        return Optional.ofNullable(peerId);
    }

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

    public static BotCommandScopeSpec of(Type type) {
        if (type == Type.PEER_USER || type == Type.PEER || type == Type.PEER_ADMINS)
            throw new IllegalArgumentException("Unexpected scope type: " + type);
        return new BotCommandScopeSpec(type);
    }

    public static BotCommandScopeSpec peerAdmins(Id peerId) {
        Objects.requireNonNull(peerId);
        return new BotCommandScopeSpec(Type.PEER_ADMINS, peerId, null);
    }

    public static BotCommandScopeSpec peer(Id peerId) {
        Objects.requireNonNull(peerId);
        return new BotCommandScopeSpec(Type.PEER, peerId, null);
    }

    public static BotCommandScopeSpec peerUser(Id peerId, Id userId) {
        Objects.requireNonNull(peerId);
        Objects.requireNonNull(userId);
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

    public enum Type {
        CHAT_ADMINS,
        CHATS,
        DEFAULT,
        PEER,
        PEER_ADMINS,
        PEER_USER,
        USERS
    }
}
