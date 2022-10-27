package telegram4j.core.object.chat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.object.BotInfo;
import telegram4j.core.object.ExportedChatInvite;
import telegram4j.core.object.Reaction;
import telegram4j.core.object.StickerSet;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.retriever.EntityRetriever;
import telegram4j.core.util.BitFlag;
import telegram4j.core.util.Id;
import telegram4j.core.util.PeerId;
import telegram4j.core.util.Variant2;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.*;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static telegram4j.tl.Channel.*;
import static telegram4j.tl.ChannelFull.*;

/**
 * Interface for Telegram channel (supergroups, broadcast channels and gigagroups).
 *
 * @see <a href="https://core.telegram.org/api/channel">Telegram Channels</a>
 */
public interface Channel extends Chat {

    /**
     * Gets enum set of enabled channel flags.
     *
     * @return The {@link Set} of enabled channel flags.
     */
    Set<Flag> getFlags();

    /**
     * Gets username of channel, if present. This username can be used to retrieve channel
     * via {@link EntityRetriever#resolvePeer(PeerId)}
     * or used in {@link PeerId peer id}.
     *
     * @return The username of channel, if present.
     */
    Optional<String> getUsername();

    /**
     * Gets list of information about chat bots, if present
     * and if detailed information about channel is available.
     *
     * @return The list of information about chat bots, if present
     * and if detailed information about channel is available.
     */
    Optional<List<BotInfo>> getBotInfo();

    /**
     * Gets timestamp when <i>current</i> user joined, or if the user isn't a member, channel created.
     *
     * @return The timestamp when <i>current</i> user joined, or if the user isn't a member, channel created.
     */
    Instant getCreateTimestamp();

    /**
     * Gets associated with this channel sticker set, if present
     * and if detailed information about channel is available.
     *
     * @return The associated sticker set, if present
     * and if detailed information about channel is available.
     */
    Optional<StickerSet> getStickerSet();

    /**
     * Gets list of reasons for why access to this channel must be restricted, if present.
     *
     * @return The list of reasons for why access to this channel must be restricted, if present.
     */
    Optional<List<RestrictionReason>> getRestrictionReason();

    /**
     * Gets current participants count, if present
     * and if detailed information about channel is available.
     *
     * @return The current participants count, if present
     * and if detailed information about channel is available.
     */
    Optional<Integer> getParticipantsCount();

    /**
     * Gets admin rights for admins in the channel, if present.
     *
     * @see <a href="https://core.telegram.org/api/rights">Channel Rights</a>
     * @return The admin rights for admins in the channel, if present.
     */
    Optional<Set<AdminRight>> getAdminRights();

    /**
     * Gets banned rights for users in the channel, if present.
     *
     * @see <a href="https://core.telegram.org/api/rights">Channel Rights</a>
     * @return The banned rights for users in the channel, if present.
     */
    Optional<ChatRestrictions> getRestrictions();

    /**
     * Gets default settings with disallowed rights for users in the channel, if present.
     *
     * @see <a href="https://core.telegram.org/api/rights">Channel Rights</a>
     * @return The settings with disallowed rights for users in the channel, if present.
     */
    Optional<ChatRestrictions> getDefaultRestrictions();

    /**
     * Get minimal id of available (not hidden by invite) message, if present
     * and if detailed information about channel is available.
     *
     * @return The minimal id of available message, if present
     * and if detailed information about channel is available.
     */
    Optional<Integer> getAvailableMinId();

    /**
     * Gets list of pending api suggestions for channel, if present
     * and if detailed information about channel is available.
     *
     * @see <a href="https://core.telegram.org/api/config#suggestions">Channel Suggestions</a>
     * @return The list of pending api suggestions for channel, if present
     * and if detailed information about channel is available.
     */
    Optional<List<String>> getPendingSuggestions();

    /**
     * Gets count of pending join requests, if present
     * and if detailed information about channel is available.
     *
     * @see <a href="https://core.telegram.org/api/invites#join-requests">Join Requests</a>
     * @return The count of pending join requests, if present
     * and if detailed information about channel is available.
     */
    Optional<Integer> getRequestsPending();

    /**
     * Gets id of peer for sending messages, if present.
     *
     * @return The id of peer for sending messages, if present
     * and if detailed information about channel is available.
     */
    Optional<Id> getDefaultSendAs();

    /**
     * Gets list of user ids, who requested to join recently, if present.
     *
     * @see <a href="https://core.telegram.org/api/invites#join-requests">Join Requests</a>
     * @return The list of user ids, who requested to join recently, if present
     * and if detailed information about channel is available.
     */
    Optional<List<Id>> getRecentRequesters();

    /**
     * Gets {@code boolean} which indicates the availability of any emojis in the channel
     * or list of available reactions, if present.
     *
     * @return The {@link Variant2} with {@code boolean} which indicates the availability of any emojis in the channel
     * or list of available reactions, if present
     * and if detailed information about channel is available.
     */
    Optional<Variant2<Boolean, List<Reaction>>> getAvailableReactions();

    /**
     * Gets the latest <a href="https://core.telegram.org/api/updates">pts</a> for this channel, if present.
     *
     * @return The latest pts for this channel, if present
     * and if detailed information about channel is available.
     */
    Optional<Integer> getPts();

    /**
     * Gets maximal message id of read incoming messages, if present.
     *
     * @return The maximal id of read incoming messages, if present
     * and if detailed information about channel is available.
     */
    Optional<Integer> getReadInboxMaxId();

    /**
     * Gets maximal message id of read outgoing messages, if present.
     *
     * @return The maximal id of read outgoing messages, if present
     * and if detailed information about channel is available.
     */
    Optional<Integer> getReadOutboxMaxId();

    /**
     * Gets count of unread messages for <i>current</i> user, if present.
     *
     * @return The count of unread messages for <i>current</i> user, if present
     * and if detailed information about channel is available.
     */
    Optional<Integer> getUnreadCount();

    /**
     * Gets count of channel admins, if present
     * and if detailed information about channel is available.
     *
     * @return The count of channel admins, if present
     * and if detailed information about channel is available.
     */
    Optional<Integer> getAdminsCount();

    /**
     * Gets count of current kicked participants, if present.
     *
     * @return The count of current kicked participates, if present
     * and if detailed information about channel is available.
     */
    Optional<Integer> getKickedCount();

    /**
     * Gets count of current banned participants, if present
     * and if detailed information about channel is available.
     *
     * @return The count of current banned participates, if present
     * and if detailed information about channel is available.
     */
    Optional<Integer> getBannedCount();

    /**
     * Gets count of current online participants, if present
     * and if detailed information about channel is available.
     *
     * @return The count of current online participants, if present
     * and if detailed information about channel is available.
     */
    Optional<Integer> getOnlineCount();

    /**
     * Gets id of linked (discussion or posting) channel, if present.
     *
     * @see <a href="https://core.telegram.org/api/discussion">Discussion Groups</a>
     * @return The id of linked channel, if present
     * and if detailed information about channel is available.
     */
    Optional<Id> getLinkedChannelId();

    /**
     * Requests to retrieve linked channel.
     *
     * @return An {@link Mono} emitting on successful completion the {@link Channel channel}.
     */
    default Mono<Channel> getLinkedChannel() {
        return getLinkedChannel(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve linked channel using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @return An {@link Mono} emitting on successful completion the {@link Channel channel}.
     */
    Mono<Channel> getLinkedChannel(EntityRetrievalStrategy strategy);

    /**
     * Gets current group call/livestream in the channel, if present
     * and if detailed information about channel is available.
     *
     * @return The current group call/livestream in the channel, if present
     * and if detailed information about channel is available.
     */
    Optional<InputGroupCall> getCall();

    /**
     * Gets id of peer, that selects by default on group call, if present
     * and if detailed information about channel is available.
     *
     * @return The id of peer, that selects by default on group call, if present
     * and if detailed information about channel is available.
     */
    Optional<Id> getGroupCallDefaultJoinAs();

    /**
     * Gets current invite link of channel, if present
     * and if detailed information about channel is available.
     *
     * @return The {@link ExportedChatInvite} of channel, if present
     * and if detailed information about channel is available.
     */
    Optional<ExportedChatInvite> getExportedInvite();

    /**
     * Gets id of DC for retrieving channel stats, if present
     * and if detailed information about channel is available.
     *
     * @return The id of DC for retrieving channel stats, if present
     * and if detailed information about channel is available.
     */
    Optional<Integer> getStatsDcId();

    // Interaction methods

    /**
     * Requests to edit current channel title.
     *
     * @param newTitle A new title for channel.
     * @return A {@link Mono} emitting on successful completion nothing.
     */
    Mono<Void> editTitle(String newTitle);

    /**
     * Requests to edit admin rights for specified user.
     *
     * @param userId The id of user to edit.
     * @param rights The {@link Iterable} with allowed admin rights.
     * @param rank The new display rank.
     * @return A {@link Mono} emitting on successful completion updated channel.
     */
    Mono<Channel> editAdmin(Id userId, Iterable<AdminRight> rights, String rank);

    /**
     * Requests to edit banned rights for specified peer.
     *
     * @param peerId The id of user/channel to edit.
     * @param rights The {@link Iterable} with disallowed rights.
     * @param untilTimestamp The timestamp before which this overwrite active, if absent - forever active.
     * @return A {@link Mono} emitting on successful completion updated channel.
     */
    Mono<Channel> editBanned(Id peerId, Iterable<ChatRestrictions.Right> rights, @Nullable Instant untilTimestamp);

    /**
     * Requests to edit current channel photo.
     *
     * @see FileReferenceId#asInputPhoto()
     * @param photo A new photo for channel, {@code null} value indicates removing.
     * @return A {@link Mono} emitting on successful completion nothing.
     */
    Mono<Void> editPhoto(@Nullable BaseInputPhoto photo);

    /**
     * Requests to edit current channel photo.
     *
     * @param spec A new uploaded photo for channel, {@code null} value indicates removing.
     * @return A {@link Mono} emitting on successful completion nothing.
     */
    Mono<Void> editPhoto(@Nullable InputChatUploadedPhoto spec);

    /**
     * Requests to leave channel by <i>current</i> user.
     *
     * @return A {@link Mono} emitting on successful completion nothing.
     */
    Mono<Void> leave();

    /**
     * Requests to associate new stickerset with this channel.
     *
     * @param stickerSetId The id of sticker set to associate.
     * @return A {@link Mono} emitting on successful completion boolean, indicates result.
     */
    Mono<Boolean> setStickers(InputStickerSet stickerSetId);

    /**
     * Retrieve channel participant by user id.
     *
     * @param participantId The id of user.
     * @return A {@link Mono} emitting on successful completion channel participant with user data.
     */
    Mono<ChatParticipant> getParticipant(Id participantId);

    /**
     * Retrieve and paginate channel participants by filter.
     *
     * @param filter The filter to retrieve channel participants.
     * @param offset The amount of participants to ignore.
     * @param limit The max count of participants to retrieve, must not exceed 200.
     * @return A {@link Flux} emitting channel participants with user data.
     */
    Flux<ChatParticipant> getParticipants(ChannelParticipantsFilter filter, int offset, int limit);

    /**
     * Requests to edit channel description.
     *
     * @param newAbout The new description to set.
     * @return A {@link Mono} emitting on successful completion completion status.
     */
    Mono<Boolean> editAbout(String newAbout);

    /** Available channel flags. */
    enum Flag implements BitFlag {
        // ChannelMin flags

        /** Whether the current user is the creator of this channel. */
        CREATOR(CREATOR_POS),

        /** Whether the current user has left this channel. */
        LEFT(LEFT_POS),

        /** Is this channel verified by telegram? */
        VERIFIED(VERIFIED_POS),

        /** Whether viewing/writing in this channel for a reason (see {@link Channel#getRestrictionReason()}) */
        RESTRICTED(RESTRICTED_POS),

        /** Whether signatures are enabled (channels). */
        SIGNATURES(SIGNATURES_POS),

        MIN(MIN_POS),

        /** This channel/supergroup is probably a scam. */
        SCAM(CREATOR_POS),

        /** Whether this channel has a private join link. */
        HAS_LINK(HAS_LINK_POS),

        /** Whether this channel has a geo position. */
        HAS_GEO(HAS_GEO_POS),

        /** Whether slow mode is enabled for groups to prevent flood in chat. */
        SLOWMODE_ENABLED(SLOWMODE_ENABLED_POS),

        /** Whether a group call or livestream is currently active. */
        CALL_ACTIVE(CALL_ACTIVE_POS),

        /** Whether there's anyone in the group call or livestream. */
        CALL_NOT_EMPTY(CALL_NOT_EMPTY_POS),

        /**
         * If set, this supergroup/channel was reported by many users
         * as a fake or scam: be careful when interacting with it.
         */
        FAKE(FAKE_POS),

        /** Whether this supergroup is a gigagroup. */
        GIGAGROUP(GIGAGROUP_POS),

        /** Whether this channel or group is protected, thus does not allow forwarding messages from it. */
        NO_FORWARDS(NOFORWARDS_POS),

        JOIN_TO_SEND(JOIN_TO_SEND_POS),

        JOIN_REQUEST(JOIN_REQUEST_POS),

        // ChannelFull flags

        /** Can we view the participant list? */
        CAN_VIEW_PARTICIPANTS(CAN_VIEW_PARTICIPANTS_POS),

        /** Can we set the channel's username? */
        CAN_SET_USERNAME(CAN_SET_USERNAME_POS),

        /** Can we associate a stickerpack to the supergroup via {@link Channel#setStickers(InputStickerSet)}? */
        CAN_SET_STICKERS(CAN_SET_STICKERS_POS),

        /** Is the history before we joined hidden to us? */
        HIDDEN_PREHISTORY(HIDDEN_PREHISTORY_POS),

        /** Can we set the geolocation of this group (for geogroups)? */
        CAN_SET_LOCATION(CAN_SET_LOCATION_POS),

        /** Whether scheduled messages are available. */
        HAS_SCHEDULED(HAS_SCHEDULED_POS),

        /** Can the user view <a href="https://core.telegram.org/api/stats">channel/supergroup statistics</a>. */
        CAN_VIEW_STATS(CAN_VIEW_STATS_POS),

        /**
         * Whether any anonymous admin of this supergroup was blocked:
         * if set, you won't receive messages from anonymous group
         * admins in <a href="https://core.telegram.org/api/discussion">discussion replies via @replies</a>.
         */
        BLOCKED(BLOCKED_POS),

        // ChannelFull flags2

        CAN_DELETE_CHANNEL(CAN_DELETE_CHANNEL_POS);

        private final byte position;

        Flag(byte position) {
            this.position = position;
        }

        @Override
        public byte position() {
            return position;
        }

        /**
         * Computes {@link EnumSet} with channel flags from given full and min data.
         *
         * @param fullData The full channel data, if present.
         * @param minData The min channel data.
         * @return The {@link EnumSet} with channel flags.
         */
        public static Set<Flag> of(@Nullable telegram4j.tl.ChannelFull fullData, telegram4j.tl.Channel minData) {
            var minFlags = of(minData);
            if (fullData != null) {
                var set = EnumSet.allOf(Flag.class);

                int flags = fullData.flags();
                // well done, telegram, good solution
                int flags2 = fullData.flags2();

                set.removeIf(value -> value.ordinal() < CAN_VIEW_PARTICIPANTS.ordinal() ||
                        value.ordinal() >= CAN_DELETE_CHANNEL.ordinal() && (flags2 & value.mask()) == 0 ||
                        (flags & value.mask()) == 0);

                set.addAll(minFlags);
                return set;
            }

            return minFlags;
        }

        /**
         * Computes {@link EnumSet} with channel flags from given min data.
         *
         * @param data The min channel data.
         * @return The {@link EnumSet} with channel flags.
         */
        public static Set<Flag> of(telegram4j.tl.Channel data) {
            var set = EnumSet.allOf(Flag.class);
            int flags = data.flags();
            set.removeIf(value -> value.ordinal() >= CAN_VIEW_PARTICIPANTS.ordinal() || (flags & value.mask()) == 0);
            return set;
        }
    }
}
