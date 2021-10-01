package telegram4j.core.event;

import telegram4j.core.TelegramClient;
import telegram4j.core.object.InlineQuery;

public class InlineQueryCreateEvent extends Event {

    private final InlineQuery inlineQuery;

    public InlineQueryCreateEvent(TelegramClient client, InlineQuery inlineQuery) {
        super(client);
        this.inlineQuery = inlineQuery;
    }

    public InlineQuery getInlineQuery() {
        return inlineQuery;
    }

    @Override
    public String toString() {
        return "InlineQueryCreateEvent{" +
                "inlineQuery=" + inlineQuery +
                "} " + super.toString();
    }
}
