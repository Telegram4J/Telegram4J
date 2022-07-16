package telegram4j.core.object.media;

import io.netty.buffer.ByteBuf;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;

public class PhotoPathSize implements PhotoSize {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.PhotoPathSize data;

    public PhotoPathSize(MTProtoTelegramClient client, telegram4j.tl.PhotoPathSize data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    @Override
    public char getType() {
        return data.type().charAt(0);
    }

    public ByteBuf getContent() {
        return data.bytes();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhotoPathSize that = (PhotoPathSize) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "PhotoPathSize{" +
                "data=" + data +
                '}';
    }
}
