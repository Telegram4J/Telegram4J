package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.chat.PrivateChat;
import telegram4j.core.util.EntityFactory;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class User implements PeerEntity {

    private final MTProtoTelegramClient client;
    private final BaseUser minData;
    @Nullable
    private final UserFull fullData;

    public User(MTProtoTelegramClient client, UserFull fullData, BaseUser minData) {
        this.client = Objects.requireNonNull(client, "client");
        this.minData = Objects.requireNonNull(minData, "minData");
        this.fullData = Objects.requireNonNull(fullData, "fullData");
    }

    public User(MTProtoTelegramClient client, BaseUser minData) {
        this.client = Objects.requireNonNull(client, "client");
        this.minData = Objects.requireNonNull(minData, "minData");
        this.fullData = null;
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    public EnumSet<Flag> getFlags() {
        return Flag.fromUserFull(fullData, minData);
    }

    public PrivateChat asPrivateChat() {
        return new PrivateChat(client, this, null);
    }

    // MinUser fields

    /**
     * Gets first name of user, if present.
     *
     * @return The first name of user, if present.
     */
    public Optional<String> getFirstName() {
        return Optional.ofNullable(minData.firstName());
    }

    /**
     * Gets last name of user, if present.
     *
     * @return The last name of user, if present.
     */
    public Optional<String> getLastName() {
        return Optional.ofNullable(minData.lastName());
    }

    /**
     * Gets full name of user in format <b>first name last name</b>, or empty string if first and last name don't present.
     *
     * @return The full name of user.
     */
    public String getFullName() {
        return getFirstName().map(s -> s + " ").orElse("") + getLastName().orElse("");
    }

    public Optional<String> getUsername() {
        return Optional.ofNullable(minData.username());
    }

    public Optional<String> getPhone() {
        return Optional.ofNullable(minData.phone());
    }

    public Optional<ChatPhoto> getMinPhoto() {
        return Optional.ofNullable(TlEntityUtil.unmapEmpty(minData.photo(), BaseUserProfilePhoto.class))
                .map(c -> new ChatPhoto(client, c, ImmutableInputPeerUser.of(minData.id(),
                        Objects.requireNonNull(minData.accessHash())), -1));
    }

    public Optional<Photo> getPhoto() {
        return Optional.ofNullable(fullData)
                .map(u -> TlEntityUtil.unmapEmpty(u.profilePhoto(), BasePhoto.class))
                .map(d -> new Photo(client, d, ImmutableInputPeerUser.of(minData.id(),
                        Objects.requireNonNull(minData.accessHash())), -1));
    }

    public Optional<UserStatus> getStatus() {
        return Optional.ofNullable(minData.status()).map(d -> EntityFactory.createUserStatus(client, d));
    }

    public Optional<Integer> getBotInfoVersion() {
        return Optional.ofNullable(minData.botInfoVersion());
    }

    public Optional<List<RestrictionReason>> getRestrictionReason() {
        return Optional.ofNullable(minData.restrictionReason())
                .map(list -> list.stream()
                        .map(d -> new RestrictionReason(client, d))
                        .collect(Collectors.toList()));
    }

    public Optional<String> getBotInlinePlaceholder() {
        return Optional.ofNullable(minData.botInlinePlaceholder());
    }

    public Optional<String> getLangCode() {
        return Optional.ofNullable(minData.langCode());
    }

    @Override
    public Id getId() {
        return Id.ofUser(minData.id(), minData.accessHash());
    }

    // FullUser fields

    public Optional<String> getAbout() {
        return Optional.ofNullable(fullData).map(UserFull::about);
    }

    public Optional<PeerSettings> getSettings() {
        return Optional.ofNullable(fullData)
                .map(UserFull::settings)
                .map(d -> new PeerSettings(client, d));
    }

    public Optional<PeerNotifySettings> getNotifySettings() {
        return Optional.ofNullable(fullData)
                .map(UserFull::notifySettings)
                .map(d -> new PeerNotifySettings(client, d));
    }

    public Optional<BotInfo> getBotInfo() {
        return Optional.ofNullable(fullData)
                .map(UserFull::botInfo)
                .map(d -> new BotInfo(client, d));
    }

    public Optional<Integer> getPinnedMessageId() {
        return Optional.ofNullable(fullData).map(UserFull::pinnedMsgId);
    }

    public Optional<Integer> getCommonChatsCount() {
        return Optional.ofNullable(fullData).map(UserFull::commonChatsCount);
    }

    public Optional<Integer> getFolderId() {
        return Optional.ofNullable(fullData).map(UserFull::folderId);
    }

    public Optional<Duration> getMessageAutoDeleteDuration() {
        return Optional.ofNullable(fullData)
                .map(UserFull::ttlPeriod)
                .map(Duration::ofSeconds);
    }

    public Optional<String> getThemeEmoticon() {
        return Optional.ofNullable(fullData).map(UserFull::themeEmoticon);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User that = (User) o;
        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public String toString() {
        return "User{" +
                "minData=" + minData +
                ", fullData=" + fullData +
                '}';
    }

    public enum Flag {

        // FullUser flags

        /** Whether you have blocked this user. */
        BLOCKED(0),

        /** Whether this user can make VoIP calls. */
        PHONE_CALLS_AVAILABLE(4),

        /** Whether this user's privacy settings allow you to call him. */
        PHONE_CALLS_PRIVATE(5),

        /** Whether you can pin messages in the chat with this user, you can do this only for a chat with yourself. */
        CAN_PIN_MESSAGE(7),

        /** Whether <a href="https://core.telegram.org/api/scheduled-messages">scheduled messages</a> are available. */
        HAS_SCHEDULED(12),

        /** Whether the user can receive video calls. */
        VIDEO_CALLS_AVAILABLE(13),

        // MinUser flags

        /** Whether this user indicates the currently logged-in user. */
        SELF(10),

        /** Whether this user is a contact. */
        CONTACT(11),

        /** Whether this user is a mutual contact. */
        MUTUAL_CONTACTS(12),

        /** Whether the account of this user was deleted. */
        DELETED(13),

        /** Is this user a bot?. */
        BOT(14),

        /** Can the bot see all messages in groups? */
        BOT_CHAT_HISTORY(15),

        /** Can the bot be added to groups? */
        BOT_NO_CHATS(16),

        /** Whether this user is verified. */
        VERIFIED(17),

        /** Access to this user must be restricted. */
        RESTRICTED(18),

        /** Whether this user is <a href="https://core.telegram.org/api/min">min</a>. */
        MIN(20),

        /** Whether the bot can request our geolocation in inline mode. */
        BOT_INLINE_GEO(21),

        /** Whether this is an official support user. */
        SUPPORT(23),

        /** This may be a scam user. */
        SCAM(24),

        /** If set, the profile picture for this user should be refetched. */
        APPLY_MIN_PHOTO(25),

        /** If set, this user was reported by many users as a fake or scam user: be careful when interacting with them. */
        FAKE(26);

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

        public static EnumSet<Flag> fromUserFull(@Nullable telegram4j.tl.UserFull userFull, telegram4j.tl.User userMin) {
            EnumSet<Flag> set = EnumSet.noneOf(Flag.class);

            if (userFull != null) {
                int flags = userFull.flags();
                for (Flag value : values()) {
                    if (value.ordinal() >= VIDEO_CALLS_AVAILABLE.ordinal()) continue;
                    if ((flags & value.flag) != 0) {
                        set.add(value);
                    }
                }
            }

            // Add min user flags
            set.addAll(fromUserMin(userMin));

            return set;
        }

        public static EnumSet<Flag> fromUserMin(telegram4j.tl.User user) {
            EnumSet<Flag> set = EnumSet.noneOf(Flag.class);
            if (user instanceof UserEmpty) {
                return set;
            }

            BaseUser user0 = (BaseUser) user;

            int flags = user0.flags();
            for (Flag value : values()) {
                // UserFull and UserMin flags has collision in bit positions
                if (value.ordinal() < SELF.ordinal()) continue;
                if ((flags & value.flag) != 0) {
                    set.add(value);
                }
            }

            return set;
        }
    }
}
