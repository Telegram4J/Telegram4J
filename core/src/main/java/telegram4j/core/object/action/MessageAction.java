package telegram4j.core.object.action;

import telegram4j.core.object.TelegramObject;
import telegram4j.tl.MessageMediaGeoLive;

public interface MessageAction extends TelegramObject {

    Type getType();

    enum Type {
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

        /** Indicates the chat was {@link telegram4j.tl.Channel migrated} to the specified supergroup. */
        CHAT_MIGRATE_TO,

        /** Indicates the channel was {@link telegram4j.tl.Channel migrated} from the specified chat. */
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
        SET_CHAT_THEME;
    }
}
