package telegram4j.core.object.chat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.internal.EntityFactory;
import telegram4j.core.internal.MappingUtil;
import telegram4j.mtproto.internal.Preconditions;
import telegram4j.core.object.BotInfo;
import telegram4j.core.object.PeerNotifySettings;
import telegram4j.core.object.Photo;
import telegram4j.core.object.User;
import telegram4j.core.object.*;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.util.BitFlag;
import telegram4j.core.util.Id;
import telegram4j.mtproto.file.Context;
import telegram4j.tl.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static telegram4j.tl.BaseChat.*;
import static telegram4j.tl.BaseChatFull.*;

/** Represents a basic group of 0-200 users. */
public final class GroupChat extends BaseChat implements GroupChatPeer {

    private final telegram4j.tl.BaseChat minData;
    @Nullable
    private final telegram4j.tl.BaseChatFull fullData;
    @Nullable
    private final List<ChatParticipant> participants;
    @Nullable
    private final List<BotInfo> botInfo;

    public GroupChat(MTProtoTelegramClient client, telegram4j.tl.BaseChat minData) {
        super(client);
        this.minData = Objects.requireNonNull(minData);
        this.fullData = null;
        this.participants = null;
        this.botInfo = null;
    }

    public GroupChat(MTProtoTelegramClient client, @Nullable BaseChatFull fullData,
                     telegram4j.tl.BaseChat minData,
                     @Nullable List<ChatParticipant> participants, @Nullable List<BotInfo> botInfo) {
        super(client);
        this.minData = Objects.requireNonNull(minData);
        this.fullData = Objects.requireNonNull(fullData);
        this.participants = participants;
        this.botInfo = botInfo;
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
    public Optional<ProfilePhoto> getMinPhoto() {
        return minData instanceof BaseChatPhoto p
                ? Optional.of(new ProfilePhoto(client, p, ImmutableInputPeerChat.of(minData.id())))
                : Optional.empty();
    }

    @Override
    public Optional<Photo> getPhoto() {
        if (fullData == null || !(fullData.chatPhoto() instanceof BasePhoto p)) {
            return Optional.empty();
        }
        return Optional.of(new Photo(client, p, Context.createChatPhotoContext(
                ImmutableInputPeerChat.of(minData.id()), -1)));
    }

    @Override
    public Optional<Integer> getPinnedMessageId() {
        return Optional.ofNullable(fullData).map(BaseChatFull::pinnedMsgId);
    }

    @Override
    public Mono<AuxiliaryMessages> getPinnedMessage(EntityRetrievalStrategy strategy) {
        return Mono.justOrEmpty(fullData)
                .mapNotNull(BaseChatFull::pinnedMsgId)
                .flatMap(id -> client.withRetrievalStrategy(strategy)
                        .getMessages(getId(), List.of(ImmutableInputMessageID.of(id))));
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
     * Requests to retrieve channel to which this chat was migrated.
     *
     * @return An {@link Mono} emitting on successful completion the {@link SupergroupChat supergroup}.
     */
    public Mono<SupergroupChat> getMigratedToChannel() {
        return getMigratedToChannel(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve channel to which this chat was migrated using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @return An {@link Mono} emitting on successful completion the {@link SupergroupChat supergroup}.
     */
    public Mono<SupergroupChat> getMigratedToChannel(EntityRetrievalStrategy strategy) {
        return Mono.justOrEmpty(getMigratedToChannelId())
                .flatMap(client.withRetrievalStrategy(strategy)::getChatById)
                .cast(SupergroupChat.class);
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
     * Gets {@link Set} with admin rights of the user in this chat, if present.
     *
     * @return The {@link Set} of admin rights for users in this chat, if present.
     */
    public Optional<Set<AdminRight>> getAdminRights() {
        return Optional.ofNullable(minData.adminRights()).map(AdminRight::of);
    }

    /**
     * Gets default settings with disallowed rights for users in this chat, if present.
     *
     * @return The default settings of disallowed rights for users in this chat, if present.
     */
    public Optional<ChatRestrictions> getDefaultRestrictions() {
        return Optional.ofNullable(minData.defaultBannedRights()).map(ChatRestrictions::new);
    }

    /**
     * Gets mutable {@link Set} of chat flags from full and min data.
     *
     * @return The mutable {@link Set} of chat flags.
     */
    public Set<Flag> getFlags() {
        return Flag.of(fullData, minData);
    }

    /**
     * Gets version of participant's list, if present and {@link Flag#CAN_VIEW_PARTICIPANTS} flag is present.
     *
     * @return The version of participant's list, if present and {@link Flag#CAN_VIEW_PARTICIPANTS} flag is present.
     */
    public Optional<Integer> getParticipantsVersion() {
        if (fullData == null || !(fullData.participants() instanceof BaseChatParticipants p)) {
            return Optional.empty();
        }
        return Optional.of(p.version());
    }

    /**
     * Gets list of {@link ChatParticipant participants} in this chat,
     * if present and full information about chat is available.
     * <p>
     * Can contain only participant info about <i>current</i> user, to determinate this situation
     * check {@link Flag#CAN_VIEW_PARTICIPANTS} is not present in {@link #getFlags() flags}.
     *
     * @return The list of {@link ChatParticipant participants} in this chat, if present.
     */
    public Optional<List<ChatParticipant>> getParticipants() {
        return Optional.ofNullable(participants);
    }

    /**
     * Gets chat participant by specified user id, if present and full information about chat is available.
     * <p>
     * Can contain only participant info about <i>current</i> user, to determinate this situation
     * check {@link Flag#CAN_VIEW_PARTICIPANTS} is not present in {@link #getFlags() flags}.
     *
     * @param userId The id of user.
     * @return The list of {@link ChatParticipant participants} in this chat, if present.
     */
    public Optional<ChatParticipant> getParticipant(Id userId) {
        Preconditions.requireArgument(userId.getType() == Id.Type.USER, () -> "Unexpected type of id: " + userId);
        return Optional.ofNullable(participants)
                .flatMap(l -> l.stream()
                        .filter(c -> c.getId().equals(userId))
                        .findFirst());
    }

    /**
     * Gets invite link for chat, if present.
     *
     * @return The {@link ExportedChatInvite invite} for chat, if present.
     */
    public Optional<ExportedChatInvite> getExportedInvite() {
        return Optional.ofNullable(fullData).map(d -> new ExportedChatInvite(client, (ChatInviteExported) d));
    }

    /**
     * Gets mutable list of {@link BotInfo bots} of chat, if present.
     *
     * @return The mutable list of {@link BotInfo bots} of chat, if present.
     */
    public Optional<List<BotInfo>> getBotInfo() {
        return Optional.ofNullable(botInfo);
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
    public Optional<Id> getGroupCallDefaultJoinAsId() {
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
    public Optional<Integer> getPendingRequests() {
        return Optional.ofNullable(fullData).map(BaseChatFull::requestsPending);
    }

    /**
     * Gets mutable set of user ids, who requested to join recently, if present.
     *
     * @see <a href="https://core.telegram.org/api/invites#join-requests">Join Requests</a>
     * @return The mutable set of user ids, who requested to join recently, if present.
     */
    public Optional<Set<Id>> getRecentRequestersIds() {
        return Optional.ofNullable(fullData)
                .map(BaseChatFull::recentRequesters)
                .map(list -> list.stream()
                        .map(Id::ofUser)
                        .collect(Collectors.toSet()));
    }

    /**
     * Requests to retrieve the user who requested to join.
     *
     * @see #getRecentRequestersIds()
     * @return A {@link Flux} which emits the {@link User requesters}.
     */
    public Flux<User> getRecentRequesters() {
        return getRecentRequesters(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve the user who requested to join using specified retrieval strategy.
     *
     * @see #getRecentRequestersIds()
     * @param strategy The strategy to apply.
     * @return A {@link Flux} which emits the {@link User requesters}.
     */
    public Flux<User> getRecentRequesters(EntityRetrievalStrategy strategy) {
        var retriever = client.withRetrievalStrategy(strategy);
        return Mono.justOrEmpty(fullData)
                .mapNotNull(ChatFull::recentRequesters)
                .flatMapIterable(Function.identity())
                .flatMap(id -> retriever.getUserById(Id.ofUser(id)));
    }

    /**
     * Gets settings for allowed emojis in the chat.
     *
     * @return The settings for allowed emojis in the chat, if present
     * and full information about group chat available.
     */
    public Optional<ChatReactions> getAvailableReactions() {
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
                .switchIfEmpty(MappingUtil.unresolvedPeer(userId))
                .flatMap(p -> client.getServiceHolder().getChatService()
                        .deleteChatUser(minData.id(), p, revokeHistory));
    }

    /**
     * Request to leave group chat.
     * Invoking this method is equivalent of following code:
     * {@code chat.deleteChatParticipant(client.getSelfId(), revokeHistory)}
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
        return client.getServiceHolder().getChatService()
                .editChatAbout(ImmutableInputPeerChat.of(minData.id()), newAbout);
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

        /** Whether the current user is the owner of the group. */
        OWNER(CREATOR_POS),

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
        HAS_SCHEDULED(HAS_SCHEDULED_POS),

        TRANSLATIONS_DISABLED(TRANSLATIONS_DISABLED_POS),

        // non-existent in chat object flags

        /** Whether current user can view list of participants. */
        CAN_VIEW_PARTICIPANTS((byte) 31);

        private final byte position;

        Flag(byte position) {
            this.position = position;
        }

        @Override
        public byte position() {
            return position;
        }

        /**
         * Computes {@link Set} of chat flags from given min and full data.
         *
         * @param fullData The full chat data.
         * @param minData The min chat data.
         * @return The {@link Set} of channel flags.
         */
        public static Set<Flag> of(@Nullable telegram4j.tl.BaseChatFull fullData, telegram4j.tl.BaseChat minData) {
            var minFlags = of(minData);
            if (fullData != null) {
                var set = EnumSet.range(CAN_SET_USERNAME, TRANSLATIONS_DISABLED);
                int flags = fullData.flags();
                set.removeIf(value -> (flags & value.mask()) == 0);
                if (fullData.participants().identifier() == BaseChatParticipants.ID) {
                    set.add(Flag.CAN_VIEW_PARTICIPANTS);
                }

                set.addAll(minFlags);
                return set;
            }

            return minFlags;
        }

        /**
         * Computes {@link Set} of chat flags from given min data.
         *
         * @param data The min chat data.
         * @return The {@link Set} of chat flags.
         */
        public static Set<Flag> of(telegram4j.tl.BaseChat data) {
            var set = EnumSet.range(OWNER, CALL_NOT_EMPTY);
            int flags = data.flags();
            set.removeIf(value -> (flags & value.mask()) == 0);
            return set;
        }
    }
}
