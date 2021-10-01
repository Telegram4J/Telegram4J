package telegram4j.core.event;

import telegram4j.core.TelegramClient;
import telegram4j.core.object.PollAnswer;

public class PollAnswerEvent extends Event {

    private final PollAnswer pollAnswer;

    public PollAnswerEvent(TelegramClient client, PollAnswer pollAnswer) {
        super(client);
        this.pollAnswer = pollAnswer;
    }

    public PollAnswer getPollAnswer() {
        return pollAnswer;
    }

    @Override
    public String toString() {
        return "PollAnswerEvent{" +
                "pollAnswer=" + pollAnswer +
                "} " + super.toString();
    }
}
