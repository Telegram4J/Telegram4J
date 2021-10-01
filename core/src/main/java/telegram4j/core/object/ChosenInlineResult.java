package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.ChosenInlineResultData;

import java.util.Objects;
import java.util.Optional;

public class ChosenInlineResult implements TelegramObject {

    private final TelegramClient client;
    private final ChosenInlineResultData data;

    public ChosenInlineResult(TelegramClient client, ChosenInlineResultData data) {
        this.client = client;
        this.data = data;
    }

    public ChosenInlineResultData getData() {
        return data;
    }

    public String getResultId() {
        return data.resultId();
    }

    public User getFromUser() {
        return new User(client, data.fromUser());
    }

    public Optional<Location> getLocation() {
        return data.location().map(data -> new Location(client, data));
    }

    public Optional<String> getInlineMessageId() {
        return data.inlineMessageId();
    }

    public String getQuery() {
        return data.query();
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChosenInlineResult that = (ChosenInlineResult) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        return "ChosenInlineResult{" +
                "data=" + data +
                '}';
    }
}
