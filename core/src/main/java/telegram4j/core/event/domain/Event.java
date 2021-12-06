package telegram4j.core.event.domain;

import telegram4j.core.MTProtoTelegramClient;

public abstract class Event {

    protected final MTProtoTelegramClient client;

    protected Event(MTProtoTelegramClient client) {
        this.client = client;
    }

    public MTProtoTelegramClient getClient() {
        return client;
    }
}
