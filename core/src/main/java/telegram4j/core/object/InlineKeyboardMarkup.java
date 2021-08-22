package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.InlineKeyboardMarkupData;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class InlineKeyboardMarkup implements TelegramObject {

    private final TelegramClient client;
    private final InlineKeyboardMarkupData data;

    public InlineKeyboardMarkup(TelegramClient client, InlineKeyboardMarkupData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public InlineKeyboardMarkupData getData() {
        return data;
    }

    public List<InlineKeyboardButton> getInlineKeyboard() {
        return data.inlineKeyboard().stream()
                .map(data -> new InlineKeyboardButton(client, data))
                .collect(Collectors.toList());
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InlineKeyboardMarkup that = (InlineKeyboardMarkup) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        return "InlineKeyboardMarkup{data=" + data + '}';
    }
}
