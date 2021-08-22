package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.MessageAutoDeleteTimerChangedData;

import java.util.Objects;

public class MessageAutoDeleteTimerChanged implements TelegramObject {

    private final TelegramClient client;
    private final MessageAutoDeleteTimerChangedData data;

    public MessageAutoDeleteTimerChanged(TelegramClient client, MessageAutoDeleteTimerChangedData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public MessageAutoDeleteTimerChangedData getData() {
        return data;
    }

    public int getMessageAutoDeleteTime() {
        return data.messageAutoDeleteTime();
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageAutoDeleteTimerChanged that = (MessageAutoDeleteTimerChanged) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        return "MessageAutoDeleteTimerChanged{data=" + data + '}';
    }
}
