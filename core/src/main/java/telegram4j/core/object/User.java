package telegram4j.core.object;

import reactor.core.publisher.Flux;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.chat.PrivateChat;
import telegram4j.core.retriever.EntityRetriever;
import telegram4j.core.util.EntityFactory;
import telegram4j.core.util.Id;
import telegram4j.core.util.PaginationSupport;
import telegram4j.core.util.PeerId;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;
import telegram4j.tl.photos.Photos;
import telegram4j.tl.photos.PhotosSlice;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static telegram4j.mtproto.util.TlEntityUtil.toInputUser;

/**
 * Representation for available min/full users.
 */
public class User implements PeerEntity {

    private final MTProtoTelegramClient client;
    private final BaseUser minData;
    @Nullable
    private final UserFull fullData;

    public User(MTProtoTelegramClient client, UserFull fullData, BaseUser minData) {
        this.client = Objects.requireNonNull(client);
        this.minData = Objects.requireNonNull(minData);
        this.fullData = Objects.requireNonNull(fullData);
    }

    public User(MTProtoTelegramClient client, BaseUser minData) {
        this.client = Objects.requireNonNull(client);
        this.minData = Objects.requireNonNull(minData);
        this.fullData = null;
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    @Override
    public Id getId() {
        return Id.ofUser(minData.id(), minData.accessHash());
    }

    /**
     * Computes {@link EnumSet} with user flags from full and min data.
     *
     * @return The {@link EnumSet} with user flags.
     */
    public EnumSet<Flag> getFlags() {
        return Flag.fromUserFull(fullData, minData);
    }

    /**
     * Converts this user into the private chat for interacting with messages.
     *
     * @return The {@link PrivateChat} from this user with {@code null} self user.
     */
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
        StringJoiner j = new StringJoiner(" ");
        getFirstName().ifPresent(j::add);
        getLastName().ifPresent(j::add);
        return j.toString();
    }

    /**
     * Gets username of this user in format <b>@username</b>, if present can be used in the {@link EntityRetriever#resolvePeer(PeerId)}.
     *
     * @return The username of this user, if present.
     */
    public Optional<String> getUsername() {
        return Optional.ofNullable(minData.username());
    }

    /**
     * Gets phone number of this user, if private settings allows.
     *
     * @return The phone number of this user, if private settings allows.
     */
    public Optional<String> getPhone() {
        return Optional.ofNullable(minData.phone());
    }

    /**
     * Gets the low quality user photo, if present.
     *
     * @return The {@link ChatPhoto photo} of user, if present.
     */
    public Optional<ChatPhoto> getMinPhoto() {
        return Optional.ofNullable(TlEntityUtil.unmapEmpty(minData.photo(), BaseUserProfilePhoto.class))
                .map(c -> new ChatPhoto(client, c, client.asResolvedInputPeer(getId()), -1));
    }

    /**
     * Gets the normal user photo, if present
     * and if detailed information about user is available.
     *
     * @return The {@link Photo photo} of user, if present.
     */
    public Optional<Photo> getPhoto() {
        return Optional.ofNullable(fullData)
                .map(u -> TlEntityUtil.unmapEmpty(u.profilePhoto(), BasePhoto.class))
                .map(d -> new Photo(client, d, -1, client.asResolvedInputPeer(getId())));
    }

    /**
     * Gets current online status of user, if present.
     *
     * @return The {@link UserStatus} of user, if present.
     */
    public Optional<UserStatus> getStatus() {
        return Optional.ofNullable(TlEntityUtil.unmapEmpty(minData.status()))
                .map(d -> EntityFactory.createUserStatus(client, d));
    }

    /**
     * Gets incremental version of the bot info, if user is bot.
     *
     * @return The incremental version of the bot info, if user is bot.
     */
    public Optional<Integer> getBotInfoVersion() {
        return Optional.ofNullable(minData.botInfoVersion());
    }

    /**
     * Gets list of reasons for why access to this user must be restricted, if present.
     *
     * @return The list of reasons for why access to this user must be restricted, if present.
     */
    public Optional<List<RestrictionReason>> getRestrictionReason() {
        return Optional.ofNullable(minData.restrictionReason())
                .map(list -> list.stream()
                        .map(d -> new RestrictionReason(client, d))
                        .collect(Collectors.toList()));
    }

    /**
     * Gets placeholder for inline query text field, if bot is user.
     *
     * @return The placeholder for inline query text field, if bot is user.
     */
    public Optional<String> getBotInlinePlaceholder() {
        return Optional.ofNullable(minData.botInlinePlaceholder());
    }

    /**
     * Gets the user's language in OS ISO 639-1 format.
     *
     * @return The ISO 639-1 lang code of the user locale.
     */
    public Optional<String> getLangCode() {
        return Optional.ofNullable(minData.langCode());
    }

    // FullUser fields

    /**
     * Gets user's bio/about text.
     *
     * @return The user's bio text.
     */
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

    public Optional<String> getPrivateForwardName() {
        return Optional.ofNullable(fullData).map(UserFull::privateForwardName);
    }

    // Interaction methods

    /**
     * Retrieve user profile {@link Photo photo}s by specified pagination parameters.
     *
     * @see <a href="https://core.telegram.org/api/offsets">Pagonation</a>
     * @param offset The number of photos to be skipped.
     * @param maxId The id of max {@link Photo#getId()}.
     * @param limit The number of photos to be returned.
     * @return A {@link Flux} emitting {@link Photo} objects.
     */
    public Flux<Photo> getPhotos(int offset, long maxId, int limit) {
        InputPeer p = client.asResolvedInputPeer(getId());
        InputUser user = toInputUser(p);

        return PaginationSupport.paginate(o -> client.getServiceHolder().getUserService()
                .getUserPhotos(user, o, maxId, limit), c -> c instanceof PhotosSlice
                        ? ((PhotosSlice) c).count() : c.photos().size(), offset, limit)
                .flatMapIterable(Photos::photos)
                .cast(BasePhoto.class)
                .map(c -> new Photo(client, c, -1, p));
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
        FAKE(26),

        BOT_ATTACH_MENU(27),

        PREMIUM(28),

        ATTACH_MENU_ENABLED(29);

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

        public static EnumSet<Flag> fromUserFull(@Nullable telegram4j.tl.UserFull userFull, telegram4j.tl.BaseUser userMin) {
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

        public static EnumSet<Flag> fromUserMin(telegram4j.tl.BaseUser user) {
            EnumSet<Flag> set = EnumSet.noneOf(Flag.class);

            int flags = user.flags();
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
