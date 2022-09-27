package telegram4j.core.object;

import io.netty.buffer.ByteBuf;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.chat.Channel;
import telegram4j.core.util.Id;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class MessageAction implements TelegramObject {
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
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageAction that = (MessageAction) o;
        return type == that.type;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
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
        CHAT_ADD_USER,

        /** User left the group. */
        CHAT_DELETE_USER,

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

        /** The chat theme was changed. */
        SET_CHAT_THEME
    }

    public static class BotAllowed extends MessageAction {

        private final MessageActionBotAllowed data;

        public BotAllowed(MTProtoTelegramClient client, MessageActionBotAllowed data) {
            super(client, Type.BOT_ALLOWED);
            this.data = Objects.requireNonNull(data);
        }

        public String getDomain() {
            return data.domain();
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BotAllowed that = (BotAllowed) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "BotAllowed{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class ChannelCreate extends MessageAction {

        private final MessageActionChannelCreate data;

        public ChannelCreate(MTProtoTelegramClient client, MessageActionChannelCreate data) {
            super(client, Type.CHANNEL_CREATE);
            this.data = Objects.requireNonNull(data);
        }

        public String getChannelTitle() {
            return data.title();
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChannelCreate that = (ChannelCreate) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "ChannelCreate{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class ChannelMigrateFrom extends MessageAction {

        private final MessageActionChannelMigrateFrom data;

        public ChannelMigrateFrom(MTProtoTelegramClient client, MessageActionChannelMigrateFrom data) {
            super(client, Type.CHANNEL_MIGRATE_FROM);
            this.data = Objects.requireNonNull(data);
        }

        public String getChatTitle() {
            return data.title();
        }

        public Id getChatId() {
            return Id.ofChat(data.chatId());
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChannelMigrateFrom that = (ChannelMigrateFrom) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "ChannelMigrateFrom{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class ChatAddUser extends MessageAction {

        private final MessageActionChatAddUser data;

        public ChatAddUser(MTProtoTelegramClient client, MessageActionChatAddUser data) {
            super(client, Type.CHAT_ADD_USER);
            this.data = Objects.requireNonNull(data);
        }

        public List<Id> getUserIds() {
            return data.users().stream()
                    .map(l -> Id.ofUser(l, null))
                    .collect(Collectors.toList());
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChatAddUser that = (ChatAddUser) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "ChatAddUser{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class ChatCreate extends MessageAction {

        private final MessageActionChatCreate data;

        public ChatCreate(MTProtoTelegramClient client, MessageActionChatCreate data) {
            super(client, Type.CHAT_CREATE);
            this.data = Objects.requireNonNull(data);
        }

        public String getTitle() {
            return data.title();
        }

        public List<Id> getUserIds() {
            return data.users().stream()
                    .map(l -> Id.ofUser(l, null))
                    .collect(Collectors.toList());
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChatCreate that = (ChatCreate) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "ChatCreate{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class ChatDeleteUser extends MessageAction {

        private final MessageActionChatDeleteUser data;

        public ChatDeleteUser(MTProtoTelegramClient client, MessageActionChatDeleteUser data) {
            super(client, Type.CHAT_DELETE_USER);
            this.data = Objects.requireNonNull(data);
        }

        public Id getUserId() {
            return Id.ofUser(data.userId(), null);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChatDeleteUser that = (ChatDeleteUser) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "ChatDeleteUser{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class ChatEditPhoto extends MessageAction {

        @Nullable
        private final MessageActionChatEditPhoto data;
        private final InputPeer peer;
        private final int messageId;

        public ChatEditPhoto(MTProtoTelegramClient client) {
            super(client, Type.DELETE_CHAT_PHOTO);

            this.data = null;
            this.peer = InputPeerEmpty.instance();
            this.messageId = -1;
        }

        public ChatEditPhoto(MTProtoTelegramClient client, MessageActionChatEditPhoto data,
                             InputPeer peer, int messageId) {
            super(client, Type.EDIT_CHAT_PHOTO);
            this.data = Objects.requireNonNull(data);
            this.peer = Objects.requireNonNull(peer);
            this.messageId = messageId;
        }

        public Optional<Photo> getCurrentPhoto() {
            return Optional.ofNullable(data)
                    .map(d -> TlEntityUtil.unmapEmpty(d.photo(), BasePhoto.class))
                    .map(d -> new Photo(client, d, messageId, peer));
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            ChatEditPhoto that = (ChatEditPhoto) o;
            return Objects.equals(data, that.data);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), data);
        }

        @Override
        public String toString() {
            return "ChatEditPhoto{" +
                    "type=" + type +
                    ", data=" + data +
                    '}';
        }
    }

    public static class ChatEditTitle extends MessageAction {

        private final MessageActionChatEditTitle data;

        public ChatEditTitle(MTProtoTelegramClient client, MessageActionChatEditTitle data) {
            super(client, Type.CHAT_EDIT_TITLE);
            this.data = Objects.requireNonNull(data);
        }

        public String getCurrentTitle() {
            return data.title();
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChatEditTitle that = (ChatEditTitle) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "ChatEditTitle{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class ChatJoinedByLink extends MessageAction {

        private final MessageActionChatJoinedByLink data;

        public ChatJoinedByLink(MTProtoTelegramClient client, MessageActionChatJoinedByLink data) {
            super(client, Type.CHAT_JOINED_BY_LINK);
            this.data = Objects.requireNonNull(data);
        }

        public Id getInviterId() {
            return Id.ofUser(data.inviterId(), null);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChatJoinedByLink that = (ChatJoinedByLink) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "ChatJoinedByLink{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class ChatMigrateTo extends MessageAction {

        private final MessageActionChatMigrateTo data;

        public ChatMigrateTo(MTProtoTelegramClient client, MessageActionChatMigrateTo data) {
            super(client, Type.CHAT_MIGRATE_TO);
            this.data = Objects.requireNonNull(data);
        }

        public Id getChannelId() {
            return Id.ofChannel(data.channelId(), null);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChatMigrateTo that = (ChatMigrateTo) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "ChatMigrateTo{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class Custom extends MessageAction {

        private final MessageActionCustomAction data;

        public Custom(MTProtoTelegramClient client, MessageActionCustomAction data) {
            super(client, Type.CUSTOM);
            this.data = Objects.requireNonNull(data);
        }

        public String getMessage() {
            return data.message();
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Custom that = (Custom) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "Custom{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class GameScore extends MessageAction {

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
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GameScore that = (GameScore) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "GameScore{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class GeoProximityReached extends MessageAction {

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
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GeoProximityReached that = (GeoProximityReached) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "GeoProximityReached{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class GroupCall extends MessageAction {

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
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GroupCall that = (GroupCall) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "GroupCall{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class GroupCallScheduled extends MessageAction {

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
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GroupCallScheduled that = (GroupCallScheduled) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "GroupCallScheduled{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class InviteToGroupCall extends MessageAction {

        private final MessageActionInviteToGroupCall data;

        public InviteToGroupCall(MTProtoTelegramClient client, MessageActionInviteToGroupCall data) {
            super(client, Type.INVITE_TO_GROUP_CALL);
            this.data = Objects.requireNonNull(data);
        }

        public InputGroupCall getCall() {
            return data.call();
        }

        public List<Id> getUserIds() {
            return data.users().stream()
                    .map(l -> Id.ofUser(l, null))
                    .collect(Collectors.toList());
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InviteToGroupCall that = (InviteToGroupCall) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "InviteToGroupCall{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class PaymentSent extends MessageAction {

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
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PaymentSent that = (PaymentSent) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "PaymentSent{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class PaymentSentMe extends MessageAction {

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
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PaymentSentMe that = (PaymentSentMe) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "PaymentSentMe{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class PhoneCall extends MessageAction {

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
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PhoneCall that = (PhoneCall) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "PhoneCall{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class SecureValuesSent extends MessageAction {

        private final MessageActionSecureValuesSent data;

        public SecureValuesSent(MTProtoTelegramClient client, MessageActionSecureValuesSent data) {
            super(client, Type.SECURE_VALUES_SENT);
            this.data = Objects.requireNonNull(data);
        }

        public List<SecureValueType> getTypes() {
            return data.types();
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SecureValuesSent that = (SecureValuesSent) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "SecureValuesSent{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class SecureValuesSentMe extends MessageAction {

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
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SecureValuesSentMe that = (SecureValuesSentMe) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "SecureValuesSentMe{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class SetChatTheme extends MessageAction {

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
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SetChatTheme that = (SetChatTheme) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "SetChatTheme{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class SetMessagesTtl extends MessageAction {

        private final MessageActionSetMessagesTTL data;

        public SetMessagesTtl(MTProtoTelegramClient client, MessageActionSetMessagesTTL data) {
            super(client, Type.SET_MESSAGES_TTL);
            this.data = Objects.requireNonNull(data);
        }

        public Duration getCurrentDuration() {
            return Duration.ofSeconds(data.period());
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SetMessagesTtl that = (SetMessagesTtl) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "SetMessagesTtl{" +
                    "data=" + data +
                    '}';
        }
    }
}
