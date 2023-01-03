package telegram4j.core.object.media;

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
    public String toString() {
        return "DefaultPhotoSize{" +
                "data=" + data +
                '}';
    }
}
