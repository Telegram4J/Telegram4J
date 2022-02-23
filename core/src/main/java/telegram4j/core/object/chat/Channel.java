package telegram4j.core.object.chat;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.object.*;
import telegram4j.tl.InputGroupCall;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public interface Channel extends Chat {

    /**
     * Gets enum set of enabled channel flags.
     *
     * @return The {@link EnumSet} of enabled channel flags.
     */
    EnumSet<Flag> getFlags();

    /**
     * Gets title (i.e. name) of channel.
     *
     * @return The name of channel.
     */
    String getTitle();

    /**
     * Gets username of channel, if present. This username can be used to retrieve channel
     * via {@link telegram4j.core.retriever.EntityRetriever#resolvePeer(PeerId)}
     * or used in {@link PeerId peer id}.
     *
     * @return The username of channel, if present.
     */
    Optional<String> getUsername();

    /**
     * Gets list of information about chat bots, if present.
     *
     * @return The list of information about chat bots, if present.
     */
    Optional<List<BotInfo>> getBotInfo();

    /**
     * Gets timestamp when <i>current</i> user joined, or if the user isn't a member, channel created.
     *
     * @return The timestamp when <i>current</i> user joined, or if the user isn't a member, channel created.
     */
    Instant getCreateTimestamp();

    /**
     * Gets associated with this channel sticker set, if present.
     *
     * @return The associated sticker set, if present.
     */
    Optional<StickerSet> getStickerSet();

    Optional<List<RestrictionReason>> getRestrictionReason();

    Optional<Integer> getParticipantsCount();

    Optional<EnumSet<ChatAdminRights>> getAdminRights();

    Optional<ChatBannedRightsSettings> getBannedRights();

    Optional<ChatBannedRightsSettings> getDefaultBannedRights();

    Optional<Integer> getAvailableMinId();

    Optional<Integer> getFolderId();

    Optional<List<String>> getPendingSuggestions();

    Optional<Integer> getRequestsPending();

    Optional<Id> getDefaultSendAs();

    Optional<List<Id>> getRecentRequesters();

    Optional<List<String>> getAvailableReactions();

    Optional<Integer> getPts();

    Optional<Integer> getReadInboxMaxId();

    Optional<Integer> getReadOutboxMaxId();

    Optional<Integer> getUnreadCount();

    Optional<Integer> getAdminsCount();

    Optional<Integer> getKickedCount();

    Optional<Integer> getBannedCount();

    Optional<Integer> getOnlineCount();

    Optional<Id> getLinkedChatId();

    Optional<String> getThemeEmoticon();

    Optional<InputGroupCall> getCall();

    Optional<Id> getGroupCallDefaultJoinAs();

    Optional<ExportedChatInvite> getExportedInvite();

    Optional<Integer> getStatsDcId();

    // Interaction methods

    Mono<ChatParticipant> getParticipant(Id participantId);

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

        // TODO: not documented flag
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

        public int getValue() {
            return value;
        }

        public int getFlag() {
            return flag;
        }

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
