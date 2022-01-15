package telegram4j.core.object.markup;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.TelegramObject;

import java.util.Objects;

public abstract class ReplyMarkup implements TelegramObject {

    protected final MTProtoTelegramClient client;

    protected ReplyMarkup(MTProtoTelegramClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    @Override
    public String toString() {
        return "ReplyMarkup{}";
    }
}
