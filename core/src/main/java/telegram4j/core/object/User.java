package telegram4j.core.object;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.internal.EntityFactory;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.object.chat.PrivateChat;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.retriever.EntityRetriever;
import telegram4j.core.util.BitFlag;
import telegram4j.core.util.Id;
import telegram4j.core.util.PaginationSupport;
import telegram4j.core.util.PeerId;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;
import telegram4j.tl.photos.Photos;
import telegram4j.tl.photos.PhotosSlice;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static telegram4j.mtproto.util.TlEntityUtil.toInputUser;
import static telegram4j.tl.BaseUser.*;
import static telegram4j.tl.UserFull.*;

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
        return Flag.of(fullData, minData);
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
     * Gets username of this user in format <b>@username</b>,
     * if present can be used in the {@link EntityRetriever#resolvePeer(PeerId)}.
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
                .map(EntityFactory::createUserStatus);
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
        return Optional.ofNullable(minData.restrictionReason());
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

    public Optional<EmojiStatus> getEmojiStatus() {
        return Optional.ofNullable(TlEntityUtil.unmapEmpty(minData.emojiStatus()))
                .map(e -> {
                    switch (e.identifier()) {
                        case BaseEmojiStatus.ID:
                            BaseEmojiStatus base = (BaseEmojiStatus) e;
                            return new EmojiStatus(base.documentId(), null);
                        case EmojiStatusUntil.ID:
                            EmojiStatusUntil until = (EmojiStatusUntil) e;
                            return new EmojiStatus(until.documentId(), Instant.ofEpochSecond(until.until()));
                        default: throw new IllegalStateException("Unknown EmojiStatus type: " + e);
                    }
                });
    }

    // FullUser fields

    /**
     * Gets user's bio/about text, if present.
     *
     * @return The user's bio text, if present.
     */
    public Optional<String> getAbout() {
        return Optional.ofNullable(fullData).map(UserFull::about);
    }

    public Optional<PeerSettings> getSettings() {
        return Optional.ofNullable(fullData)
                .map(UserFull::settings)
                .map(PeerSettings::new);
    }

    public Optional<PeerNotifySettings> getNotifySettings() {
        return Optional.ofNullable(fullData)
                .map(UserFull::notifySettings)
                .map(PeerNotifySettings::new);
    }

    /**
     * Gets detailed info about bot, if present.
     *
     * @return The detailed info about bot, if present.
     */
    public Optional<BotInfo> getBotInfo() {
        return Optional.ofNullable(fullData)
                .map(UserFull::botInfo)
                .map(d -> new BotInfo(client, d));
    }

    /**
     * Gets id of pinned message in this user dialog, if present.
     *
     * @return The id of pinned message in this user dialog, if present.
     */
    public Optional<Integer> getPinnedMessageId() {
        return Optional.ofNullable(fullData).map(UserFull::pinnedMsgId);
    }

    /**
     * Requests to retrieve pinned message in this dialog.
     *
     * @return An {@link Mono} emitting on successful completion the {@link AuxiliaryMessages message container}.
     */
    public Mono<AuxiliaryMessages> getPinnedMessage() {
        return getPinnedMessage(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve pinned message in this user dialog using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @return An {@link Mono} emitting on successful completion the {@link AuxiliaryMessages message container}.
     */
    public Mono<AuxiliaryMessages> getPinnedMessage(EntityRetrievalStrategy strategy) {
        return Mono.justOrEmpty(fullData)
                .mapNotNull(UserFull::pinnedMsgId)
                .flatMap(id -> client.withRetrievalStrategy(strategy)
                        .getMessagesById(List.of(ImmutableInputMessageID.of(id))));
    }

    /**
     * Gets count of common with current user chats, if full data is present.
     *
     * @return The count of common with current user chats, if full data is present.
     */
    public Optional<Integer> getCommonChatsCount() {
        return Optional.ofNullable(fullData).map(UserFull::commonChatsCount);
    }

    public Optional<Integer> getFolderId() {
        return Optional.ofNullable(fullData).map(UserFull::folderId);
    }

    /**
     * Gets {@link Duration} of the message Time-To-Live in this user dialog, if present.
     *
     * @return The {@link Duration} of the message Time-To-Live in this user dialog, if present.
     */
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

    public Optional<EnumSet<ChatAdminRights>> getBotGroupAdminRights() {
        return Optional.ofNullable(fullData)
                .map(UserFull::botGroupAdminRights)
                .map(ChatAdminRights::of);
    }

    public Optional<EnumSet<ChatAdminRights>> getBotBroadcastAdminRights() {
        return Optional.ofNullable(fullData)
                .map(UserFull::botBroadcastAdminRights)
                .map(ChatAdminRights::of);
    }

    // TODO: implement
    // @Nullable
    // List<PremiumGiftOption> premiumGifts();

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

    public enum Flag implements BitFlag {

        // FullUser flags

        /** Whether you have blocked this user. */
        BLOCKED(BLOCKED_POS),

        /** Whether this user can make VoIP calls. */
        PHONE_CALLS_AVAILABLE(PHONE_CALLS_AVAILABLE_POS),

        /** Whether this user's privacy settings allow you to call him. */
        PHONE_CALLS_PRIVATE(PHONE_CALLS_PRIVATE_POS),

        /** Whether you can pin messages in the chat with this user, you can do this only for a chat with yourself. */
        CAN_PIN_MESSAGE(CAN_PIN_MESSAGE_POS),

        /** Whether <a href="https://core.telegram.org/api/scheduled-messages">scheduled messages</a> are available. */
        HAS_SCHEDULED(HAS_SCHEDULED_POS),

        /** Whether the user can receive video calls. */
        VIDEO_CALLS_AVAILABLE(VIDEO_CALLS_AVAILABLE_POS),

        // MinUser flags

        /** Whether this user indicates the currently logged-in user. */
        SELF(SELF_POS),

        /** Whether this user is a contact. */
        CONTACT(CONTACT_POS),

        /** Whether this user is a mutual contact. */
        MUTUAL_CONTACTS(MUTUAL_CONTACT_POS),

        /** Whether the account of this user was deleted. */
        DELETED(DELETED_POS),

        /** Is this user a bot?. */
        BOT(BOT_POS),

        /** Can the bot see all messages in groups? */
        BOT_CHAT_HISTORY(BOT_CHAT_HISTORY_POS),

        /** Can the bot be added to groups? */
        BOT_NO_CHATS(BOT_NOCHATS_POS),

        /** Whether this user is verified. */
        VERIFIED(VERIFIED_POS),

        /** Access to this user must be restricted. */
        RESTRICTED(RESTRICTED_POS),

        /** Whether this user is <a href="https://core.telegram.org/api/min">min</a>. */
        MIN(MIN_POS),

        /** Whether the bot can request our geolocation in inline mode. */
        BOT_INLINE_GEO(BOT_INLINE_GEO_POS),

        /** Whether this is an official support user. */
        SUPPORT(SUPPORT_POS),

        /** This may be a scam user. */
        SCAM(SCAM_POS),

        /** If set, the profile picture for this user should be refetched. */
        APPLY_MIN_PHOTO(APPLY_MIN_PHOTO_POS),

        /** If set, this user was reported by many users as a fake or scam user: be careful when interacting with them. */
        FAKE(FAKE_POS),

        BOT_ATTACH_MENU(BOT_ATTACH_MENU_POS),

        PREMIUM(PREMIUM_POS),

        ATTACH_MENU_ENABLED(ATTACH_MENU_ENABLED_POS);

        private final byte position;

        Flag(byte position) {
            this.position = position;
        }

        @Override
        public byte position() {
            return position;
        }

        private static EnumSet<Flag> of(@Nullable telegram4j.tl.UserFull userFull, telegram4j.tl.BaseUser userMin) {
            var minFlags = of(userMin);
            if (userFull != null) {
                var set = EnumSet.allOf(Flag.class);
                int flags = userFull.flags();
                set.removeIf(value -> value.ordinal() >= SELF.ordinal() || (flags & value.mask()) == 0);
                set.addAll(of(userMin));
                return set;
            }
            return minFlags;
        }

        private static EnumSet<Flag> of(telegram4j.tl.BaseUser user) {
            var set = EnumSet.allOf(Flag.class);
            int flags = user.flags();
            set.removeIf(value -> value.ordinal() < SELF.ordinal() || (flags & value.mask()) == 0);
            return set;
        }
    }
}
