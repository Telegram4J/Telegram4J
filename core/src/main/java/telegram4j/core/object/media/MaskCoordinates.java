package telegram4j.core.object.media;

import java.util.Objects;

public final class MaskCoordinates {
    private final telegram4j.tl.MaskCoords data;

    public MaskCoordinates(telegram4j.tl.MaskCoords data) {
        this.data = Objects.requireNonNull(data);
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
    public String toString() {
        return "MaskCoordinates{" +
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
