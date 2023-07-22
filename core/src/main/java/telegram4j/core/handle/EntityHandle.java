package telegram4j.core.handle;

import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;

public abstract class EntityHandle {
    protected final MTProtoTelegramClient client;

    protected EntityHandle(MTProtoTelegramClient client) {
        this.client = Objects.requireNonNull(client);
    }

    public final MTProtoTelegramClient getClient() {
        return client;
    }
}
