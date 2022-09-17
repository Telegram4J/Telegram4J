package telegram4j.core.util;

import reactor.util.annotation.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * A tuple which holds only one non-null value of specified types.
 *
 * @param <T1> The type of the first value
 * @param <T2> The type of the second value
 */
public class Variant2<T1, T2> {

    @Nullable
    private final T1 t1;
    @Nullable
    private final T2 t2;

    private Variant2(@Nullable T1 t1, @Nullable T2 t2) {
        this.t1 = t1;
        this.t2 = t2;
    }

    /**
     * Create a {@code Variant2} with the given value in first slot.
     *
     * @param t1 The first value in the tuple, not null
     * @param <T1> The type of the first value
     * @param <T2> The type of the second ({@code null}) value
     * @return The new {@code Variant2}.
     */
    public static <T1, T2> Variant2<T1, T2> ofT1(T1 t1) {
        Objects.requireNonNull(t1);
        return new Variant2<>(t1, null);
    }

    /**
     * Create a {@code Variant2} with the given value in second slot.
     *
     * @param t2 The second value in the tuple, not null
     * @param <T1> The type of the first ({@code null}) value
     * @param <T2> The type of the second value
     * @return The new {@code Variant2}.
     */
    public static <T1, T2> Variant2<T1, T2> ofT2(T2 t2) {
        Objects.requireNonNull(t2);
        return new Variant2<>(null, t2);
    }

    /**
     * Gets first value, if present.
     *
     * @return The first value, if present.
     */
    public Optional<T1> getT1() {
        return Optional.ofNullable(t1);
    }

    /**
     * Gets second value, if present.
     *
     * @return The second value, if present.
     */
    public Optional<T2> getT2() {
        return Optional.ofNullable(t2);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof Variant2)) return false;
        Variant2<?, ?> variant2 = (Variant2<?, ?>) o;
        return Objects.equals(t1, variant2.t1) && Objects.equals(t2, variant2.t2);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(t1) + Objects.hashCode(t2);
    }

    @Override
    public String toString() {
        return "Variant2{" + (t1 != null ? "t1=" + t1 : "t2=" + t2) + '}';
    }
}
