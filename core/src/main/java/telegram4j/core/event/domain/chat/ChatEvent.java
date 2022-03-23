package telegram4j.core.event.domain.chat;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.domain.Event;

public abstract class ChatEvent extends Event {

    protected ChatEvent(MTProtoTelegramClient client) {
        super(client);
    }
}
