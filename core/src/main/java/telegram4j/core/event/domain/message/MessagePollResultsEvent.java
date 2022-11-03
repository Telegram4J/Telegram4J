package telegram4j.core.event.domain.message;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.media.Poll;
import telegram4j.core.object.media.PollResults;

import java.util.Optional;

/** Event of poll results. */
public class MessagePollResultsEvent extends MessageEvent{

    private final @Nullable Poll poll;
    private final PollResults results;

    public MessagePollResultsEvent(MTProtoTelegramClient client, @Nullable Poll poll, PollResults results) {
        super(client);
        this.poll = poll;
        this.results = results;
    }

    /**
     * Gets poll object, if present.
     *
     * @return The poll object, if present.
     */
    public Optional<Poll> getPoll() {
        return Optional.ofNullable(poll);
    }

    /**
     * Gets results of poll.
     *
     * @return The results of poll.
     */
    public PollResults getResults() {
        return results;
    }

    @Override
    public String toString() {
        return "MessagePollResultsEvent{" +
                "poll=" + poll +
                ", results=" + results +
                '}';
    }
}
