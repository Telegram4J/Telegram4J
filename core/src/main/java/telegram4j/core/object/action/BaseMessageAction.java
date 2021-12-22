package telegram4j.core.object.action;

import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;

abstract class BaseMessageAction implements MessageAction {
    private final MTProtoTelegramClient client;
    private final Type type;

    protected BaseMessageAction(MTProtoTelegramClient client, Type type) {
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
        return "BaseMessageAction{" +
                "type=" + type +
                '}';
    }
}
