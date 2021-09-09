package telegram4j.core.object.replymarkup;

import telegram4j.core.object.LoginUrl;
import telegram4j.json.InlineKeyboardButtonData;

import java.util.Objects;
import java.util.Optional;

public final class InlineKeyboardButton {

    private final InlineKeyboardButtonData data;

    public InlineKeyboardButton(InlineKeyboardButtonData data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    public static InlineKeyboardButton fromData(InlineKeyboardButtonData data) {
        return new InlineKeyboardButton(data);
    }

    public InlineKeyboardButtonData getData() {
        return data;
    }

    public String getText() {
        return data.text();
    }

    public Optional<String> getUrl() {
        return data.url();
    }

    public Optional<LoginUrl> getLoginUrl() {
        return data.loginUrl().map(LoginUrl::new);
    }

    public Optional<String> getCallbackData() {
        return data.callbackData();
    }

    public Optional<String> getSwitchInlineQuery() {
        return data.switchInlineQuery();
    }

    public Optional<String> getSwitchInlineQueryCurrentChat() {
        return data.switchInlineQueryCurrentChat();
    }

    // InlineKeyboardButtonData#callbackName not added because this is empty class

    public boolean isPay() {
        return data.pay();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InlineKeyboardButton that = (InlineKeyboardButton) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        return "InlineKeyboardButton{data=" + data + '}';
    }
}
