package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.InlineKeyboardButtonData;

import java.util.Objects;
import java.util.Optional;

public class InlineKeyboardButton implements TelegramObject {

    private final TelegramClient client;
    private final InlineKeyboardButtonData data;

    public InlineKeyboardButton(TelegramClient client, InlineKeyboardButtonData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
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
        return data.loginUrl().map(data -> new LoginUrl(client, data));
    }

    // TODO: rename?
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
    public TelegramClient getClient() {
        return client;
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
