package telegram4j.core.event.domain;

import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;

/** General interface of Telegram API events. */
public abstract class Event {

    protected final MTProtoTelegramClient client;

    protected Event(MTProtoTelegramClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    public final MTProtoTelegramClient getClient() {
        return client;
    }
}
