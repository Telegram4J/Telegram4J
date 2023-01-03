package telegram4j.core.object.media;

import io.netty.buffer.ByteBuf;

import java.util.Objects;

public class PhotoCachedSize implements PhotoSize {

    private final telegram4j.tl.PhotoCachedSize data;

    public PhotoCachedSize(telegram4j.tl.PhotoCachedSize data) {
        this.data = Objects.requireNonNull(data);
    }

    @Override
    public char getType() {
        return data.type().charAt(0);
    }

    public int getWidth() {
        return data.w();
    }

    public int getHeight() {
        return data.h();
    }

    public ByteBuf getContent() {
        return data.bytes();
    }

    @Override
    public String toString() {
        return "PhotoCachedSize{" +
                "data=" + data +
                '}';
    }
}
