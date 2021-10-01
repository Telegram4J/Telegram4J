package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.CallbackQueryData;

import java.util.Objects;
import java.util.Optional;

public class CallbackQuery implements TelegramObject {

    private final TelegramClient client;
    private final CallbackQueryData data;

    public CallbackQuery(TelegramClient client, CallbackQueryData data) {
        this.client = client;
        this.data = data;
    }

    public CallbackQueryData getData() {
        return data;
    }

    public String getId() {
        return data.id();
    }

    public User getFromUser() {
        return new User(client, data.fromUser());
    }

    public Optional<Message> getMessage() {
        return data.message().map(data -> new Message(client, data));
    }

    public Optional<String> getInlineMessageId() {
        return data.inlineMessageId();
    }

    public String getChatInstance() {
        return data.chatInstance();
    }

    // TODO: change name
    public Optional<String> getCallbackData() {
        return data.data();
    }

    public Optional<String> getGameShortName() {
        return data.gameShortName();
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CallbackQuery that = (CallbackQuery) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        return "CallbackQuery{" +
                "client=" + client +
                ", data=" + data +
                '}';
    }
}
