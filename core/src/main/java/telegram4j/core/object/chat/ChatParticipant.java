package telegram4j.core.object.chat;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.object.MentionablePeer;
import telegram4j.core.object.TelegramObject;
import telegram4j.core.object.User;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.util.Id;
import telegram4j.tl.*;
import telegram4j.tl.api.TlObject;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** A {@link GroupChat chat}/{@link Channel channel} participant implementation. */
public final class ChatParticipant implements TelegramObject {
    private final MTProtoTelegramClient client;
    @Nullable
    private final MentionablePeer peer;
    private final TlObject data;
    private final Id chatId;

    public ChatParticipant(MTProtoTelegramClient client, @Nullable MentionablePeer peer, ChannelParticipant data, Id chatId) {
        this.client = Objects.requireNonNull(client);
        this.peer = peer;
        this.data = Objects.requireNonNull(data);
        this.chatId = Objects.requireNonNull(chatId);
    }

    public ChatParticipant(MTProtoTelegramClient client, @Nullable MentionablePeer peer,
                           telegram4j.tl.ChatParticipant data, Id chatId) {
        this.client = Objects.requireNonNull(client);
        this.peer = peer;
        this.data = Objects.requireNonNull(data);
        this.chatId = Objects.requireNonNull(chatId);
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets minimal information about the user/chat to whom this {@code ChatParticipant} is associated, if present.
     *
     * @return The {@link MentionablePeer} object with minimal information, if present.
     */
    private Optional<MentionablePeer> getPeer() {
        return Optional.ofNullable(peer);
    }

    /**
     * Gets {@link Status status} type of participant.
     *
     * @return The status type of participant.
     */
    public Status getStatus() {
        return Status.of0(data);
    }

    /**
     * Gets id of chat/channel with which participant associated.
     *
     * @return The id of chat/channel with which participant associated.
     */
    public Id getChatId() {
        return chatId;
    }

    /**
     * Requests to retrieve chat.
     *
     * @return An {@link Mono} emitting on successful completion the {@link GroupChat group chat} or {@link Channel channel}.
     */
    public Mono<Chat> getChat() {
        return getChat(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve chat using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @return An {@link Mono} emitting on successful completion the {@link GroupChat group chat} or {@link Channel channel}.
     */
    public Mono<Chat> getChat(EntityRetrievalStrategy strategy) {
        return client.withRetrievalStrategy(strategy).getChatById(chatId);
    }

    /**
     * Gets id of participant.
     *
     * @return The id of participant.
     */
    public Id getId() {
        return switch (data.identifier()) {
            case BaseChannelParticipant.ID -> Id.ofUser(((BaseChannelParticipant) data).userId());
            case ChannelParticipantSelf.ID -> Id.ofUser(((ChannelParticipantSelf) data).userId());
            case BaseChatParticipant.ID -> Id.ofUser(((BaseChatParticipant) data).userId());
            case ChannelParticipantCreator.ID -> Id.ofUser(((ChannelParticipantCreator) data).userId());
            case ChatParticipantCreator.ID -> Id.ofUser(((ChatParticipantCreator) data).userId());
            case ChannelParticipantAdmin.ID -> Id.ofUser(((ChannelParticipantAdmin) data).userId());
            case ChatParticipantAdmin.ID -> Id.ofUser(((ChatParticipantAdmin) data).userId());
            case ChannelParticipantBanned.ID -> Id.of(((ChannelParticipantBanned) data).peer());
            case ChannelParticipantLeft.ID -> Id.of(((ChannelParticipantLeft) data).peer());
            default -> throw new IllegalStateException("Unexpected ChatParticipant type: " + data);
        };
    }

    /**
     * Gets participant join timestamp, if present.
     *
     * @return The participant join timestamp, if present.
     */
    public Optional<Instant> getJoinTimestamp() {
        return switch (data.identifier()) {
            case BaseChannelParticipant.ID -> Optional.of(Instant.ofEpochSecond(((BaseChannelParticipant) data).date()));
            case ChannelParticipantSelf.ID -> Optional.of(Instant.ofEpochSecond(((ChannelParticipantSelf) data).date()));
            case BaseChatParticipant.ID -> Optional.of(Instant.ofEpochSecond(((BaseChatParticipant) data).date()));
            case ChannelParticipantAdmin.ID -> Optional.of(Instant.ofEpochSecond(((ChannelParticipantAdmin) data).date()));
            case ChatParticipantAdmin.ID -> Optional.of(Instant.ofEpochSecond(((ChatParticipantAdmin) data).date()));
            case ChannelParticipantBanned.ID -> Optional.of(Instant.ofEpochSecond(((ChannelParticipantBanned) data).date()));
            case ChannelParticipantLeft.ID, ChannelParticipantCreator.ID, ChatParticipantCreator.ID -> Optional.empty();
            default -> throw new IllegalStateException("Unexpected ChatParticipant type: " + data);
        };
    }

    /**
     * Gets id of the inviter user, if present.
     *
     * @return The id of inviter user, if present.
     */
    public Optional<Id> getInviterId() {
        return switch (data.identifier()) {
            case ChannelParticipantSelf.ID -> Optional.of(Id.ofUser(((ChannelParticipantSelf) data).inviterId()));
            case BaseChatParticipant.ID -> Optional.of(Id.ofUser(((BaseChatParticipant) data).inviterId()));
            case ChannelParticipantAdmin.ID -> Optional.ofNullable(((ChannelParticipantAdmin) data).inviterId())
                    .map(Id::ofUser);
            case ChatParticipantAdmin.ID -> Optional.of(Id.ofUser(((ChatParticipantAdmin) data).inviterId()));
            case BaseChannelParticipant.ID, ChannelParticipantBanned.ID, ChannelParticipantLeft.ID,
                    ChannelParticipantCreator.ID, ChatParticipantCreator.ID -> Optional.empty();
            default -> throw new IllegalStateException("Unexpected ChatParticipant type: " + data);
        };
    }

    /**
     * Requests to retrieve the inviter user.
     *
     * @return An {@link Mono} emitting on successful completion the {@link User inviter}.
     */
    public Mono<User> getInviter() {
        return getInviter(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve the inviter user using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @return An {@link Mono} emitting on successful completion the {@link User inviter}.
     */
    public Mono<User> getInviter(EntityRetrievalStrategy strategy) {
        return Mono.justOrEmpty(getInviterId())
                .flatMap(client.withRetrievalStrategy(strategy)::getUserById);
    }

    // ChannelParticipantAdmin#self() flag is ignored

    /**
     * Gets whether this <i>current</i> user can edit this participant.
     *
     * @return {@code true} if <i>current</i> user can edit this participant.
     */
    public boolean isCanEdit() {
        return data instanceof ChannelParticipantAdmin p && p.canEdit();
    }

    /**
     * Gets whether this participant is <i>current</i> user and invited via request.
     *
     * @return {@code true} if participant is <i>current</i> user and invited via request.
     */
    public boolean isInvitedViaRequest() {
        return data instanceof ChannelParticipantSelf p && p.viaRequest();
    }

    /**
     * Gets {@link Set} of permissions this participant, if it's admin and present
     *
     * @return The {@link Set} of permissions this participant, if it's admin and present.
     */
    public Optional<Set<AdminRight>> getAdminRights() {
        return switch (data.identifier()) {
            case ChannelParticipantCreator.ID -> Optional.of(AdminRight.of(((ChannelParticipantCreator) data).adminRights()));
            case ChannelParticipantAdmin.ID -> Optional.of(AdminRight.of(((ChannelParticipantAdmin) data).adminRights()));
            default -> Optional.empty();
        };
    }

    /**
     * Gets rank of participant, if it's admin and present.
     *
     * @return The rank of participant, if it's admin and present.
     */
    public Optional<String> getRank() {
        return switch (data.identifier()) {
            case ChannelParticipantCreator.ID -> Optional.ofNullable(((ChannelParticipantCreator) data).rank());
            case ChannelParticipantAdmin.ID -> Optional.ofNullable(((ChannelParticipantAdmin) data).rank());
            default -> Optional.empty();
        };
    }

    /**
     * Gets whether this participant is banned or left chat/channel.
     * <p>This method more accurate than comparison on {@link Status#LEFT}
     * because considers the exited banned participants.
     *
     * @return {@literal true} if participant is banned or left chat/channel, {@literal false} otherwise.
     */
    public boolean isLeft() {
        return data.identifier() == ChannelParticipantLeft.ID ||
                (data instanceof ChannelParticipantBanned p && p.left());
    }

    /**
     * Gets id of admin which kicks participant, if participant was banned.
     *
     * @return The id of admin which kicks participant, if participant was banned.
     */
    public Optional<Id> getKickerId() {
        return data instanceof ChannelParticipantBanned p
                ? Optional.of(Id.ofUser(p.kickedBy()))
                : Optional.empty();
    }

    /**
     * Requests to retrieve admin which kicks participant.
     *
     * @return An {@link Mono} emitting on successful completion the {@link User admin}.
     */
    public Mono<User> getKicker() {
        return getKicker(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve admin which kicks participant using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @return An {@link Mono} emitting on successful completion the {@link User admin}.
     */
    public Mono<User> getKicker(EntityRetrievalStrategy strategy) {
        return Mono.justOrEmpty(getKickerId())
                .flatMap(client.withRetrievalStrategy(strategy)::getUserById);
    }

    /**
     * Gets permissions overwrite for this participant, if participant was banned.
     *
     * @return The permissions overwrite for this participant, if participant was banned.
     */
    public Optional<ChatRestrictions> getRestrictions() {
        return data instanceof ChannelParticipantBanned p
                ? Optional.of(new ChatRestrictions(p.bannedRights()))
                : Optional.empty();
    }

    /**
     * Gets id of user which promoted this participant to admins, if present.
     *
     * @return The id of user which promoted this participant to admins, if present.
     */
    public Optional<Id> getPromoterId() {
        return data instanceof ChannelParticipantAdmin p
                ? Optional.of(Id.ofUser(p.promotedBy()))
                : Optional.empty();
    }

    /**
     * Requests to retrieve admin which promoted this participant to admins.
     *
     * @return An {@link Mono} emitting on successful completion the {@link User admin}.
     */
    public Mono<User> getPromoter() {
        return getPromoter(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve admin which promoted this participant to admins using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @return An {@link Mono} emitting on successful completion the {@link User admin}.
     */
    public Mono<User> getPromoter(EntityRetrievalStrategy strategy) {
        return Mono.justOrEmpty(getPromoterId())
                .flatMap(client.withRetrievalStrategy(strategy)::getUserById);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatParticipant p)) return false;
        return chatId.equals(p.chatId) && getId().equals(p.getId());
    }

    @Override
    public int hashCode() {
        return chatId.hashCode() + 51 * getId().hashCode();
    }

    @Override
    public String toString() {
        return "ChatParticipant{" +
                "data=" + data +
                ", peer=" + peer +
                '}';
    }

    /** Inferred status types of {@link telegram4j.tl.ChatParticipant} object. */
    public enum Status {
        /** Default chat/channel participant type. */
        DEFAULT,

        /** Status of chat/channel owner. */
        OWNER,

        /** Status of chat/channel admin or user with rank. */
        ADMIN,

        /** Status which indicates a restricted participant. */
        BANNED,

        /** Status which indicated a left from chat/channel participant. */
        LEFT;

        /**
         * Gets type of the {@link ChannelParticipant}.
         *
         * @param data The {@link ChannelParticipant} object to get status.
         * @return The type of channel participant
         */
        public static Status of(ChannelParticipant data) {
            return of0(data);
        }

        /**
         * Gets type of the {@link telegram4j.tl.ChatParticipant}.
         *
         * @param data The {@link telegram4j.tl.ChatParticipant} object to get status.
         * @return The type of chat participant
         */
        public static Status of(telegram4j.tl.ChatParticipant data) {
            return of0(data);
        }

        private static Status of0(TlObject data) {
            return switch (data.identifier()) {
                case BaseChannelParticipant.ID, ChannelParticipantSelf.ID, BaseChatParticipant.ID -> Status.DEFAULT;
                case ChannelParticipantCreator.ID, ChatParticipantCreator.ID -> Status.OWNER;
                case ChannelParticipantAdmin.ID, ChatParticipantAdmin.ID -> Status.ADMIN;
                case ChannelParticipantBanned.ID -> Status.BANNED;
                case ChannelParticipantLeft.ID -> Status.LEFT;
                default -> throw new IllegalStateException("Unexpected ChatParticipant/ChannelParticipant type: " + data);
            };
        }
    }
}
