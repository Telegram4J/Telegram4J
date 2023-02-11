package telegram4j.core.event.domain;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.domain.chat.ChatEvent;
import telegram4j.core.event.domain.inline.BotEvent;
import telegram4j.core.event.domain.message.MessageEvent;

import java.util.Objects;

/** General interface of Telegram API events. */
public abstract sealed class Event
        permits ChatEvent, BotEvent, MessageEvent {

    protected final MTProtoTelegramClient client;

    protected Event(MTProtoTelegramClient client) {
        this.client = Objects.requireNonNull(client);
    }

    /**
     * Gets client that applied this event.
     *
     * @return The client applying this event.
     */
    public final MTProtoTelegramClient getClient() {
        return client;
    }
}
