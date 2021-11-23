package telegram4j.core.event.domain.message;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.Event;

public abstract class MessageEvent extends Event {

    protected MessageEvent(MTProtoTelegramClient client) {
        super(client);
    }
}
