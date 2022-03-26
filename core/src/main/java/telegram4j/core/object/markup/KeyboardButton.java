package telegram4j.core.object.markup;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Id;
import telegram4j.core.object.TelegramObject;
import telegram4j.tl.*;

import java.util.Objects;
import java.util.Optional;

/**
 * Representation of various types of markup button.
 *
 * @see telegram4j.core.spec.MessageFields.KeyboardButtonSpec
 */
public final class KeyboardButton implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.KeyboardButton data;

    public KeyboardButton(MTProtoTelegramClient client, telegram4j.tl.KeyboardButton data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets type of button.
     *
     * @return The {@link Type} of button.
     */
    public Type getType() {
        return Type.of(data);
    }

    /**
     * Gets text of button.
     *
     * @return The text of button.
     */
    public String getText() {
        return data.text();
    }

    /**
     * Gets callback data, if {@link #getType() type} is {@link Type#CALLBACK}.
     *
     * @return The callback data, if {@link #getType() type} is {@link Type#CALLBACK}.
     */
    public Optional<byte[]> getData() {
        return data.identifier() == KeyboardButtonCallback.ID
                ? Optional.of(((KeyboardButtonCallback) data).data())
                : Optional.empty();
    }

    /**
     * Gets whether button allows creating quiz polls, if {@link #getType() type} is {@link Type#REQUEST_POLL}.
     *
     * @return {@code true} if button allows creating and if {@link #getType() type} is {@link Type#REQUEST_POLL}.
     */
    public Optional<Boolean> isQuiz() {
        return data.identifier() == KeyboardButtonRequestPoll.ID
                ? Optional.ofNullable(((KeyboardButtonRequestPoll) data).quiz())
                : Optional.empty();
    }

    /**
     * Gets inline query, if {@link #getType() type} is {@link Type#SWITCH_INLINE}.
     *
     * @return The inline query, if {@link #getType() type} is {@link Type#SWITCH_INLINE}.
     */
    public Optional<String> getQuery() {
        return data.identifier() == KeyboardButtonSwitchInline.ID
                ? Optional.of(((KeyboardButtonSwitchInline) data).query())
                : Optional.empty();
    }

    /**
     * Gets whether button uses current chat/channel for inline query, if {@link #getType() type} is {@link Type#SWITCH_INLINE}.
     *
     * @return {@code true} if button uses current chat/channel for inline query and if {@link #getType() type} is {@link Type#SWITCH_INLINE}.
     */
    public Optional<Boolean> isSamePeer() {
        return data.identifier() == KeyboardButtonSwitchInline.ID
                ? Optional.of(((KeyboardButtonSwitchInline) data).samePeer())
                : Optional.empty();
    }

    /**
     * Gets url that will be opened on press, if {@link #getType() type} is {@link Type#URL}/{@link Type#URL_AUTH}.
     *
     * @return The url that will be opened on press, if {@link #getType() type} is {@link Type#URL}/{@link Type#URL_AUTH}.
     */
    public Optional<String> getUrl() {
        switch (data.identifier()) {
            case KeyboardButtonUrl.ID: return Optional.of(((KeyboardButtonUrl) data).url());
            case KeyboardButtonUrlAuth.ID: return Optional.of(((KeyboardButtonUrlAuth) data).url());
            case InputKeyboardButtonUrlAuth.ID: return Optional.of(((InputKeyboardButtonUrlAuth) data).url());
            default: return Optional.empty();
        }
    }

    /**
     * Gets text that will be displayed in forwarded messages, if {@link #getType() type} is {@link Type#URL_AUTH}.
     *
     * @return The text that will be displayed in forwarded messages, if {@link #getType() type} is {@link Type#URL_AUTH}.
     */
    public Optional<String> getForwardText() {
        return data.identifier() == KeyboardButtonUrlAuth.ID
                ? Optional.ofNullable(((KeyboardButtonUrlAuth) data).fwdText())
                : Optional.empty();
    }

    /**
     * Gets id of login button, if {@link #getType() type} is {@link Type#URL_AUTH}.
     *
     * @return The id of login button, if {@link #getType() type} is {@link Type#URL_AUTH}.
     */
    public Optional<Integer> getButtonId() {
        return data.identifier() == KeyboardButtonUrlAuth.ID
                ? Optional.of(((KeyboardButtonUrlAuth) data).buttonId())
                : Optional.empty();
    }

    /**
     * Gets whether bot requests the permission to send messages to user, if {@link #getType() type} is {@link Type#URL_AUTH}.
     *
     * @return {@code true} if bot requests the permission to send messages to user and if {@link #getType() type} is {@link Type#URL_AUTH}.
     */
    public Optional<Boolean> isRequestWriteAccess() {
        return data.identifier() == InputKeyboardButtonUrlAuth.ID
                ? Optional.of(((InputKeyboardButtonUrlAuth) data).requestWriteAccess())
                : Optional.empty();
    }

    /**
     * Gets whether user should verify his identity by password, if {@link #getType() type} is {@link Type#CALLBACK}.
     *
     * @return {@code true} if user should verify his identity by password and if {@link #getType() type} is {@link Type#CALLBACK}.
     */
    public Optional<Boolean> isRequiresPassword() {
        return data.identifier() == KeyboardButtonCallback.ID
                ? Optional.of(((KeyboardButtonCallback) data).requiresPassword())
                : Optional.empty();
    }

    /**
     * Gets id of user their profile will be opened on press, if {@link #getType() type} is {@link Type#URL_AUTH}.
     *
     * @return The id of user their profile will be opened on press, if {@link #getType() type} is {@link Type#URL_AUTH}.
     */
    public Optional<Id> getBotId() {
        if (data.identifier() == InputKeyboardButtonUrlAuth.ID) {
            var inputUser = ((InputKeyboardButtonUrlAuth) data).bot();
            return Optional.of(Id.of(inputUser, client.getSelfId()));
        }
        return Optional.empty();
    }

    /**
     * Gets id of user their profile will be opened on press, if {@link #getType() type} is {@link Type#USER_PROFILE}.
     *
     * @return The id of user their profile will be opened on press, if {@link #getType() type} is {@link Type#USER_PROFILE}.
     */
    public Optional<Id> getUserId() {
        switch (data.identifier()) {
            case KeyboardButtonUserProfile.ID:
                return Optional.of(Id.ofUser(((KeyboardButtonUserProfile) data).userId(), null));
            case InputKeyboardButtonUserProfile.ID:
                return Optional.of(Id.of(((InputKeyboardButtonUserProfile) data).userId(), client.getSelfId()));
            default:
                return Optional.empty();
        }
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyboardButton that = (KeyboardButton) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "KeyboardButton{" +
                "data=" + data +
                '}';
    }

    public enum Type {
        /** Bot keyboard button. */
        DEFAULT,

        /** Button to buy a product. */
        BUY,

        /** Callback button. */
        CALLBACK,

        /** Button to start a game. */
        GAME,

        /** Button to request a user's geolocation. */
        REQUEST_GEO_LOCATION,

        /** Button to request a user's phone number. */
        REQUEST_PHONE,

        /**
         * A button that allows the user to create and
         * send a poll when pressed; available only in private.
         */
        REQUEST_POLL,

        /**
         * Button to force a user to switch to inline mode Pressing the button
         * will prompt the user to select one of their chats, open that chat and
         * insert the bot‘s username and the specified inline query in the input field.
         */
        SWITCH_INLINE,

        /** URL embedded button. */
        URL,

        USER_PROFILE,

        /**
         * Button to request a user to authorize via URL
         * using <a href="https://telegram.org/blog/privacy-discussions-web-bots#meet-seamless-web-bots">Seamless Telegram Login</a>.
         * When the user clicks on such a button, {@link telegram4j.tl.request.messages.RequestUrlAuth}
         * should be called, providing the {@link KeyboardButtonUrlAuth#buttonId()} and the ID of the container message.
         * The returned {@link UrlAuthResultRequest} object will contain more details
         * about the authorization request ({@link UrlAuthResultRequest#requestWriteAccess()}
         * if the bot would like to send messages to the user along with the username of
         * the bot which will be used for user authorization).
         * Finally, the user can choose to call {@link telegram4j.tl.request.messages.AcceptUrlAuth} to
         * get a {@link UrlAuthResultAccepted} with the URL to open instead of the {@code url}
         * of this constructor, or a {@link UrlAuthResultDefault}, in which case the {@code url}
         * of this constructor must be opened, instead. If the user refuses the
         * authorization request but still wants to open the link, the {@code url} of this constructor must be used.
         */
        URL_AUTH;

        /**
         * Gets type of raw {@link telegram4j.tl.KeyboardButton} object.
         * Input button also will be handled but as resolved objects.
         *
         * @param data The button data.
         * @return The {@code Type} of raw button object.
         */
        public static Type of(telegram4j.tl.KeyboardButton data) {
            switch (data.identifier()) {
                case BaseKeyboardButton.ID: return DEFAULT;
                case InputKeyboardButtonUrlAuth.ID:
                case KeyboardButtonUrlAuth.ID: return URL_AUTH;
                case InputKeyboardButtonUserProfile.ID:
                case KeyboardButtonUserProfile.ID: return USER_PROFILE;
                case KeyboardButtonBuy.ID: return BUY;
                case KeyboardButtonCallback.ID: return CALLBACK;
                case KeyboardButtonGame.ID: return GAME;
                case KeyboardButtonRequestGeoLocation.ID: return REQUEST_GEO_LOCATION;
                case KeyboardButtonRequestPhone.ID: return REQUEST_PHONE;
                case KeyboardButtonRequestPoll.ID: return REQUEST_POLL;
                case KeyboardButtonSwitchInline.ID: return SWITCH_INLINE;
                case KeyboardButtonUrl.ID: return URL;
                default: throw new IllegalStateException("Unexpected keyboard button type: " + data);
            }
        }
    }
}
