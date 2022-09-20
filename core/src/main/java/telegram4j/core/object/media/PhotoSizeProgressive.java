package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;

import java.util.List;
import java.util.Objects;

public class PhotoSizeProgressive implements PhotoSize {

    private final telegram4j.tl.PhotoSizeProgressive data;

    public PhotoSizeProgressive(telegram4j.tl.PhotoSizeProgressive data) {
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

    public List<Integer> getSizes() {
        return data.sizes();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhotoSizeProgressive that = (PhotoSizeProgressive) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "PhotoSizeProgressive{" +
                "data=" + data +
                '}';
    }
}
