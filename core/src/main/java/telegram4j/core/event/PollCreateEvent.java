package telegram4j.core.event;

import telegram4j.core.TelegramClient;
import telegram4j.core.object.Poll;

public class PollCreateEvent extends Event {

    private final Poll poll;

    public PollCreateEvent(TelegramClient client, Poll poll) {
        super(client);
        this.poll = poll;
    }

    public Poll getPoll() {
        return poll;
    }

    @Override
    public String toString() {
        return "PollCreateEvent{" +
                "poll=" + poll +
                "} " + super.toString();
    }
}
