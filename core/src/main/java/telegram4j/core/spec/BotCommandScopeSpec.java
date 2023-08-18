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
package telegram4j.core.spec;

import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.internal.MonoSpec;
import telegram4j.mtproto.internal.Preconditions;
import telegram4j.core.object.chat.Channel;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.chat.PrivateChat;
import telegram4j.core.util.Id;
import telegram4j.tl.*;

import java.util.Objects;
import java.util.Optional;

/** Represents a scope where the bot commands will be valid. */
public final class BotCommandScopeSpec implements MonoSpec<BotCommandScope> {
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

    @Override
    public Mono<BotCommandScope> resolve(MTProtoTelegramClient client) {
        return Mono.defer(() -> switch (type) {
            case CHAT_ADMINS -> Mono.just(BotCommandScopeChatAdmins.instance());
            case CHATS -> Mono.just(BotCommandScopeChats.instance());
            case DEFAULT -> Mono.just(BotCommandScopeDefault.instance());
            case PEER -> {
                Objects.requireNonNull(peerId);
                yield client.asInputPeerExact(peerId)
                        .map(ImmutableBotCommandScopePeer::of);
            }
            case PEER_ADMINS -> {
                Objects.requireNonNull(peerId);
                yield client.asInputPeerExact(peerId)
                        .map(ImmutableBotCommandScopePeerAdmins::of);
            }
            case PEER_USER -> {
                Objects.requireNonNull(peerId);
                Objects.requireNonNull(userId);
                yield Mono.zip(client.asInputPeerExact(peerId), client.asInputUserExact(userId),
                        ImmutableBotCommandScopePeerUser::of);
            }
            case USERS -> Mono.just(BotCommandScopeUsers.instance());
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
        Preconditions.requireArgument(type != Type.PEER && type != Type.PEER_USER && type != Type.PEER_ADMINS,
                () -> "Unexpected scope type: " + type);
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
        Preconditions.requireArgument(userId.getType() == Id.Type.USER, () ->
                "userId is not types as user: " + userId.getType());
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
