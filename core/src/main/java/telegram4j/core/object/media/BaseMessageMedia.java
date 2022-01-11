package telegram4j.core.object.media;

import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;

public class BaseMessageMedia implements MessageMedia {

    protected final MTProtoTelegramClient client;
    protected final Type type;

    public BaseMessageMedia(MTProtoTelegramClient client, Type type) {
        this.client = Objects.requireNonNull(client, "client");
        this.type = Objects.requireNonNull(type, "type");
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "BaseMessageMedia{" +
                "type=" + type +
                '}';
    }
}
