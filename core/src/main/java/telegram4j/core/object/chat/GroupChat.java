package telegram4j.core.object.chat;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.BotInfo;
import telegram4j.core.object.ChatAdminRights;
import telegram4j.core.object.ChatPhoto;
import telegram4j.core.object.ExportedChatInvite;
import telegram4j.core.object.PeerNotifySettings;
import telegram4j.core.object.Photo;
import telegram4j.core.object.Reaction;
import telegram4j.core.object.*;
import telegram4j.core.util.EntityFactory;
import telegram4j.core.util.Id;
import telegram4j.core.util.Variant2;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static telegram4j.tl.BaseChat.*;
import static telegram4j.tl.BaseChatFull.CAN_SET_USERNAME_POS;
import static telegram4j.tl.BaseChatFull.HAS_SCHEDULED_POS;

/** Represents a basic group of 0-200 users (must be upgraded to a supergroup to accommodate more than 200 users). */
public final class GroupChat extends BaseChat {

    private final telegram4j.tl.BaseChat minData;
    @Nullable
    private final telegram4j.tl.BaseChatFull fullData;
    @Nullable
    private final ExportedChatInvite exportedChatInvite;
    @Nullable
    private final List<ChatParticipant> chatParticipants;

    public GroupChat(MTProtoTelegramClient client, telegram4j.tl.BaseChat minData) {
        super(client);
        this.minData = Objects.requireNonNull(minData);
        this.fullData = null;
        this.exportedChatInvite = null;
        this.chatParticipants = null;
    }

    public GroupChat(MTProtoTelegramClient client, BaseChatFull fullData,
                     telegram4j.tl.BaseChat minData, @Nullable ExportedChatInvite exportedChatInvite,
                     @Nullable List<ChatParticipant> chatParticipants) {
        super(client);
        this.minData = Objects.requireNonNull(minData);
        this.fullData = Objects.requireNonNull(fullData);
        this.exportedChatInvite = exportedChatInvite;
        this.chatParticipants = chatParticipants;
    }

    @Override
    public Id getId() {
        return Id.ofChat(minData.id());
    }

    @Override
    public Type getType() {
        return Type.GROUP;
    }

    @Override
    public Optional<ChatPhoto> getMinPhoto() {
        return Optional.ofNullable(TlEntityUtil.unmapEmpty(minData.photo(), BaseChatPhoto.class))
                .map(d -> new ChatPhoto(client, d, ImmutableInputPeerChat.of(minData.id()), -1));
    }

    @Override
    public Optional<Photo> getPhoto() {
        return Optional.ofNullable(fullData)
                .map(d -> TlEntityUtil.unmapEmpty(d.chatPhoto(), BasePhoto.class))
                .map(d -> new Photo(client, d, -1, ImmutableInputPeerChat.of(minData.id())));
    }

    @Override
    public Optional<Integer> getPinnedMessageId() {
        return Optional.ofNullable(fullData).map(BaseChatFull::pinnedMsgId);
    }

    @Override
    public Optional<Duration> getMessageAutoDeleteDuration() {
        return Optional.ofNullable(fullData)
                .map(BaseChatFull::ttlPeriod)
                .map(Duration::ofSeconds);
    }

    @Override
    public String getName() {
        return minData.title();
    }

    @Override
    public Optional<String> getAbout() {
        return Optional.ofNullable(fullData).map(BaseChatFull::about);
    }

    @Override
    public Optional<PeerNotifySettings> getNotifySettings() {
        return Optional.ofNullable(fullData)
                .map(BaseChatFull::notifySettings)
                .map(PeerNotifySettings::new);
    }

    @Override
    public Optional<Integer> getFolderId() {
        return Optional.ofNullable(fullData).map(BaseChatFull::folderId);
    }

    @Override
    public Optional<String> getThemeEmoticon() {
        return Optional.ofNullable(fullData).map(BaseChatFull::themeEmoticon);
    }

    /**
     * Gets id of channel to which this chat was migrated, if present.
     *
     * @see <a href="https://core.telegram.org/api/channel#migration">Chat Migration</a>
     * @return The id of channel to which this chat was migrated, if present.
     */
    public Optional<Id> getMigratedToChannelId() {
        return Optional.ofNullable(minData.migratedTo()).map(p -> Id.of(p, client.getSelfId()));
    }

    /**
     * Gets current participants count in this chat.
     *
     * @return The current participants count in this chat.
     */
    public int getParticipantsCount() {
        return minData.participantsCount();
    }

    /**
     * Gets timestamp of chat creation.
     *
     * @return The {@link Instant} of chat creation.
     */
    public Instant getCreateTimestamp() {
        return Instant.ofEpochSecond(minData.date());
    }

    /**
     * Gets current modification version of chat.
     *
     * @return The current modification version of chat.
     */
    public int getVersion() {
        return minData.version();
    }

    /**
     * Gets {@link EnumSet} with admin rights of the user in this chat, if present.
     *
     * @return The {@link EnumSet} of admin rights for users in this chat, if present.
     */
    public Optional<EnumSet<ChatAdminRights>> getAdminRights() {
        return Optional.ofNullable(minData.adminRights()).map(ChatAdminRights::of);
    }

    /**
     * Gets {@link EnumSet} with disallowed rights for users in this chat, if present.
     *
     * @return The {@link EnumSet} of disallowed rights for users in this chat, if present.
     */
    public Optional<ChatBannedRightsSettings> getDefaultBannedRights() {
        return Optional.ofNullable(minData.defaultBannedRights()).map(ChatBannedRightsSettings::new);
    }

    /**
     * Gets {@link EnumSet} with chat flags from full and min data.
     *
     * @return The {@link EnumSet} with chat flags.
     */
    public EnumSet<Flag> getFlags() {
        return Flag.of(fullData, minData);
    }

    /**
     * Gets list of {@link ChatParticipant participants} in this chat, if present.
     * Can contain only participant info about <i>current</i> user if it's joined.
     *
     * @return The list of {@link ChatParticipant participants} in this chat, if present.
     */
    public Optional<List<ChatParticipant>> getParticipants() {
        return Optional.ofNullable(chatParticipants);
    }

    /**
     * Gets invite link for chat, if present.
     *
     * @return The {@link ExportedChatInvite invite} for chat, if present.
     */
    public Optional<ExportedChatInvite> getExportedInvite() {
        return Optional.ofNullable(exportedChatInvite);
    }

    /**
     * Gets list of {@link BotInfo bots} of chat, if present.
     *
     * @return The list of {@link BotInfo bots} of chat, if present.
     */
    public Optional<List<BotInfo>> getBotInfo() {
        return Optional.ofNullable(fullData)
                .map(BaseChatFull::botInfo)
                .map(list -> list.stream()
                        .map(d -> new BotInfo(client, d))
                        .collect(Collectors.toList()));
    }

    /**
     * Gets id of current group call/livestream, if present.
     *
     * @return The id of current group call/livestream, if present.
     */
    public Optional<InputGroupCall> getCall() {
        return Optional.ofNullable(fullData).map(BaseChatFull::call);
    }

    /**
     * Gets id of peer, that selects by default on group call, if present.
     *
     * @return The id of peer, that selects by default on group call, if present.
     */
    public Optional<Id> getGroupCallDefaultJoinAs() {
        return Optional.ofNullable(fullData)
                .map(BaseChatFull::groupcallDefaultJoinAs)
                .map(Id::of);
    }

    /**
     * Gets count of pending join requests, if present.
     *
     * @see <a href="https://core.telegram.org/api/invites#join-requests">Join Requests</a>
     * @return The count of pending join requests, if present.
     */
    public Optional<Integer> getRequestsPending() {
        return Optional.ofNullable(fullData).map(BaseChatFull::requestsPending);
    }

    /**
     * Gets list of user ids, who requested to join recently, if present.
     *
     * @see <a href="https://core.telegram.org/api/invites#join-requests">Join Requests</a>
     * @return The list of user ids, who requested to join recently, if present.
     */
    public Optional<List<Id>> getRecentRequesters() {
        return Optional.ofNullable(fullData)
                .map(BaseChatFull::recentRequesters)
                .map(list -> list.stream()
                        .map(l -> Id.ofUser(l, null))
                        .collect(Collectors.toList()));
    }

    /**
     * Gets {@code boolean} which indicates the availability of any emojis in the group
     * or list of available reactions, if present
     * and if detailed information about channel is available.
     *
     * @return The {@link Variant2} with {@code boolean} which indicates the availability of any emojis in the group
     * or list of available reactions, if present
     * and if detailed information about channel is available.
     */
    public Optional<Variant2<Boolean, List<Reaction>>> getAvailableReactions() {
        return Optional.ofNullable(fullData)
                .map(BaseChatFull::availableReactions)
                .map(EntityFactory::createChatReactions);
    }

    /**
     * Request to kick user from group chat.
     *
     * @param userId The id to kick.
     * @param revokeHistory The remove the entire chat history of the specified user in this chat.
     * @return A {@link Mono} emitting on successful completion nothing.
     */
    public Mono<Void> deleteChatParticipant(Id userId, boolean revokeHistory) {
        return client.asInputUser(userId)
                .flatMap(p -> client.getServiceHolder().getChatService()
                        .deleteChatUser(minData.id(), p, revokeHistory));
    }

    /**
     * Request to leave group chat.
     *
     * @see #deleteChatParticipant(Id, boolean)
     * @param revokeHistory The remove the entire chat history of self in this chat.
     * @return A {@link Mono} emitting on successful completion nothing.
     */
    public Mono<Void> leave(boolean revokeHistory) {
        return client.getServiceHolder().getChatService()
                .deleteChatUser(minData.id(), InputUserSelf.instance(), revokeHistory);
    }

    /**
     * Requests to edit current chat title.
     *
     * @param newTitle A new title for chat.
     * @return A {@link Mono} emitting on successful completion nothing.
     */
    public Mono<Void> editTitle(String newTitle) {
        return client.getServiceHolder().getChatService()
                .editChatTitle(minData.id(), newTitle);
    }

    /**
     * Requests to edit current chat photo.
     *
     * @param photo A new photo for chat, {@code null} value indicates removing.
     * @return A {@link Mono} emitting on successful completion nothing.
     */
    public Mono<Void> editPhoto(@Nullable BaseInputPhoto photo) {
        return Mono.justOrEmpty(photo)
                .<InputChatPhoto>map(ImmutableBaseInputChatPhoto::of)
                .defaultIfEmpty(InputChatPhotoEmpty.instance())
                .flatMap(c -> client.getServiceHolder().getChatService()
                        .editChatPhoto(minData.id(), c));
    }

    /**
     * Requests to edit current chat photo.
     *
     * @param spec A new uploaded photo for chat, {@code null} value indicates removing.
     * @return A {@link Mono} emitting on successful completion nothing.
     */
    public Mono<Void> editPhoto(@Nullable InputChatUploadedPhoto spec) {
        return Mono.<InputChatPhoto>justOrEmpty(spec)
                .defaultIfEmpty(InputChatPhotoEmpty.instance())
                .flatMap(c -> client.getServiceHolder().getChatService()
                        .editChatPhoto(minData.id(), c));
    }

    /**
     * Requests to edit group description.
     *
     * @param newAbout The new description to set.
     * @return A {@link Mono} emitting on successful completion completion status.
     */
    public Mono<Boolean> editAbout(String newAbout) {
        InputPeer peer = client.asResolvedInputPeer(getId());

        return client.getServiceHolder().getChatService()
                .editChatAbout(peer, newAbout);
    }

    @Override
    public String toString() {
        return "GroupChat{" +
                "minData=" + minData +
                ", fullData=" + fullData +
                '}';
    }

    /** Types of the group chat flags. */
    public enum Flag implements BitFlag {
        // MinChat flags

        /** Whether the current user is the creator of the group. */
        CREATOR(CREATOR_POS),

        /** Whether the current user has left the group. */
        LEFT(LEFT_POS),

        /** Whether the group was <a href="https://core.telegram.org/api/channel">migrated</a>. */
        DEACTIVATED(DEACTIVATED_POS),

        /** Whether a group call is currently active. */
        CALL_ACTIVE(CALL_ACTIVE_POS),

        /** Whether there's anyone in the group call. */
        CALL_NOT_EMPTY(CALL_NOT_EMPTY_POS),

        /**
         * Whether this group is <a href="https://telegram.org/blog/protected-content-delete-by-date-and-more">protected</a>,
         * this does not allow forwarding messages from it.
         */
        NO_FORWARDS(NOFORWARDS_POS),

        // FullChat flags

        /** Can we change the username of this chat? */
        CAN_SET_USERNAME(CAN_SET_USERNAME_POS),

        /** Whether <a href="https://core.telegram.org/api/scheduled-messages">scheduled messages</a> are available. */
        HAS_SCHEDULED(HAS_SCHEDULED_POS);

        private final byte position;

        Flag(byte position) {
            this.position = position;
        }

        @Override
        public byte position() {
            return position;
        }

        /**
         * Computes {@link EnumSet} with chat flags from given min and full data.
         *
         * @param fullData The full chat data.
         * @param minData The min chat data.
         * @return The {@link EnumSet} with channel flags.
         */
        public static EnumSet<Flag> of(@Nullable telegram4j.tl.BaseChatFull fullData, telegram4j.tl.BaseChat minData) {
            var minFlags = of(minData);
            if (fullData != null) {
                var set = EnumSet.allOf(Flag.class);
                int flags = fullData.flags();
                set.removeIf(value -> value.ordinal() < CAN_SET_USERNAME.ordinal() || (flags & value.mask()) == 0);
                set.addAll(minFlags);
                return set;
            }

            return minFlags;
        }

        /**
         * Computes {@link EnumSet} with chat flags from given min data.
         *
         * @param data The min chat data.
         * @return The {@link EnumSet} with chat flags.
         */
        public static EnumSet<Flag> of(telegram4j.tl.BaseChat data) {
            var set = EnumSet.allOf(Flag.class);
            int flags = data.flags();
            set.removeIf(value -> value.ordinal() >= CAN_SET_USERNAME.ordinal() || (flags & value.mask()) == 0);
            return set;
        }
    }
}
