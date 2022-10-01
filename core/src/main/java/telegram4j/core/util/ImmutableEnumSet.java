package telegram4j.core.util;

import telegram4j.core.object.BitFlag;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

/** Immutable version of {@link EnumSet} which backed as {@code int}.  */
public final class ImmutableEnumSet<E extends Enum<E> & BitFlag> extends AbstractSet<E> {

    private final Class<E> elementType;
    private final int value;

    private ImmutableEnumSet(Class<E> elementType, int value) {
        this.elementType = elementType;
        this.value = value;
    }

    public static <E extends Enum<E> & BitFlag> ImmutableEnumSet<E> of(Class<E> type, Iterable<E> values) {
        if (values instanceof ImmutableEnumSet) {
            return (ImmutableEnumSet<E>) values;
        }

        Objects.requireNonNull(type);
        return new ImmutableEnumSet<>(type, StreamSupport.stream(values.spliterator(), false)
                .map(E::mask)
                .reduce(0, (l, r) -> l | r));
    }

    public static <E extends Enum<E> & BitFlag> ImmutableEnumSet<E> of(Class<E> type, int value) {
        Objects.requireNonNull(type);
        return new ImmutableEnumSet<>(type, value);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <E extends Enum<E> & BitFlag> ImmutableEnumSet<E> of(E... value) {
        Class<?> ctype = value.getClass().getComponentType();
        Class<?> etype = (etype = ctype.getSuperclass()) == Enum.class ? ctype : etype;
        Objects.requireNonNull(etype);
        return new ImmutableEnumSet<>((Class<E>) etype, Arrays.stream(value)
                .map(E::mask)
                .reduce(0, (l, r) -> l | r));
    }

    public Class<E> getElementType() {
        return elementType;
    }

    public int getValue() {
        return value;
    }

    public EnumSet<E> asEnumSet() {
        EnumSet<E> e = EnumSet.allOf(elementType);
        e.removeIf(v -> !contains(v));
        return e;
    }

    public ImmutableEnumSet<E> and(ImmutableEnumSet<E> other) {
        return new ImmutableEnumSet<>(elementType, other.value & value);
    }

    public ImmutableEnumSet<E> andNot(ImmutableEnumSet<E> other) {
        return new ImmutableEnumSet<>(elementType, ~other.value & value);
    }

    public ImmutableEnumSet<E> or(ImmutableEnumSet<E> other) {
        return new ImmutableEnumSet<>(elementType, other.value | value);
    }

    public ImmutableEnumSet<E> xor(ImmutableEnumSet<E> other) {
        return new ImmutableEnumSet<>(elementType, other.value ^ value);
    }

    @Override
    public int size() {
        return Integer.bitCount(value);
    }

    @Override
    public boolean isEmpty() {
        return value == 0;
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) return false;
        Class<?> c = o.getClass();
        if (c != elementType && c.getSuperclass() != elementType)
            return false;

        return (value & ((BitFlag) o).mask()) != 0;
    }

    @Override
    public Iterator<E> iterator() {
        return Collections.unmodifiableSet(asEnumSet()).iterator();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if (!(c instanceof ImmutableEnumSet<?>))
            return super.containsAll(c);

        ImmutableEnumSet<?> e = (ImmutableEnumSet<?>) c;
        if (e.elementType != elementType)
            return e.value == 0;
        return (e.value & value) == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o.getClass() != getClass())
            return super.equals(o);

        ImmutableEnumSet<?> e = (ImmutableEnumSet<?>) o;
        if (e.elementType != elementType)
            return value == 0 && e.value == 0;
        return e.value == value;
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }
}
