package telegram4j.core.object.chat;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.BotInfo;
import telegram4j.core.object.ChatAdminRights;
import telegram4j.core.object.ChatPhoto;
import telegram4j.core.object.Photo;
import telegram4j.core.object.*;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.ExportedChatInvite;
import telegram4j.tl.PeerNotifySettings;
import telegram4j.tl.*;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** Represents a basic group of 0-200 users (must be upgraded to a supergroup to accommodate more than 200 users). */
public class GroupChat extends BaseChat {

    private final telegram4j.tl.BaseChat minData;
    @Nullable
    private final telegram4j.tl.BaseChatFull fullData;

    public GroupChat(MTProtoTelegramClient client, telegram4j.tl.BaseChat minData) {
        super(client, Id.ofChat(minData.id()), Type.GROUP);
        this.minData = Objects.requireNonNull(minData, "minData");
        this.fullData = null;
    }

    public GroupChat(MTProtoTelegramClient client, telegram4j.tl.BaseChatFull fullData, telegram4j.tl.BaseChat minData) {
        super(client, Id.ofChat(minData.id()), Type.GROUP);
        this.minData = Objects.requireNonNull(minData, "minData");
        this.fullData = Objects.requireNonNull(fullData, "fullData");
    }

    @Override
    public Optional<ChatPhoto> getMinPhoto() {
        return Optional.ofNullable(TlEntityUtil.unmapEmpty(minData.photo(), BaseChatPhoto.class))
                .map(d -> new ChatPhoto(client, d, getIdAsPeer(), -1));
    }

    @Override
    public Optional<Photo> getPhoto() {
        return Optional.ofNullable(fullData)
                .map(d -> TlEntityUtil.unmapEmpty(d.chatPhoto(), BasePhoto.class))
                .map(d -> new Photo(client, d, ImmutableInputPeerChat.of(minData.id()), -1));
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

    // ChatMin fields

    public Optional<Id> getMigratedToChannelId() {
        return Optional.ofNullable(minData.migratedTo())
                .map(c -> (BaseInputChannel) c)
                .map(c -> Id.ofChannel(c.channelId(), c.accessHash()));
    }

    public String getTitle() {
        return minData.title();
    }

    public int getParticipantsCount() {
        return minData.participantsCount();
    }

    public Instant getCreateTimestamp() {
        return Instant.ofEpochSecond(minData.date());
    }

    public int getVersion() {
        return minData.version();
    }

    public Optional<EnumSet<ChatAdminRights>> getAdminRights() {
        return Optional.ofNullable(minData.adminRights()).map(ChatAdminRights::of);
    }

    public Optional<ChatBannedRightsSettings> getDefaultBannedRights() {
        return Optional.ofNullable(minData.defaultBannedRights()).map(data -> new ChatBannedRightsSettings(client, data));
    }

    public EnumSet<Flag> getFlags() {
        return Flag.of(fullData, minData);
    }

    // BaseChatFull fields

    public Optional<String> getAbout() {
        return Optional.ofNullable(fullData).map(BaseChatFull::about);
    }

    public Optional<ChatParticipants> getParticipants() {
        return Optional.ofNullable(fullData).map(BaseChatFull::participants);
    }

    public Optional<PeerNotifySettings> getNotifySettings() {
        return Optional.ofNullable(fullData).map(BaseChatFull::notifySettings);
    }

    public Optional<ExportedChatInvite> getExportedInvite() {
        return Optional.ofNullable(fullData).map(BaseChatFull::exportedInvite);
    }

    public Optional<List<BotInfo>> getBotInfo() {
        return Optional.ofNullable(fullData)
                .map(BaseChatFull::botInfo)
                .map(list -> list.stream()
                        .map(d -> new BotInfo(client, d))
                        .collect(Collectors.toList()));
    }

    public Optional<Integer> getFolderId() {
        return Optional.ofNullable(fullData).map(BaseChatFull::folderId);
    }

    public Optional<InputGroupCall> getCall() {
        return Optional.ofNullable(fullData).map(BaseChatFull::call);
    }

    public Optional<Id> getGroupCallDefaultJoinAs() {
        return Optional.ofNullable(fullData)
                .map(BaseChatFull::groupcallDefaultJoinAs)
                .map(Id::of);
    }

    public Optional<String> getThemeEmoticon() {
        return Optional.ofNullable(fullData).map(BaseChatFull::themeEmoticon);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupChat groupChat = (GroupChat) o;
        return minData.equals(groupChat.minData) && Objects.equals(fullData, groupChat.fullData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minData, fullData);
    }

    @Override
    public String toString() {
        return "GroupChat{" +
                "minData=" + minData +
                ", fullData=" + fullData +
                '}';
    }

    public enum Flag {
        // MinChat flags

        /** Whether the current user is the creator of the group. */
        CREATOR(0),

        /** Whether the current user was kicked from the group. */
        KICKED(1),

        /** Whether the current user has left the group. */
        LEFT(2),

        /** Whether the group was <a href="https://core.telegram.org/api/channel">migrated</a>. */
        DEACTIVATED(5),

        /** Whether a group call is currently active. */
        CALL_ACTIVE(23),

        /** Whether there's anyone in the group call. */
        CALL_NOT_EMPTY(24),

        // FullChat flags

        /** Can we change the username of this chat? */
        CAN_SET_USERNAME(7),

        /** Whether <a href="https://core.telegram.org/api/scheduled-messages">scheduled messages</a> are available. */
        HAS_SCHEDULED(8);

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

        public static EnumSet<Flag> of(@Nullable telegram4j.tl.BaseChatFull fullData, telegram4j.tl.BaseChat minData) {
            EnumSet<Flag> set = EnumSet.noneOf(Flag.class);
            if (fullData != null) {
                int flags = fullData.flags();
                for (Flag value : values()) {
                    if (value != CAN_SET_USERNAME && value != HAS_SCHEDULED) continue;
                    if ((flags & value.flag) != 0) {
                        set.add(value);
                    }
                }
            }

            set.addAll(of(minData));

            return set;
        }

        public static EnumSet<Flag> of(telegram4j.tl.Chat data) {
            EnumSet<Flag> set = EnumSet.noneOf(Flag.class);
            if (data instanceof ChatEmpty) {
                return set;
            }

            telegram4j.tl.BaseChat chat = (telegram4j.tl.BaseChat) data;
            int flags = chat.flags();
            for (Flag value : values()) {
                // This check is unnecessary, because in the MinUser
                // these flags are not occupied, but if a new one with one of
                // these values is added, it will be dangerous...
                if (value == CAN_SET_USERNAME || value == HAS_SCHEDULED) continue;
                if ((flags & value.flag) != 0) {
                    set.add(value);
                }
            }

            return set;
        }
    }
}
