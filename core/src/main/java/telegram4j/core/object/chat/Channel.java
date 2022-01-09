package telegram4j.core.object.chat;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.BotInfo;
import telegram4j.core.object.ChannelLocation;
import telegram4j.core.object.ChatAdminRights;
import telegram4j.core.object.ChatPhoto;
import telegram4j.core.object.ExportedChatInvite;
import telegram4j.core.object.PeerNotifySettings;
import telegram4j.core.object.Photo;
import telegram4j.core.object.RestrictionReason;
import telegram4j.core.object.StickerSet;
import telegram4j.core.object.User;
import telegram4j.core.object.*;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class Channel extends BaseChat {

    private final telegram4j.tl.Channel minData;
    @Nullable
    private final telegram4j.tl.ChannelFull fullData;
    @Nullable
    private final List<Chat> chats;
    @Nullable
    private final List<User> users;

    public Channel(MTProtoTelegramClient client, telegram4j.tl.Channel minData) {
        super(client, Id.ofChannel(minData.id(), minData.accessHash()), Type.CHANNEL);
        this.minData = minData;
        this.fullData = null;
        this.chats = null;
        this.users = null;
    }

    public Channel(MTProtoTelegramClient client, telegram4j.tl.ChannelFull fullData, telegram4j.tl.Channel minData,
                   List<Chat> chats, List<User> users) {
        super(client, Id.ofChannel(minData.id(), minData.accessHash()), Type.CHANNEL);
        this.minData = Objects.requireNonNull(minData, "minData");
        this.fullData = Objects.requireNonNull(fullData, "fullData");
        this.chats = Collections.unmodifiableList(chats);
        this.users = Collections.unmodifiableList(users);
    }

    @Override
    public Optional<Integer> getPinnedMessageId() {
        return Optional.ofNullable(fullData).map(ChannelFull::pinnedMsgId);
    }

    @Override
    public Optional<ChatPhoto> getPhoto() {
        // TODO: implement
        Optional<ChatPhoto> fullChatPhoto = Optional.empty()/*Optional.ofNullable(fullData)
                .map(d -> TlEntityUtil.unmapEmpty(d.chatPhoto(), BasePhoto.class))
                .map(d -> new ChatPhoto(client, d))*/;

        Optional<ChatPhoto> minChatPhoto = Optional.of(minData)
                .map(d -> TlEntityUtil.unmapEmpty(d.photo(), BaseChatPhoto.class))
                .map(d -> new ChatPhoto(client, d));

        return fullChatPhoto.isPresent() ? fullChatPhoto : minChatPhoto;
    }

    @Override
    public Optional<Duration> getMessageAutoDeleteDuration() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::ttlPeriod)
                .map(Duration::ofSeconds);
    }

    // ChannelMin fields

    public EnumSet<Flag> getFlags() {
        return Flag.of(fullData, minData);
    }

    public String getTitle() {
        return minData.title();
    }

    public Optional<String> getUsername() {
        return Optional.ofNullable(minData.username());
    }

    public Instant getCreateTimestamp() {
        return Instant.ofEpochSecond(minData.date());
    }

    public Optional<List<RestrictionReason>> getRestrictionReason() {
        return Optional.ofNullable(minData.restrictionReason())
                .map(list -> list.stream()
                        .map(d -> new RestrictionReason(client, d))
                        .collect(Collectors.toList()));
    }

    public Optional<EnumSet<ChatAdminRights>> getAdminRights() {
        return Optional.ofNullable(minData.adminRights()).map(ChatAdminRights::of);
    }

    public Optional<ChatBannedRightsSettings> getBannedRights() {
        return Optional.ofNullable(minData.bannedRights()).map(d -> new ChatBannedRightsSettings(client, d));
    }

    public Optional<ChatBannedRightsSettings> getDefaultBannedRights() {
        return Optional.ofNullable(minData.defaultBannedRights()).map(d -> new ChatBannedRightsSettings(client, d));
    }

    public Optional<Integer> getParticipantsCount() {
        return Optional.ofNullable(minData.participantsCount());
    }

    // ChannelFull fields

    public Optional<String> getAbout() {
        return Optional.ofNullable(fullData).map(ChannelFull::about);
    }

    public Optional<Integer> getAdminsCount() {
        return Optional.ofNullable(fullData).map(ChannelFull::adminsCount);
    }

    public Optional<Integer> getKickedCount() {
        return Optional.ofNullable(fullData).map(ChannelFull::kickedCount);
    }

    public Optional<Integer> getBannedCount() {
        return Optional.ofNullable(fullData).map(ChannelFull::bannedCount);
    }

    public Optional<Integer> getOnlineCount() {
        return Optional.ofNullable(fullData).map(ChannelFull::onlineCount);
    }

    public Optional<Integer> getReadInboxMaxId() {
        return Optional.ofNullable(fullData).map(ChannelFull::readInboxMaxId);
    }

    public Optional<Integer> getReadOutboxMaxId() {
        return Optional.ofNullable(fullData).map(ChannelFull::readOutboxMaxId);
    }

    public Optional<Integer> getUnreadCount() {
        return Optional.ofNullable(fullData).map(ChannelFull::unreadCount);
    }

    public Optional<PeerNotifySettings> getNotifySettings() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::notifySettings)
                .map(d -> new PeerNotifySettings(client, d));
    }

    public Optional<ExportedChatInvite> getExportedInvite() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::exportedInvite)
                .map(d -> new ExportedChatInvite(client, d));
    }

    public Optional<List<BotInfo>> getBotInfo() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::botInfo)
                .map(list -> list.stream()
                        .map(d -> new BotInfo(client, d))
                        .collect(Collectors.toList()));
    }

    public Optional<Id> getMigratedFromChatId() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::migratedFromChatId)
                .map(Id::ofChat);
    }

    public Optional<Integer> getMigratedFromMaxId() {
        return Optional.ofNullable(fullData).map(ChannelFull::migratedFromMaxId);
    }

    public Optional<StickerSet> getStickerSet() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::stickerset)
                .map(d -> new StickerSet(client, d));
    }

    public Optional<Integer> getAvailableMinId() {
        return Optional.ofNullable(fullData).map(ChannelFull::availableMinId);
    }

    public Optional<Integer> getFolderId() {
        return Optional.ofNullable(fullData).map(ChannelFull::folderId);
    }

    public Optional<Id> getLinkedChatId() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::linkedChatId)
                .map(Id::ofChat);
    }

    public Optional<ChannelLocation> getLocation() {
        return Optional.ofNullable(fullData)
                .map(d -> TlEntityUtil.unmapEmpty(d.location(), BaseChannelLocation.class))
                .map(d -> new ChannelLocation(client, d));
    }

    public Optional<Duration> getSlowmodeDuration() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::slowmodeSeconds)
                .map(Duration::ofSeconds);
    }

    public Optional<Instant> getSlowmodeNextSendTimestamp() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::slowmodeNextSendDate)
                .map(Instant::ofEpochSecond);
    }

    public Optional<Integer> getStatsDcId() {
        return Optional.ofNullable(fullData).map(ChannelFull::statsDc);
    }

    public Optional<Integer> getPts() {
        return Optional.ofNullable(fullData).map(ChannelFull::pts);
    }

    public Optional<InputGroupCall> getCall() {
        return Optional.ofNullable(fullData).map(ChannelFull::call);
    }

    public Optional<List<String>> getPendingSuggestions() {
        return Optional.ofNullable(fullData).map(ChannelFull::pendingSuggestions);
    }

    public Optional<Peer> getGroupCallDefaultJoinAs() {
        return Optional.ofNullable(fullData).map(ChannelFull::groupcallDefaultJoinAs);
    }

    public Optional<String> getThemeEmoticon() {
        return Optional.ofNullable(fullData).map(ChannelFull::themeEmoticon);
    }

    // Auxiliary contacts fields

    public Optional<List<User>> getUsers() {
        return Optional.ofNullable(users);
    }

    public Optional<List<Chat>> getChats() {
        return Optional.ofNullable(chats);
    }

    public enum Flag {
        // ChannelMin flags

        /** Whether the current user is the creator of this channel. */
        CREATOR(0),

        /** Whether the current user has left this channel. */
        LEFT(2),

        /** Is this a channel? */
        BROADCAST(5),

        /** Is this channel verified by telegram?. */
        VERIFIED(7),

        /** Is this a supergroup? */
        MEGAGROUP(8),

        /** Whether viewing/writing in this channel for a reason (see {@link Channel#getRestrictionReason()}) */
        RESTRICTED(9),

        /** Whether signatures are enabled (channels). */
        SIGNATURES(11),

        /** See <a href="https://core.telegram.org/api/min">min</a> */
        MIN(12),

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

        /** Can we vew the participant list? */
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
