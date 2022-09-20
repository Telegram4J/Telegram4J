package telegram4j.core.object.media;

import io.netty.buffer.ByteBuf;
import reactor.util.annotation.Nullable;

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

    public int getWight() {
        return data.w();
    }

    public int getHeight() {
        return data.h();
    }

    public ByteBuf getContent() {
        return data.bytes();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhotoCachedSize that = (PhotoCachedSize) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "PhotoCachedSize{" +
                "data=" + data +
                '}';
    }
}
