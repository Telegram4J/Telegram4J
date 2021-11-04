package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.json.PollOptionData;

import java.util.Objects;

public class PollOption implements TelegramObject {

    private final TelegramClient client;
    private final PollOptionData data;

    public PollOption(TelegramClient client, PollOptionData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public PollOptionData getData() {
        return data;
    }

    public String getText() {
        return data.text();
    }

    public int getVoteCount() {
        return data.voteCount();
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PollOption that = (PollOption) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "PollOption{data=" + data + '}';
    }
}
