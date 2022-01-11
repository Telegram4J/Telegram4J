package telegram4j.core.object.media;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.poll.Poll;
import telegram4j.core.object.poll.PollResults;

import java.util.Objects;

public class MessageMediaPoll extends BaseMessageMedia {

    private final telegram4j.tl.MessageMediaPoll data;

    public MessageMediaPoll(MTProtoTelegramClient client, telegram4j.tl.MessageMediaPoll data) {
        super(client, Type.POLL);
        this.data = Objects.requireNonNull(data, "data");
    }

    public Poll getPoll() {
        return new Poll(client, data.poll());
    }

    public PollResults getResults() {
        return new PollResults(client, data.results());
    }
}
