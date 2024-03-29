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
package telegram4j.core.object.chat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.object.BotInfo;
import telegram4j.core.object.MentionablePeer;
import telegram4j.core.object.User;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.util.BitFlag;
import telegram4j.core.util.Id;
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
public sealed interface Channel extends MentionablePeer, ChannelPeer
        permits BaseChannel, SupergroupChat, BroadcastChannel {

    /**
     * Gets enum set of enabled channel flags.
     *
     * @return The {@link Set} of enabled channel flags.
     */
    Set<Flag> getFlags();

    @Override
    Optional<String> getUsername();

    @Override
    List<Username> getUsernames();

    /**
     * Gets timestamp when <i>current</i> user joined, or if the user isn't a member, channel created.
     *
     * @return The timestamp when <i>current</i> user joined, or if the user isn't a member, channel created.
     */
    Instant getCreateTimestamp();

    /**
     * Gets list of reasons for why access to this channel must be restricted, if present.
     *
     * @return The list of reasons for why access to this channel must be restricted, if present.
     */
    @Override
    List<RestrictionReason> getRestrictionReasons();

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
     * Requests to retrieve the oldest available message.
     *
     * @return An {@link Mono} emitting on successful completion the {@link AuxiliaryMessages message container}.
     */
    default Mono<AuxiliaryMessages> getAvailableMinMessage() {
        return getAvailableMinMessage(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve the oldest available message using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @return An {@link Mono} emitting on successful completion the {@link AuxiliaryMessages message container}.
     */
    Mono<AuxiliaryMessages> getAvailableMinMessage(EntityRetrievalStrategy strategy);

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
    Optional<Integer> getPendingRequests();

    /**
     * Gets id of peer for sending messages, if present.
     *
     * @return The id of peer for sending messages, if present
     * and if detailed information about channel is available.
     */
    Optional<Id> getDefaultSendAs();

    /**
     * Gets mutable set of user ids, who requested to join recently, if present.
     *
     * @see <a href="https://core.telegram.org/api/invites#join-requests">Join Requests</a>
     * @return The set of user ids, who requested to join recently, if present
     * and if detailed information about channel is available.
     */
    Optional<Set<Id>> getRecentRequestersIds();

    /**
     * Requests to retrieve the user who requested to join.
     *
     * @see #getRecentRequestersIds()
     * @return A {@link Flux} which emits the {@link User requesters}.
     */
    default Flux<User> getRecentRequesters() {
        return getRecentRequesters(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve the user who requested to join using specified retrieval strategy.
     *
     * @see #getRecentRequestersIds()
     * @param strategy The strategy to apply.
     * @return A {@link Flux} which emits the {@link User requesters}.
     */
    Flux<User> getRecentRequesters(EntityRetrievalStrategy strategy);

    /**
     * Gets settings for allowed emojis in the channel.
     *
     * @return The settings for allowed emojis in the channel, if present
     * and full information about group channel available.
     */
    Optional<ChatReactions> getAvailableReactions();

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
    default Mono<? extends Channel> getLinkedChannel() {
        return getLinkedChannel(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve linked channel using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @return An {@link Mono} emitting on successful completion the {@link Channel channel}.
     */
    Mono<? extends Channel> getLinkedChannel(EntityRetrievalStrategy strategy);

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
    Optional<Id> getGroupCallDefaultJoinAsId();

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

    /**
     * Gets list of information about chat bots, if present
     * and if detailed information about channel is available.
     *
     * @return The list of information about chat bots, if present
     * and if detailed information about channel is available.
     */
    Optional<List<BotInfo>> getBotInfo();

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
     * @param spec A new spec for channel photo, {@code null} value indicates removing.
     * @return A {@link Mono} emitting on successful completion nothing.
     */
    Mono<Void> editPhoto(@Nullable BaseInputPhoto spec);

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
     * Retrieve channel participant by user id.
     *
     * @param peerId The id of peer.
     * @return A {@link Mono} emitting on successful completion channel participant with user data.
     */
    default Mono<ChatParticipant> getParticipantById(Id peerId) {
        return getParticipantById(MappingUtil.IDENTITY_RETRIEVER, peerId);
    }

    /**
     * Retrieve channel participant by user id using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @param peerId The id of peer.
     * @return A {@link Mono} emitting on successful completion channel participant with user data.
     */
    Mono<ChatParticipant> getParticipantById(EntityRetrievalStrategy strategy, Id peerId);

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
        // This enum contains more than 32 constants and
        // BitFlag used only for reading flags from channel data

        // ChannelMin flags

        /** Whether the current user is the owner of this channel. */
        OWNER(CREATOR_POS),

        /** Whether the current user has left this channel. */
        LEFT(LEFT_POS),

        /** Is this channel verified by telegram? */
        VERIFIED(VERIFIED_POS),

        /** Whether signatures are enabled for channel posts. */
        SIGNATURES(SIGNATURES_POS),

        MIN(MIN_POS),

        /** Whether this channel is probably a scam. */
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

        /** Whether this channel is marked as a fake. */
        FAKE(FAKE_POS),

        /** Whether this supergroup is a gigagroup. */
        GIGAGROUP(GIGAGROUP_POS),

        /** Whether this channel or group is protected, thus does not allow forwarding messages from it. */
        NO_FORWARDS(NOFORWARDS_POS),

        /** Whether user must join to the discussion group to comment channel posts. */
        JOIN_TO_SEND_MESSAGE(JOIN_TO_SEND_POS),

        /** Whether a user's join request will have to be approved by admins of channel. */
        JOIN_REQUEST(JOIN_REQUEST_POS),

        // ChannelFull flags

        /** Can we view the participant list? */
        CAN_VIEW_PARTICIPANTS(CAN_VIEW_PARTICIPANTS_POS),

        /** Can we set the channel's username? */
        CAN_SET_USERNAME(CAN_SET_USERNAME_POS),

        /** Can we associate a stickerpack to the supergroup via {@link SupergroupChat#setStickerSet(InputStickerSet)}? */
        CAN_SET_STICKERS(CAN_SET_STICKERS_POS),

        /** Is the history before we joined hidden to us? */
        HIDDEN_PREHISTORY(HIDDEN_PREHISTORY_POS),

        /** Can we set the geolocation of this supergroup? */
        CAN_SET_LOCATION(CAN_SET_LOCATION_POS),

        /** Whether scheduled messages are available. */
        HAS_SCHEDULED(HAS_SCHEDULED_POS),

        /** Whether can the user view <a href="https://core.telegram.org/api/stats">channel statistics</a>. */
        CAN_VIEW_STATS(CAN_VIEW_STATS_POS),

        /**
         * Whether any anonymous admin of this supergroup was blocked:
         * if set, you won't receive messages from anonymous group
         * admins in <a href="https://core.telegram.org/api/discussion">discussion replies via @replies</a>.
         */
        BLOCKED(BLOCKED_POS),

        FORUM(FORUM_POS),

        // ChannelFull flags2

        /** Can we delete this channel? */
        CAN_DELETE_CHANNEL(CAN_DELETE_CHANNEL_POS),

        ANTISPAM(ANTISPAM_POS),

        PARTICIPANTS_HIDDEN(PARTICIPANTS_HIDDEN_POS),

        TRANSLATIONS_DISABLED(TRANSLATIONS_DISABLED_POS);

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
                var set1 = EnumSet.range(CAN_VIEW_PARTICIPANTS, FORUM);
                int flags1 = fullData.flags();
                set1.removeIf(value -> (flags1 & value.mask()) == 0);

                var set2 = EnumSet.range(CAN_DELETE_CHANNEL, TRANSLATIONS_DISABLED);
                int flags2 = fullData.flags2();
                set1.removeIf(value -> (flags2 & value.mask()) == 0);

                minFlags.addAll(set1);
                minFlags.addAll(set2);
                return set2;
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
            var set = EnumSet.range(OWNER, JOIN_REQUEST);
            int flags = data.flags();
            set.removeIf(value -> (flags & value.mask()) == 0);
            return set;
        }
    }
}
