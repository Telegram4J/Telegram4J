package telegram4j.core.object.chat;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.ChatAdminRights;
import telegram4j.core.object.ChatBannedRightsSettings;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.TelegramObject;
import telegram4j.core.util.Id;
import telegram4j.tl.*;
import telegram4j.tl.api.TlObject;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

/** A {@link GroupChat chat}/{@link Channel channel} participant implementation. */
public final class ChatParticipant implements TelegramObject {
    private final MTProtoTelegramClient client;
    @Nullable
    private final PeerEntity peer;
    private final TlObject data;
    private final Id chatId;

    public ChatParticipant(MTProtoTelegramClient client, @Nullable PeerEntity peer, ChannelParticipant data, Id chatId) {
        this.client = Objects.requireNonNull(client);
        this.peer = peer;
        this.data = Objects.requireNonNull(data);
        this.chatId = Objects.requireNonNull(chatId);
    }

    public ChatParticipant(MTProtoTelegramClient client, @Nullable PeerEntity peer, telegram4j.tl.ChatParticipant data, Id chatId) {
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
     * @return The {@link PeerEntity} object with minimal information, if present.
     */
    private Optional<PeerEntity> getPeer() {
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
     * Gets id of participant.
     *
     * @return The id of participant.
     */
    public Id getId() {
        switch (data.identifier()) {
            case BaseChannelParticipant.ID: return Id.ofUser(((BaseChannelParticipant) data).userId(), null);
            case ChannelParticipantSelf.ID: return Id.ofUser(((ChannelParticipantSelf) data).userId(), null);
            case BaseChatParticipant.ID: return Id.ofUser(((BaseChatParticipant) data).userId(), null);
            case ChannelParticipantCreator.ID: return Id.ofUser(((ChannelParticipantCreator) data).userId(), null);
            case ChatParticipantCreator.ID: return Id.ofUser(((ChatParticipantCreator) data).userId(), null);
            case ChannelParticipantAdmin.ID: return Id.ofUser(((ChannelParticipantAdmin) data).userId(), null);
            case ChatParticipantAdmin.ID: return Id.ofUser(((ChatParticipantAdmin) data).userId(), null);
            case ChannelParticipantBanned.ID: return Id.of(((ChannelParticipantBanned) data).peer());
            case ChannelParticipantLeft.ID: return Id.of(((ChannelParticipantLeft) data).peer());
            default: throw new IllegalStateException("Unexpected ChatParticipant type: " + data);
        }
    }

    /**
     * Gets participant join timestamp, if present.
     *
     * @return The participant join timestamp, if present.
     */
    public Optional<Instant> getJoinTimestamp() {
        switch (data.identifier()) {
            case BaseChannelParticipant.ID: return Optional.of(Instant.ofEpochSecond(((BaseChannelParticipant) data).date()));
            case ChannelParticipantSelf.ID: return Optional.of(Instant.ofEpochSecond(((ChannelParticipantSelf) data).date()));
            case BaseChatParticipant.ID: return Optional.of(Instant.ofEpochSecond(((BaseChatParticipant) data).date()));
            case ChannelParticipantAdmin.ID: return Optional.of(Instant.ofEpochSecond(((ChannelParticipantAdmin) data).date()));
            case ChatParticipantAdmin.ID: return Optional.of(Instant.ofEpochSecond(((ChatParticipantAdmin) data).date()));
            case ChannelParticipantBanned.ID: return Optional.of(Instant.ofEpochSecond(((ChannelParticipantBanned) data).date()));
            case ChannelParticipantLeft.ID:
            case ChannelParticipantCreator.ID:
            case ChatParticipantCreator.ID: return Optional.empty();
            default: throw new IllegalStateException("Unexpected ChatParticipant type: " + data);
        }
    }

    /**
     * Gets id of the inviter user, if present.
     *
     * @return The id of inviter user, if present.
     */
    public Optional<Id> getInviterId() {
        switch (data.identifier()) {
            case ChannelParticipantSelf.ID: return Optional.of(Id.ofUser(((ChannelParticipantSelf) data).inviterId(), null));
            case BaseChatParticipant.ID: return Optional.of(Id.ofUser(((BaseChatParticipant) data).inviterId(), null));
            case ChannelParticipantAdmin.ID: return Optional.ofNullable(((ChannelParticipantAdmin) data).inviterId())
                    .map(l -> Id.ofUser(l, null));
            case ChatParticipantAdmin.ID: return Optional.of(Id.ofUser(((ChatParticipantAdmin) data).inviterId(), null));
            case BaseChannelParticipant.ID:
            case ChannelParticipantBanned.ID:
            case ChannelParticipantLeft.ID:
            case ChannelParticipantCreator.ID:
            case ChatParticipantCreator.ID: return Optional.empty();
            default: throw new IllegalStateException("Unexpected ChatParticipant type: " + data);
        }
    }

    /**
     * Gets whether this chat participant is <i>current</i> user.
     *
     * @return {@literal true} if participant is <i>current</i> user, {@literal false} otherwise.
     */
    public boolean isSelf() {
        return data.identifier() == ChannelParticipantSelf.ID ||
                (data.identifier() == ChannelParticipantAdmin.ID &&
                ((ChannelParticipantAdmin) data).self());
    }

    /**
     * Gets whether this <i>current</i> user can edit this participant.
     *
     * @return {@literal true} if <i>current</i> user can edit this participant, {@literal false} otherwise.
     */
    public boolean isCanEdit() {
        return data.identifier() == ChannelParticipantAdmin.ID
                && ((ChannelParticipantAdmin) data).canEdit();
    }

    /**
     * Gets whether this participant is <i>current</i> user and invited via request.
     *
     * @return {@literal true} if participant is <i>current</i> user and invited via request, {@literal false} otherwise.
     */
    public boolean isInvitedViaRequest() {
        return data.identifier() == ChannelParticipantSelf.ID
                && ((ChannelParticipantSelf) data).viaRequest();
    }

    /**
     * Gets {@link EnumSet} of permissions this participant, if it's admin or present
     *
     * @return The {@link EnumSet} of permissions this participant, if it's admin or present.
     */
    public Optional<EnumSet<ChatAdminRights>> getAdminRights() {
        switch (data.identifier()) {
            case ChannelParticipantCreator.ID: return Optional.of(ChatAdminRights.of(((ChannelParticipantCreator) data).adminRights()));
            case ChannelParticipantAdmin.ID: return Optional.of(ChatAdminRights.of(((ChannelParticipantAdmin) data).adminRights()));
            case ChatParticipantAdmin.ID:
            case BaseChatParticipant.ID:
            case ChannelParticipantSelf.ID:
            case BaseChannelParticipant.ID:
            case ChannelParticipantBanned.ID:
            case ChannelParticipantLeft.ID:
            case ChatParticipantCreator.ID: return Optional.empty();
            default: throw new IllegalStateException("Unexpected ChatParticipant type: " + data);
        }
    }

    /**
     * Gets rank of participant, if it's admin or present.
     *
     * @return The rank of participant, if it's admin or present.
     */
    public Optional<String> getRank() {
        switch (data.identifier()) {
            case ChannelParticipantCreator.ID: return Optional.ofNullable(((ChannelParticipantCreator) data).rank());
            case ChannelParticipantAdmin.ID: return Optional.ofNullable(((ChannelParticipantAdmin) data).rank());
            case ChatParticipantAdmin.ID:
            case BaseChatParticipant.ID:
            case ChannelParticipantSelf.ID:
            case BaseChannelParticipant.ID:
            case ChannelParticipantBanned.ID:
            case ChannelParticipantLeft.ID:
            case ChatParticipantCreator.ID: return Optional.empty();
            default: throw new IllegalStateException("Unexpected ChatParticipant type: " + data);
        }
    }

    /**
     * Gets whether this participant is banned or left chat/channel.
     *
     * @return {@literal true} if participant is banned or left chat/channel, {@literal false} otherwise.
     */
    public boolean isLeft() {
        return data.identifier() == ChannelParticipantLeft.ID ||
                (data.identifier() == ChannelParticipantBanned.ID
                && ((ChannelParticipantBanned) data).left());
    }

    /**
     * Gets id of admin which kicks participant, if participant was banned.
     *
     * @return The id of admin which kicks participant, if participant was banned.
     */
    public Optional<Id> getKickerId() {
        return data.identifier() == ChannelParticipantBanned.ID
                ? Optional.of(((ChannelParticipantBanned) data).kickedBy()).map(l -> Id.ofUser(l, null))
                : Optional.empty();
    }

    /**
     * Gets permissions overwrite for this participant, if participant was banned.
     *
     * @return The permissions overwrite for this participant, if participant was banned.
     */
    public Optional<ChatBannedRightsSettings> getBannedRights() {
        return data.identifier() == ChannelParticipantBanned.ID
                ? Optional.of(new ChatBannedRightsSettings(((ChannelParticipantBanned) data).bannedRights()))
                : Optional.empty();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatParticipant that = (ChatParticipant) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "ChatParticipant{" +
                "data=" + data +
                '}';
    }

    /** Inferred status types of {@link telegram4j.tl.ChatParticipant} object. */
    public enum Status {
        /** Default chat/channel participant type. */
        DEFAULT,

        /** Status of chat/channel owner. */
        CREATOR,

        /** Status of chat/channel admin or user with rank. */
        ADMIN,

        /** Status which indicates a banned participant. */
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
            switch (data.identifier()) {
                case BaseChannelParticipant.ID:
                case ChannelParticipantSelf.ID:
                case BaseChatParticipant.ID: return Status.DEFAULT;
                case ChannelParticipantCreator.ID:
                case ChatParticipantCreator.ID: return Status.CREATOR;
                case ChannelParticipantAdmin.ID:
                case ChatParticipantAdmin.ID: return Status.ADMIN;
                case ChannelParticipantBanned.ID: return Status.BANNED;
                case ChannelParticipantLeft.ID: return Status.LEFT;
                default: throw new IllegalStateException("Unexpected ChatParticipant/ChannelParticipant type: " + data);
            }
        }
    }
}
