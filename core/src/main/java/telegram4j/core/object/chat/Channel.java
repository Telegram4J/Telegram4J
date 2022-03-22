package telegram4j.core.object.chat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.object.*;
import telegram4j.tl.ChannelParticipantsFilter;
import telegram4j.tl.InputGroupCall;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Interface for Telegram channel (supergroups, broadcast channels and gigagroups).
 *
 * @see <a href="https://core.telegram.org/api/channel">Telegram Channels</a>
 */
public interface Channel extends Chat {

    /**
     * Gets enum set of enabled channel flags.
     *
     * @return The {@link EnumSet} of enabled channel flags.
     */
    EnumSet<Flag> getFlags();

    /**
     * Gets username of channel, if present. This username can be used to retrieve channel
     * via {@link telegram4j.core.retriever.EntityRetriever#resolvePeer(PeerId)}
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
    Optional<EnumSet<ChatAdminRights>> getAdminRights();

    /**
     * Gets banned rights for users in the channel, if present.
     *
     * @see <a href="https://core.telegram.org/api/rights">Channel Rights</a>
     * @return The banned rights for users in the channel, if present.
     */
    Optional<ChatBannedRightsSettings> getBannedRights();

    /**
     * Gets default rights for users in the channel, if present.
     *
     * @see <a href="https://core.telegram.org/api/rights">Channel Rights</a>
     * @return The default rights for users in the channel, if present.
     */
    Optional<ChatBannedRightsSettings> getDefaultBannedRights();

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
     * @see <a href="https://core.telegram.org/api/config#suggestions>Channel Suggestions</a>
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
     * Gets id of peer for sending messages, if present
     * and if detailed information about channel is available.
     *
     * @return The id of peer for sending messages, if present
     * and if detailed information about channel is available.
     */
    Optional<Id> getDefaultSendAs();

    /**
     * Gets list of user ids, who requested to join recently, if present
     * and if detailed information about channel is available.
     *
     * @see <a href="https://core.telegram.org/api/invites#join-requests">Join Requests</a>
     * @return The list of user ids, who requested to join recently, if present
     * and if detailed information about channel is available.
     */
    Optional<List<Id>> getRecentRequesters();

    /**
     * Gets list of available unicode emojis, used as reactions, if present
     * and if detailed information about channel is available.
     *
     * @return The list of available unicode emojis, used as reactions, if present
     * and if detailed information about channel is available.
     */
    Optional<List<String>> getAvailableReactions();

    /**
     * Gets the latest <a href="https://core.telegram.org/api/updates">pts</a> for this channel, if present
     * and if detailed information about channel is available.
     *
     * @return The latest pts for this channel, if present
     * and if detailed information about channel is available.
     */
    Optional<Integer> getPts();

    /**
     * Gets maximal message id of read incoming messages, if present
     * and if detailed information about channel is available.
     *
     * @return The maximal id of read incoming messages, if present
     * and if detailed information about channel is available.
     */
    Optional<Integer> getReadInboxMaxId();

    /**
     * Gets maximal message id of read outgoing messages, if present
     * and if detailed information about channel is available.
     *
     * @return The maximal id of read outgoing messages, if present
     * and if detailed information about channel is available.
     */
    Optional<Integer> getReadOutboxMaxId();

    /**
     * Gets count of unread messages for <i>current</i> user, if present
     * and if detailed information about channel is available.
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
     * Gets count of current kicked participants, if present
     * and if detailed information about channel is available.
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
     * Gets id of linked (discussion>) channel, if present
     * and if detailed information about channel is available.
     *
     * @see <a href="https://core.telegram.org/api/discussion">Discussion Groups</a>
     * @return The id of linked channel, if present
     * and if detailed information about channel is available.
     */
    Optional<Id> getLinkedChatId();

    /**
     * Gets unicode emoji representation a specific channel theme.
     *
     * @return The unicode emoji representation a specific channel theme.
     */
    Optional<String> getThemeEmoticon();

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
     * @param limit The max count of participants to retrieve.
     * @return A {@link Flux} emitting channel participants with user data.
     */
    Flux<ChatParticipant> getParticipants(ChannelParticipantsFilter filter, int offset, int limit);

    /** Available channel flags. */
    enum Flag {
        // ChannelMin flags

        /** Whether the current user is the creator of this channel. */
        CREATOR(0),

        /** Whether the current user has left this channel. */
        LEFT(2),

        /** Is this channel verified by telegram?. */
        VERIFIED(7),

        /** Whether viewing/writing in this channel for a reason (see {@link BroadcastChannel#getRestrictionReason()}) */
        RESTRICTED(9),

        /** Whether signatures are enabled (channels). */
        SIGNATURES(11),

        /** This channel/supergroup is probably a scam. */
        SCAM(19),

        /** Whether this channel has a private join link. */
        HAS_LINK(20),

        /** Whether this channel has a geoposition. */
        HAS_GEO(21),

        /** Whether slow mode is enabled for groups to prevent flood in chat. */
        SLOWMODE_ENABLED(22),

        /** Whether a group call or livestream is currently active. */
        CALL_ACTIVE(23),

        /** Whether there's anyone in the group call or livestream. */
        CALL_NOT_EMPTY(24),

        /**
         * If set, this <a href="https://core.telegram.org/api/channel">supergroup/channel</a> was reported by many users
         * as a fake or scam: be careful when interacting with it.
         */
        FAKE(25),

        /** Whether this <a href="https://core.telegram.org/api/channel">supergroup</a> is a gigagroup. */
        GIGAGROUP(26),

        /** Whether this channel or group is protected, thus does not allow forwarding messages from it. */
        NOFORWARDS(27),

        // ChannelFull flags

        /** Can we view the participant list? */
        CAN_VIEW_PARTICIPANTS(3),

        /** Can we set the channel's username? */
        CAN_SET_USERNAME(6),

        /** Can we <a href="https://core.telegram.org/method/channels.setStickers">associate</a> a stickerpack to the supergroup? */
        CAN_SET_STICKERS(7),

        /** Is the history before we joined hidden to us? */
        HIDDEN_PREHISTORY(10),

        /** Can we set the geolocation of this group (for geogroups)? */
        CAN_SET_LOCATION(16),

        /** Whether scheduled messages are available. */
        HAS_SCHEDULED(19),

        /** Can the user view <a href="https://core.telegram.org/api/stats">channel/supergroup statistics</a>. */
        CAN_VIEW_STATS(20),

        /**
         * Whether any anonymous admin of this supergroup was blocked:
         * if set, you won't receive messages from anonymous group
         * admins in <a href="https://core.telegram.org/api/discussion">discussion replies via @replies</a>.
         */
        BLOCKED(22);

        private final int value;
        private final int flag;

        Flag(int value) {
            this.value = value;
            this.flag = 1 << value;
        }

        /**
         * Gets flag position, used in the {@link #getFlag()} as {@code 1 << position}.
         *
         * @return The flag shift position.
         */
        public int getValue() {
            return value;
        }

        /**
         * Gets bit-mask for flag.
         *
         * @return The bit-mask for flag.
         */
        public int getFlag() {
            return flag;
        }

        /**
         * Computes {@link EnumSet} with channel flags from given full and min data.
         *
         * @param fullData The full channel data, if present.
         * @param minData The min channel data.
         * @return The {@link EnumSet} with channel flags.
         */
        public static EnumSet<Flag> of(@Nullable telegram4j.tl.ChannelFull fullData, telegram4j.tl.Channel minData) {
            EnumSet<Flag> set = EnumSet.noneOf(Flag.class);
            if (fullData != null) {
                int flags = fullData.flags();
                for (Flag value : values()) {
                    if (value.ordinal() < CAN_VIEW_PARTICIPANTS.ordinal()) continue;
                    if ((flags & value.flag) != 0) {
                        set.add(value);
                    }
                }
            }

            set.addAll(of(minData));
            return set;
        }

        /**
         * Computes {@link EnumSet} with channel flags from given min data.
         *
         * @param data The min channel data.
         * @return The {@link EnumSet} with channel flags.
         */
        public static EnumSet<Flag> of(telegram4j.tl.Channel data) {
            EnumSet<Flag> set = EnumSet.noneOf(Flag.class);
            int flags = data.flags();
            for (Flag value : values()) {
                if (value.ordinal() >= CALL_NOT_EMPTY.ordinal()) continue;
                if ((flags & value.flag) != 0) {
                    set.add(value);
                }
            }
            return set;
        }
    }
}
