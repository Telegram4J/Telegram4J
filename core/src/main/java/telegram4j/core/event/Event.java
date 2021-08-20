package telegram4j.core.event;

import telegram4j.TelegramClient;

import java.util.Objects;

public abstract class Event {

    private final TelegramClient client;

    protected Event(TelegramClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    public TelegramClient getClient() {
        return client;
    }
}
