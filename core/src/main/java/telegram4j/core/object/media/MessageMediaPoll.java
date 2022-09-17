package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.poll.Poll;
import telegram4j.core.object.poll.PollResults;

import java.util.Objects;

public class MessageMediaPoll extends BaseMessageMedia {

    private final telegram4j.tl.MessageMediaPoll data;

    public MessageMediaPoll(MTProtoTelegramClient client, telegram4j.tl.MessageMediaPoll data) {
        super(client, Type.POLL);
        this.data = Objects.requireNonNull(data);
    }

    /**
     * Gets information about poll.
     *
     * @return The {@link Poll} object.
     */
    public Poll getPoll() {
        return new Poll(client, data.poll());
    }

    /**
     * Gets information about poll results.
     *
     * @return The {@link PollResults} object.
     */
    public PollResults getResults() {
        return new PollResults(client, data.results());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageMediaPoll that = (MessageMediaPoll) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageMediaPoll{" +
                "data=" + data +
                '}';
    }
}
