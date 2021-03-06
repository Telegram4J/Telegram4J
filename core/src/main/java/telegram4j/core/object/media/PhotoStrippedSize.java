package telegram4j.core.object.media;

import io.netty.buffer.ByteBuf;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.mtproto.util.TlEntityUtil;

import java.util.Objects;

public class PhotoStrippedSize implements PhotoSize {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.PhotoStrippedSize data;

    public PhotoStrippedSize(MTProtoTelegramClient client, telegram4j.tl.PhotoStrippedSize data) {
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

    public ByteBuf getStrippedContent() {
        return data.bytes();
    }

    public ByteBuf getContent() {
        return TlEntityUtil.expandInlineThumb(data.bytes());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhotoStrippedSize that = (PhotoStrippedSize) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "PhotoStrippedSize{" +
                "data=" + data +
                '}';
    }
}
