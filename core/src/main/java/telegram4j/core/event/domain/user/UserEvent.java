package telegram4j.core.event.domain.user;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.domain.Event;

public abstract class UserEvent extends Event {

    protected UserEvent(MTProtoTelegramClient client) {
        super(client);
    }
}
