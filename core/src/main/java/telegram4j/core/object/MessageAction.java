package telegram4j.core.object;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.object.chat.Channel;
import telegram4j.core.object.chat.GroupChat;
import telegram4j.core.object.chat.SupergroupChat;
import telegram4j.core.object.markup.KeyboardButton;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.util.Id;
import telegram4j.mtproto.file.ChatPhotoContext;
import telegram4j.mtproto.file.MessageActionContext;
import telegram4j.tl.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/** Basic type of service messages. */
public sealed class MessageAction implements TelegramObject {
    protected final MTProtoTelegramClient client;
    protected final Type type;

    public MessageAction(MTProtoTelegramClient client, Type type) {
        this.client = Objects.requireNonNull(client);
        this.type = Objects.requireNonNull(type);
    }

    @Override
    public final MTProtoTelegramClient getClient() {
        return client;
    }

    public final Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "MessageAction{" +
                "type=" + type +
                '}';
    }

    public enum Type {
        /** Group created. */
        CHAT_CREATE,

        /** Group name changed. */
        CHAT_EDIT_TITLE,

        /** Group profile changed. */
        EDIT_CHAT_PHOTO,

        /** Group profile photo removed. */
        DELETE_CHAT_PHOTO,

        /** New member in the group. */
        CHAT_JOIN_USERS,

        /** User left the group. */
        CHAT_LEFT_USER,

        /** A user joined the chat via an invitation link. */
        CHAT_JOINED_BY_LINK,

        /** A user joined the chat via a request link. */
        CHAT_JOINED_BY_REQUEST,

        /** The channel was created. */
        CHANNEL_CREATE,

        /** Indicates the chat was {@link Channel migrated} to the specified supergroup. */
        CHAT_MIGRATE_TO,

        /** Indicates the channel was {@link Channel migrated} from the specified chat. */
        CHANNEL_MIGRATE_FROM,

        /** A message was pinned. */
        PIN_MESSAGE,

        /** Chat history was cleared. */
        HISTORY_CLEAR,

        /** Someone scored in a game. */
        GAME_SCORE,

        /** A user just sent a payment to me (a bot). */
        PAYMENT_SENT_ME,

        /** A payment was sent. */
        PAYMENT_SENT,

        /** A phone call. */
        PHONE_CALL,

        /** A screenshot of the chat was taken. */
        SCREENSHOT_TAKEN,

        /** Custom action (most likely not supported by the current layer, an upgrade might be needed). */
        CUSTOM,

        /**
         * The domain name of the website on which the user has logged in.
         * <a href="https://core.telegram.org/widgets/login">More about Telegram Login.</a>
         */
        BOT_ALLOWED,

        /** Secure <a href="https://core.telegram.org/passport">telegram passport</a> values were received. */
        SECURE_VALUES_SENT_ME,

        /** Request for secure <a href="https://core.telegram.org/passport">telegram passport</a> values was sent. */
        SECURE_VALUES_SENT,

        /** A contact just signed up to telegram. */
        CONTACT_SIGN_UP,

        /**
         * We are now in proximity of this user (triggered by the other user,
         * by sending a live geolocation with the {@link MessageMediaGeoLive#proximityNotificationRadius()} flag).
         */
        GEO_PROXIMITY_REACHED,

        /** The group call has ended. */
        GROUP_CALL,

        /** A set of users was invited to the group call. */
        INVITE_TO_GROUP_CALL,

        /** You changed the Time-To-Live of your messages in this chat. */
        SET_MESSAGES_TTL,

        /** A group call was scheduled. */
        GROUP_CALL_SCHEDULED,

        TOPIC_CREATE,

        TOPIC_EDIT,
        /** The chat theme was changed. */
        SET_CHAT_THEME,

        SUGGEST_PROFILE_PHOTO,

        REQUESTED_PEER
    }

    public static final class BotAllowed extends MessageAction {

        private final MessageActionBotAllowed data;

        public BotAllowed(MTProtoTelegramClient client, MessageActionBotAllowed data) {
            super(client, Type.BOT_ALLOWED);
            this.data = Objects.requireNonNull(data);
        }

        public Optional<String> getDomain() {
            return Optional.ofNullable(data.domain());
        }

        @Override
        public String toString() {
            return "BotAllowed{" +
                    "data=" + data +
                    '}';
        }
    }

    /** Service message representing {@link Channel} creation. */
    public static final class ChannelCreate extends MessageAction {

        private final MessageActionChannelCreate data;

        public ChannelCreate(MTProtoTelegramClient client, MessageActionChannelCreate data) {
            super(client, Type.CHANNEL_CREATE);
            this.data = Objects.requireNonNull(data);
        }

        /**
         * Gets title of channel on creation.
         *
         * @return The title of channel on creation.
         */
        public String getTitle() {
            return data.title();
        }

        @Override
        public String toString() {
            return "ChannelCreate{" +
                    "data=" + data +
                    '}';
        }
    }

    /** Service message representing first message after {@link GroupChat} migration. */
    public static final class ChannelMigrateFrom extends MessageAction {

        private final MessageActionChannelMigrateFrom data;

        public ChannelMigrateFrom(MTProtoTelegramClient client, MessageActionChannelMigrateFrom data) {
            super(client, Type.CHANNEL_MIGRATE_FROM);
            this.data = Objects.requireNonNull(data);
        }

        /**
         * Gets title of old group chat.
         *
         * @return The title of old group chat.
         */
        public String getChatTitle() {
            return data.title();
        }

        /**
         * Gets id of old group chat.
         *
         * @return The id of old group chat.
         */
        public Id getChatId() {
            return Id.ofChat(data.chatId());
        }

        /**
         * Requests to retrieve {@link GroupChat} from which channel was migrated.
         *
         * @return A {@link Mono} emitting on successful completion {@link GroupChat old group chat}.
         */
        public Mono<GroupChat> getChat() {
            return getChat(MappingUtil.IDENTITY_RETRIEVER);
        }

        /**
         * Requests to retrieve {@link GroupChat} from which channel was migrated using
         * specified retrieval strategy.
         *
         * @param strategy The strategy to apply.
         * @return A {@link Mono} emitting on successful completion {@link GroupChat old group chat}.
         */
        public Mono<GroupChat> getChat(EntityRetrievalStrategy strategy) {
            return client.withRetrievalStrategy(strategy).getChatById(getChatId())
                    .cast(GroupChat.class);
        }

        @Override
        public String toString() {
            return "ChannelMigrateFrom{" +
                    "data=" + data +
                    '}';
        }
    }

    /** Service message representing joined to chat users. */
    public static final class ChatJoinUsers extends MessageAction {

        private final MessageActionChatAddUser data;

        public ChatJoinUsers(MTProtoTelegramClient client, MessageActionChatAddUser data) {
            super(client, Type.CHAT_JOIN_USERS);
            this.data = Objects.requireNonNull(data);
        }

        /**
         * Gets a set of ids of the joined users.
         *
         * @return The set of ids of the joined users.
         */
        public Set<Id> getUserIds() {
            return data.users().stream()
                    .map(Id::ofUser)
                    .collect(Collectors.toSet());
        }

        /**
         * Requests to retrieve joined users.
         *
         * @return A {@link Flux} which continually emits {@link User joined users}.
         */
        public Flux<User> getUsers() {
            return getUsers(MappingUtil.IDENTITY_RETRIEVER);
        }

        /**
         * Requests to retrieve joined users using specified retrieval strategy.
         *
         * @param strategy The strategy to apply.
         * @return A {@link Flux} which continually emits {@link User joined users}.
         */
        public Flux<User> getUsers(EntityRetrievalStrategy strategy) {
            var retriever = client.withRetrievalStrategy(strategy);
            return Flux.fromIterable(data.users())
                    .map(Id::ofUser)
                    .flatMap(retriever::getUserById);
        }

        @Override
        public String toString() {
            return "ChatAddUser{" +
                    "data=" + data +
                    '}';
        }
    }

    /** Service message representing {@link GroupChat} creation. */
    public static final class ChatCreate extends MessageAction {

        private final MessageActionChatCreate data;

        public ChatCreate(MTProtoTelegramClient client, MessageActionChatCreate data) {
            super(client, Type.CHAT_CREATE);
            this.data = Objects.requireNonNull(data);
        }

        /**
         * Gets title of group chat.
         *
         * @return The title of group chat.
         */
        public String getChatTitle() {
            return data.title();
        }

        /**
         * Gets a set of ids of the invited users.
         *
         * @return The set of ids of the invited users.
         */
        public Set<Id> getUserIds() {
            return data.users().stream()
                    .map(Id::ofUser)
                    .collect(Collectors.toSet());
        }

        /**
         * Requests to retrieve invited users.
         *
         * @return A {@link Flux} which continually emits {@link User invited users}.
         */
        public Flux<User> getUsers() {
            return getUsers(MappingUtil.IDENTITY_RETRIEVER);
        }

        /**
         * Requests to retrieve invited users using specified retrieval strategy.
         *
         * @param strategy The strategy to apply.
         * @return A {@link Flux} which continually emits {@link User invited users}.
         */
        public Flux<User> getUsers(EntityRetrievalStrategy strategy) {
            var retriever = client.withRetrievalStrategy(strategy);
            return Flux.fromIterable(data.users())
                    .map(Id::ofUser)
                    .flatMap(retriever::getUserById);
        }

        @Override
        public String toString() {
            return "ChatCreate{" +
                    "data=" + data +
                    '}';
        }
    }

    /** Service message representing left from the chat user. */
    public static final class ChatLeftUser extends MessageAction {

        private final MessageActionChatDeleteUser data;

        public ChatLeftUser(MTProtoTelegramClient client, MessageActionChatDeleteUser data) {
            super(client, Type.CHAT_LEFT_USER);
            this.data = Objects.requireNonNull(data);
        }

        /**
         * Gets id of left user.
         *
         * @return The id of left user.
         */
        public Id getUserId() {
            return Id.ofUser(data.userId());
        }

        /**
         * Requests to retrieve left user.
         *
         * @return An {@link Mono} emitting on successful completion the {@link User left user}.
         */
        public Mono<User> getUser() {
            return getUser(MappingUtil.IDENTITY_RETRIEVER);
        }

        /**
         * Requests to retrieve left user using specified retrieval strategy.
         *
         * @param strategy The strategy to apply
         * @return An {@link Mono} emitting on successful completion the {@link User left user}.
         */
        public Mono<User> getUser(EntityRetrievalStrategy strategy) {
            return client.withRetrievalStrategy(strategy).getUserById(getUserId());
        }

        @Override
        public String toString() {
            return "ChatDeleteUser{" +
                    "data=" + data +
                    '}';
        }
    }

    public static final class UpdateChatPhoto extends MessageAction {

        @Nullable
        private final MessageActionChatEditPhoto data;
        private final ChatPhotoContext context;

        public UpdateChatPhoto(MTProtoTelegramClient client) {
            super(client, Type.DELETE_CHAT_PHOTO);
            this.data = null;
            this.context = null;
        }

        public UpdateChatPhoto(MTProtoTelegramClient client, MessageActionChatEditPhoto data,
                               ChatPhotoContext context) {
            super(client, Type.EDIT_CHAT_PHOTO);
            this.data = Objects.requireNonNull(data);
            this.context = Objects.requireNonNull(context);
        }

        /**
         * Gets current chat photo, absent if photo was deleted,
         *
         * @return The current chat photo, absent if photo was deleted,
         */
        public Optional<Photo> getCurrentPhoto() {
            if (data == null || !(data.photo() instanceof BasePhoto p)) {
                return Optional.empty();
            }
            return Optional.of(new Photo(client, p, Objects.requireNonNull(context)));
        }

        @Override
        public String toString() {
            return "ChatEditPhoto{" +
                    "type=" + type +
                    ", data=" + data +
                    '}';
        }
    }

    public static final class ChatEditTitle extends MessageAction {

        private final MessageActionChatEditTitle data;

        public ChatEditTitle(MTProtoTelegramClient client, MessageActionChatEditTitle data) {
            super(client, Type.CHAT_EDIT_TITLE);
            this.data = Objects.requireNonNull(data);
        }

        /**
         * Gets current title of chat.
         *
         * @return The current title of chat.
         */
        public String getCurrentTitle() {
            return data.title();
        }

        @Override
        public String toString() {
            return "ChatEditTitle{" +
                    "data=" + data +
                    '}';
        }
    }

    /** Service message representing the joining of <i>current</i> user to the chat by invite link. */
    public static final class ChatJoinedByLink extends MessageAction {

        private final MessageActionChatJoinedByLink data;

        public ChatJoinedByLink(MTProtoTelegramClient client, MessageActionChatJoinedByLink data) {
            super(client, Type.CHAT_JOINED_BY_LINK);
            this.data = Objects.requireNonNull(data);
        }

        /**
         * Gets id of user, which invited <i>current</i> user to chat.
         *
         * @return The id of user, which invited <i>current</i> user to chat.
         */
        public Id getInviterId() {
            return Id.ofUser(data.inviterId());
        }

        /**
         * Requests to retrieve user which invited <i>current</i> user to chat.
         *
         * @return An {@link Mono} emitting on successful completion the {@link User inviter}.
         */
        public Mono<User> getInviter() {
            return getInviter(MappingUtil.IDENTITY_RETRIEVER);
        }

        /**
         * Requests to retrieve user which invited <i>current</i> user to chat using specified retrieval strategy.
         *
         * @param strategy The strategy to apply
         * @return An {@link Mono} emitting on successful completion the {@link User inviter}.
         */
        public Mono<User> getInviter(EntityRetrievalStrategy strategy) {
            return client.withRetrievalStrategy(strategy).getUserById(getInviterId());
        }

        @Override
        public String toString() {
            return "ChatJoinedByLink{" +
                    "data=" + data +
                    '}';
        }
    }

    /** Service message representing the latest message in old {@link GroupChat}. */
    public static final class ChatMigrateTo extends MessageAction {

        private final MessageActionChatMigrateTo data;

        public ChatMigrateTo(MTProtoTelegramClient client, MessageActionChatMigrateTo data) {
            super(client, Type.CHAT_MIGRATE_TO);
            this.data = Objects.requireNonNull(data);
        }

        /**
         * Gets id of new {@link SupergroupChat} to which group chat was migrated.
         *
         * @return The id of new {@link SupergroupChat} to which group chat was migrated.
         */
        public Id getChannelId() {
            return Id.ofChannel(data.channelId());
        }

        /**
         * Requests to retrieve {@link SupergroupChat} to which group chat was migrated.
         *
         * @return A {@link Mono} emitting on successful completion {@link SupergroupChat new supergroup}.
         */
        public Mono<SupergroupChat> getChannel() {
            return getChannel(MappingUtil.IDENTITY_RETRIEVER);
        }

        /**
         * Requests to retrieve {@link SupergroupChat} to which group chat was migrated using
         * specified retrieval strategy.
         *
         * @param strategy The strategy to apply.
         * @return A {@link Mono} emitting on successful completion {@link SupergroupChat new supergroup}.
         */
        public Mono<SupergroupChat> getChannel(EntityRetrievalStrategy strategy) {
            return client.withRetrievalStrategy(strategy).getChatById(getChannelId())
                    .cast(SupergroupChat.class);
        }

        @Override
        public String toString() {
            return "ChatMigrateTo{" +
                    "data=" + data +
                    '}';
        }
    }

    public static final class Custom extends MessageAction {

        private final MessageActionCustomAction data;

        public Custom(MTProtoTelegramClient client, MessageActionCustomAction data) {
            super(client, Type.CUSTOM);
            this.data = Objects.requireNonNull(data);
        }

        public String getMessage() {
            return data.message();
        }

        @Override
        public String toString() {
            return "Custom{" +
                    "data=" + data +
                    '}';
        }
    }

    public static final class GameScore extends MessageAction {

        private final MessageActionGameScore data;

        public GameScore(MTProtoTelegramClient client, MessageActionGameScore data) {
            super(client, Type.GAME_SCORE);
            this.data = Objects.requireNonNull(data);
        }

        public long getGameId() {
            return data.gameId();
        }

        public int getScore() {
            return data.score();
        }

        @Override
        public String toString() {
            return "GameScore{" +
                    "data=" + data +
                    '}';
        }
    }

    public static final class GeoProximityReached extends MessageAction {

        private final MessageActionGeoProximityReached data;

        public GeoProximityReached(MTProtoTelegramClient client, MessageActionGeoProximityReached data) {
            super(client, Type.GEO_PROXIMITY_REACHED);
            this.data = Objects.requireNonNull(data);
        }

        public Id getFromPeerId() {
            return Id.of(data.fromId());
        }

        public Id getDestinationPeerId() {
            return Id.of(data.toId());
        }

        public int getDistance() {
            return data.distance();
        }

        @Override
        public String toString() {
            return "GeoProximityReached{" +
                    "data=" + data +
                    '}';
        }
    }

    public static final class GroupCall extends MessageAction {

        private final MessageActionGroupCall data;

        public GroupCall(MTProtoTelegramClient client, MessageActionGroupCall data) {
            super(client, Type.GROUP_CALL);
            this.data = Objects.requireNonNull(data);
        }

        public InputGroupCall getCall() {
            return data.call();
        }

        public Optional<Duration> getDuration() {
            return Optional.ofNullable(data.duration()).map(Duration::ofSeconds);
        }

        @Override
        public String toString() {
            return "GroupCall{" +
                    "data=" + data +
                    '}';
        }
    }

    public static final class GroupCallScheduled extends MessageAction {

        private final MessageActionGroupCallScheduled data;

        public GroupCallScheduled(MTProtoTelegramClient client, MessageActionGroupCallScheduled data) {
            super(client, Type.GROUP_CALL_SCHEDULED);
            this.data = Objects.requireNonNull(data);
        }

        public InputGroupCall getCall() {
            return data.call();
        }

        public Instant getScheduleTimestamp() {
            return Instant.ofEpochSecond(data.scheduleDate());
        }

        @Override
        public String toString() {
            return "GroupCallScheduled{" +
                    "data=" + data +
                    '}';
        }
    }

    public static final class InviteToGroupCall extends MessageAction {

        private final MessageActionInviteToGroupCall data;

        public InviteToGroupCall(MTProtoTelegramClient client, MessageActionInviteToGroupCall data) {
            super(client, Type.INVITE_TO_GROUP_CALL);
            this.data = Objects.requireNonNull(data);
        }

        public InputGroupCall getCall() {
            return data.call();
        }

        public Set<Id> getUserIds() {
            return data.users().stream()
                    .map(Id::ofUser)
                    .collect(Collectors.toSet());
        }

        @Override
        public String toString() {
            return "InviteToGroupCall{" +
                    "data=" + data +
                    '}';
        }
    }

    public static final class PaymentSent extends MessageAction {

        private final MessageActionPaymentSent data;

        public PaymentSent(MTProtoTelegramClient client, MessageActionPaymentSent data) {
            super(client, Type.PAYMENT_SENT);
            this.data = Objects.requireNonNull(data);
        }

        public String getCurrency() {
            return data.currency();
        }

        public long getTotalAmount() {
            return data.totalAmount();
        }

        @Override
        public String toString() {
            return "PaymentSent{" +
                    "data=" + data +
                    '}';
        }
    }

    public static final class PaymentSentMe extends MessageAction {

        private final MessageActionPaymentSentMe data;

        public PaymentSentMe(MTProtoTelegramClient client, MessageActionPaymentSentMe data) {
            super(client, Type.PAYMENT_SENT);
            this.data = Objects.requireNonNull(data);
        }

        public String getCurrency() {
            return data.currency();
        }

        public long getTotalAmount() {
            return data.totalAmount();
        }

        public ByteBuf getPayload() {
            return data.payload();
        }

        public Optional<PaymentRequestedInfo> getInfo() {
            return Optional.ofNullable(data.info());
        }

        public Optional<String> getShippingOptionId() {
            return Optional.ofNullable(data.shippingOptionId());
        }

        public PaymentCharge getCharge() {
            return data.charge();
        }

        @Override
        public String toString() {
            return "PaymentSentMe{" +
                    "data=" + data +
                    '}';
        }
    }

    public static final class PhoneCall extends MessageAction {

        private final MessageActionPhoneCall data;

        public PhoneCall(MTProtoTelegramClient client, MessageActionPhoneCall data) {
            super(client, Type.PHONE_CALL);
            this.data = Objects.requireNonNull(data);
        }

        public boolean isVideo() {
            return data.video();
        }

        public long getCallId() {
            return data.callId();
        }

        public Optional<PhoneCallDiscardReason> reason() {
            return Optional.ofNullable(data.reason());
        }

        public Optional<Duration> getDuration() {
            return Optional.ofNullable(data.duration()).map(Duration::ofSeconds);
        }

        @Override
        public String toString() {
            return "PhoneCall{" +
                    "data=" + data +
                    '}';
        }
    }

    public static final class SecureValuesSent extends MessageAction {

        private final MessageActionSecureValuesSent data;

        public SecureValuesSent(MTProtoTelegramClient client, MessageActionSecureValuesSent data) {
            super(client, Type.SECURE_VALUES_SENT);
            this.data = Objects.requireNonNull(data);
        }

        public Set<SecureValueType> getTypes() {
            var set = EnumSet.noneOf(SecureValueType.class);
            set.addAll(data.types());
            return set;
        }

        @Override
        public String toString() {
            return "SecureValuesSent{" +
                    "data=" + data +
                    '}';
        }
    }

    public static final class SecureValuesSentMe extends MessageAction {

        private final MessageActionSecureValuesSentMe data;

        public SecureValuesSentMe(MTProtoTelegramClient client, MessageActionSecureValuesSentMe data) {
            super(client, Type.SECURE_VALUES_SENT_ME);
            this.data = Objects.requireNonNull(data);
        }

        public List<SecureValue> getValues() {
            return data.values();
        }

        public SecureCredentialsEncrypted getCredentials() {
            return data.credentials();
        }

        @Override
        public String toString() {
            return "SecureValuesSentMe{" +
                    "data=" + data +
                    '}';
        }
    }

    public static final class SetChatTheme extends MessageAction {

        private final MessageActionSetChatTheme data;

        public SetChatTheme(MTProtoTelegramClient client, MessageActionSetChatTheme data) {
            super(client, Type.SET_CHAT_THEME);
            this.data = Objects.requireNonNull(data);
        }

        /**
         * Gets unicode emoji of dice.
         *
         * @return The unicode emoji of dice.
         */
        public String getCurrentEmoticon() {
            return data.emoticon();
        }

        @Override
        public String toString() {
            return "SetChatTheme{" +
                    "data=" + data +
                    '}';
        }
    }

    public static final class SetMessagesTtl extends MessageAction {

        private final MessageActionSetMessagesTTL data;

        public SetMessagesTtl(MTProtoTelegramClient client, MessageActionSetMessagesTTL data) {
            super(client, Type.SET_MESSAGES_TTL);
            this.data = Objects.requireNonNull(data);
        }

        public Duration getCurrentDuration() {
            return Duration.ofSeconds(data.period());
        }

        /**
         * Gets id of user from which default setting was automatically applied, if present.
         *
         * @return The id of user from which default setting was automatically applied, if present.
         */
        public Optional<Id> getAutoSettingFromUserId() {
            return Optional.ofNullable(data.autoSettingFrom()).map(Id::ofUser);
        }

        /**
         * Requests to retrieve user from which default setting was automatically applied.
         *
         * @return A {@link Mono} emitting on successful completion user.
         */
        public Mono<User> getAutoSettingFromUser() {
            return getAutoSettingFromUser(MappingUtil.IDENTITY_RETRIEVER);
        }

        /**
         * Requests to retrieve user from which default setting was automatically applied
         * using specified retrieval strategy.
         *
         * @return A {@link Mono} emitting on successful completion user.
         */
        public Mono<User> getAutoSettingFromUser(EntityRetrievalStrategy strategy) {
            return Mono.justOrEmpty(getAutoSettingFromUserId())
                    .flatMap(client.withRetrievalStrategy(strategy)::getUserById);
        }

        @Override
        public String toString() {
            return "SetMessagesTtl{" +
                    "data=" + data +
                    '}';
        }
    }

    /** Service messages representing creation of new channel topic. */
    public static final class TopicCreate extends MessageAction {
        // from https://github.com/telegramdesktop/tdesktop/blob/55fd9c50912b127bf782765f23a1b31569e53cbe/Telegram/SourceFiles/data/data_forum_topic.cpp#L47
        /** Topic icon color in RGB format looking like this: <span style="color: #6FB9F0">■</span>. */
        public static final int BLUE = 0x6FB9F0;
        /** Topic icon color in RGB format looking like this: <span style="color: #FFD67E">■</span>. */
        public static final int YELLOW = 0xFFD67E;
        /** Topic icon color in RGB format looking like this: <span style="color: #CB86DB">■</span>. */
        public static final int VIOLET = 0xCB86DB;
        /** Topic icon color in RGB format looking like this: <span style="color: #8EEE98">■</span>. */
        public static final int GREEN = 0x8EEE98;
        /** Topic icon color in RGB format looking like this: <span style="color: #FF93B2">■</span>. */
        public static final int ROSE = 0xFF93B2;
        /** Topic icon color in RGB format looking like this: <span style="color: #FB6F5F">■</span>. */
        public static final int RED = 0xFB6F5F;

        private final MessageActionTopicCreate data;

        public TopicCreate(MTProtoTelegramClient client, MessageActionTopicCreate data) {
            super(client, Type.TOPIC_CREATE);
            this.data = data;
        }

        /**
         * Gets title of topic.
         *
         * @return The title of topic.
         */
        public String getTitle() {
            return data.title();
        }

        /**
         * Gets color of the topic in RGB format, currently only one of constants in this type.
         *
         * @return The color of the topic in RGB format.
         */
        public int getIconColor() {
            return data.iconColor();
        }

        /**
         * Gets id of {@link Sticker custom emoji} which used as topic icon, if present.
         *
         * @return The id of {@link Sticker custom emoji} which used as topic icon, if present.
         */
        public Optional<Long> getIconEmojiId() {
            return Optional.ofNullable(data.iconEmojiId());
        }

        /**
         * Requests to retrieve {@link Sticker custom emoji} which used as topic icon.
         *
         * @return A {@link Mono} emitting on successful completion {@link Sticker custom emoji}.
         */
        public Mono<Sticker> getIconEmoji() {
            return Mono.justOrEmpty(getIconEmojiId())
                    .flatMap(client::getCustomEmoji);
        }
    }

    /** Service messages representing edition of channel topic. */
    public static final class TopicEdit extends MessageAction {

        private final MessageActionTopicEdit data;

        public TopicEdit(MTProtoTelegramClient client, MessageActionTopicEdit data) {
            super(client, Type.TOPIC_EDIT);
            this.data = data;
        }

        /**
         * Gets new title for topic, if changed.
         *
         * @return A new title for topic, if changed.
         */
        public Optional<String> getTitle() {
            return Optional.ofNullable(data.title());
        }

        /**
         * Gets id of {@link Sticker custom emoji} which used as topic icon, if changed.
         *
         * @return The id of {@link Sticker custom emoji} which used as topic icon, if changed.
         */
        public Optional<Long> getIconEmojiId() {
            return Optional.ofNullable(data.iconEmojiId());
        }

        /**
         * Requests to retrieve {@link Sticker custom emoji} which used as topic icon.
         *
         * @return A {@link Mono} emitting on successful completion {@link Sticker custom emoji}.
         */
        public Mono<Sticker> getIconEmoji() {
            return Mono.justOrEmpty(getIconEmojiId())
                    .flatMap(client::getCustomEmoji);
        }

        /**
         * Gets whether topic is closed or reopened, if changed.
         *
         * @return {@code true} if closed, {@code false} if reopened and {@code null} if there is no changes.
         */
        public Optional<Boolean> isClosed() {
            return Optional.ofNullable(data.closed());
        }

        // TODO: docs
        public Optional<Boolean> isHidden() {
            return Optional.ofNullable(data.hidden());
        }
    }

    public static final class SuggestProfilePhoto extends MessageAction {
        private final MessageActionSuggestProfilePhoto data;
        private final MessageActionContext context;

        public SuggestProfilePhoto(MTProtoTelegramClient client,
                                   MessageActionSuggestProfilePhoto data,
                                   MessageActionContext context) {
            super(client, Type.SUGGEST_PROFILE_PHOTO);
            this.data = data;
            this.context = context;
        }

        public Photo getPhoto() {
            return new Photo(client, (BasePhoto) data.photo(), context);
        }
    }

    public static final class RequestedPeer extends MessageAction {
        private final MessageActionRequestedPeer data;

        public RequestedPeer(MTProtoTelegramClient client, MessageActionRequestedPeer data) {
            super(client, Type.REQUESTED_PEER);
            this.data = data;
        }

        /**
         * Gets custom id of button by which requested this peer.
         *
         * @see KeyboardButton#getButtonId()
         * @return The id of button by which requested this peer.
         */
        public int getButtonId() {
            return data.buttonId();
        }

        /**
         * Gets id of selected peer.
         *
         * @return The id of selected peer.
         */
        public Id getPeerId() {
            return Id.of(data.peer());
        }

        /**
         * Requests to retrieve selected peer.
         *
         * @return An {@link Mono} emitting on successful completion the {@link PeerEntity}.
         */
        public Mono<PeerEntity> getPeer() {
            return getPeer(MappingUtil.IDENTITY_RETRIEVER);
        }

        /**
         * Requests to retrieve selected peer using specified retrieval strategy.
         *
         * @param strategy The strategy to apply.
         * @return An {@link Mono} emitting on successful completion the {@link PeerEntity}.
         */
        public Mono<PeerEntity> getPeer(EntityRetrievalStrategy strategy) {
            return Mono.defer(() -> {
                Id id = getPeerId();
                var retriever = client.withRetrievalStrategy(strategy);
                return switch (id.getType()) {
                    case CHAT, CHANNEL -> retriever.getChatById(id);
                    case USER -> retriever.getUserById(id);
                };
            });
        }
    }
}
