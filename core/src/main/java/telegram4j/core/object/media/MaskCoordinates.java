package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.TelegramObject;

import java.util.Objects;

public class MaskCoordinates implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.MaskCoords data;

    public MaskCoordinates(MTProtoTelegramClient client, telegram4j.tl.MaskCoords data) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    public Type getType() {
        return Type.ALL[data.n()];
    }

    public double getX() {
        return data.x();
    }

    public double getY() {
        return data.y();
    }

    public double getZoom() {
        return data.zoom();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MaskCoordinates that = (MaskCoordinates) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MaskCoords{" +
                "data=" + data +
                '}';
    }

    public enum Type {
        FOREHEAD,
        EYES,
        MOUTH,
        CHIN;

        static final Type[] ALL = values();
    }
}
