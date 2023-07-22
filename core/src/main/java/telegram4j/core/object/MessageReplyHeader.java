package telegram4j.core.object;

import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;

/** Reply information header. */
public abstract sealed class MessageReplyHeader implements TelegramObject
        permits MessageReplyToMessageHeader, MessageReplyToStoryHeader {

    protected final MTProtoTelegramClient client;

    protected MessageReplyHeader(MTProtoTelegramClient client) {
        this.client = Objects.requireNonNull(client);
    }

    @Override
    public final MTProtoTelegramClient getClient() {
        return client;
    }
}
