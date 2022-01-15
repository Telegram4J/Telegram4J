package telegram4j.core.object.markup;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Id;
import telegram4j.core.object.TelegramObject;
import telegram4j.tl.*;

import java.util.Objects;
import java.util.Optional;

public class KeyboardButton implements TelegramObject {

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

    public Type getType() {
        return Type.of(data);
    }

    public String getText() {
        return data.text();
    }

    public Optional<byte[]> getData() {
        return data.identifier() == KeyboardButtonCallback.ID
                ? Optional.of(((KeyboardButtonCallback) data).data())
                : Optional.empty();
    }

    public Optional<Boolean> isQuiz() {
        return data.identifier() == KeyboardButtonRequestPoll.ID
                ? Optional.ofNullable(((KeyboardButtonRequestPoll) data).quiz())
                : Optional.empty();
    }

    public Optional<String> getQuery() {
        return data.identifier() == KeyboardButtonSwitchInline.ID
                ? Optional.of(((KeyboardButtonSwitchInline) data).query())
                : Optional.empty();
    }

    public Optional<String> getUrl() {
        return data.identifier() == KeyboardButtonSwitchInline.ID
                ? Optional.of(((KeyboardButtonSwitchInline) data).query())
                : Optional.empty();
    }

    public Optional<String> getForwardText() {
        return data.identifier() == KeyboardButtonSwitchInline.ID
                ? Optional.of(((KeyboardButtonSwitchInline) data).query())
                : Optional.empty();
    }

    public Optional<Integer> getButtonId() {
        return data.identifier() == KeyboardButtonUrlAuth.ID
                ? Optional.of(((KeyboardButtonUrlAuth) data).buttonId())
                : Optional.empty();
    }

    public Optional<Boolean> isRequestWriteAccess() {
        return data.identifier() == InputKeyboardButtonUrlAuth.ID
                ? Optional.of(((InputKeyboardButtonUrlAuth) data).requestWriteAccess())
                : Optional.empty();
    }

    public Optional<Boolean> isRequiresPassword() {
        return data.identifier() == KeyboardButtonCallback.ID
                ? Optional.of(((KeyboardButtonCallback) data).requiresPassword())
                : Optional.empty();
    }

    public Optional<Id> getBotId() {
        if (data.identifier() == InputKeyboardButtonUrlAuth.ID) {
            var inputUser = ((BaseInputUser) ((InputKeyboardButtonUrlAuth) data).bot());
            return Optional.of(Id.ofUser(inputUser.userId(), inputUser.accessHash()));
        }
        return Optional.empty();
    }

    public Optional<Id> getUserId() {
        if (data.identifier() == InputKeyboardButtonUserProfile.ID) {
            var inputUser = ((BaseInputUser) ((InputKeyboardButtonUserProfile) data).userId());
            return Optional.of(Id.ofUser(inputUser.userId(), inputUser.accessHash()));
        }
        return Optional.empty();
    }

    public enum Type {
        DEFAULT,

        BUY,

        INPUT_URH_AUTH,

        CALLBACK,

        GAME,

        REQUEST_GEO_LOCATION,

        REQUEST_PHONE,

        REQUEST_POLL,

        SWITCH_INLINE,

        URL,

        USER_PROFILE,

        URL_AUTH;

        public static Type of(telegram4j.tl.KeyboardButton data) {
            switch (data.identifier()) {
                case BaseKeyboardButton.ID:
                    return DEFAULT;
                case KeyboardButtonUrlAuth.ID:
                case InputKeyboardButtonUrlAuth.ID:
                    return URL_AUTH;
                case KeyboardButtonUserProfile.ID:
                case InputKeyboardButtonUserProfile.ID:
                    return USER_PROFILE;
                case KeyboardButtonBuy.ID:
                    return BUY;
                case KeyboardButtonCallback.ID:
                    return CALLBACK;
                case KeyboardButtonGame.ID:
                    return GAME;
                case KeyboardButtonRequestGeoLocation.ID:
                    return REQUEST_GEO_LOCATION;
                case KeyboardButtonRequestPhone.ID:
                    return REQUEST_PHONE;
                case KeyboardButtonRequestPoll.ID:
                    return REQUEST_POLL;
                case KeyboardButtonSwitchInline.ID:
                    return SWITCH_INLINE;
                case KeyboardButtonUrl.ID:
                    return URL;
                default:
                    throw new IllegalStateException("Unexpected keyboard button type: " + data);
            }
        }
    }
}
