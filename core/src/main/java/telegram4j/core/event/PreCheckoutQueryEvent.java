package telegram4j.core.event;

import telegram4j.core.TelegramClient;
import telegram4j.core.object.PreCheckoutQuery;

public class PreCheckoutQueryEvent extends Event {

    private final PreCheckoutQuery preCheckoutQuery;

    public PreCheckoutQueryEvent(TelegramClient client, PreCheckoutQuery preCheckoutQuery) {
        super(client);
        this.preCheckoutQuery = preCheckoutQuery;
    }

    public PreCheckoutQuery getPreCheckoutQuery() {
        return preCheckoutQuery;
    }

    @Override
    public String toString() {
        return "PreCheckoutQueryEvent{" +
                "preCheckoutQuery=" + preCheckoutQuery +
                "} " + super.toString();
    }
}
