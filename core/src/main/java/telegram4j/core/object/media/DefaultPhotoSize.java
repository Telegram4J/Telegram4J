package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;

import java.util.Objects;

public class DefaultPhotoSize implements PhotoSize {

    private final telegram4j.tl.BasePhotoSize data;

    public DefaultPhotoSize(telegram4j.tl.BasePhotoSize data) {
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

    public int getSize() {
        return data.size();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultPhotoSize that = (DefaultPhotoSize) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "DefaultPhotoSize{" +
                "data=" + data +
                '}';
    }
}
