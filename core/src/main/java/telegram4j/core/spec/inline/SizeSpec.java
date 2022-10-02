package telegram4j.core.spec.inline;

import reactor.util.annotation.Nullable;
import telegram4j.core.spec.Spec;

public final class SizeSpec implements Spec {
    private final int width;
    private final int height;

    private SizeSpec(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public static SizeSpec of(int size) {
        return new SizeSpec(size, size);
    }

    public static SizeSpec of(int width, int height) {
        return new SizeSpec(width, height);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof SizeSpec)) return false;
        SizeSpec sizeSpec = (SizeSpec) o;
        return width == sizeSpec.width && height == sizeSpec.height;
    }

    @Override
    public int hashCode() {
        return height + 51 * height;
    }

    @Override
    public String toString() {
        return "SizeSpec{" +
                "width=" + width +
                ", height=" + height +
                '}';
    }
}
