package telegram4j.core.event.domain.inline;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.domain.Event;
import telegram4j.core.object.User;

public abstract class BotEvent extends Event {

    public BotEvent(MTProtoTelegramClient client) {
        super(client);
    }

    public abstract long getQueryId();

    public abstract User getUser();
}
