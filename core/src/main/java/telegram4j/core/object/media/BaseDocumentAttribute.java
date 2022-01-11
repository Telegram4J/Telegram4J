package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

public class BaseDocumentAttribute implements DocumentAttribute {

    protected final MTProtoTelegramClient client;
    protected final Type type;

    public BaseDocumentAttribute(MTProtoTelegramClient client, Type type) {
        this.client = client;
        this.type = type;
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
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseDocumentAttribute that = (BaseDocumentAttribute) o;
        return type == that.type;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
        return "BaseDocumentAttribute{" +
                "type=" + type +
                '}';
    }
}
