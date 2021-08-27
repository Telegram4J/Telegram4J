package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.PollAnswerData;

import java.util.List;
import java.util.Objects;

public class PollAnswer implements TelegramObject {

    private final TelegramClient client;
    private final PollAnswerData data;

    public PollAnswer(TelegramClient client, PollAnswerData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public PollAnswerData getData() {
        return data;
    }

    public String getPollId() {
        return data.pollId();
    }

    public User getUser() {
        return new User(client, data.user());
    }

    public List<Integer> getOptionIds() {
        return data.optionIds();
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PollAnswer that = (PollAnswer) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        return "PollAnswer{data=" + data + '}';
    }
}
