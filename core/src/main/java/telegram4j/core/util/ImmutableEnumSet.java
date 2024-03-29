/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package telegram4j.core.util;

import telegram4j.tl.api.TlEncodingUtil;

import java.util.*;
import java.util.function.IntBinaryOperator;
import java.util.function.Predicate;

/**
 * Immutable version of {@link EnumSet} which backed as {@code int}.
 * This enum set accepts only enums which implement {@link BitFlag} to
 * use custom bit masks and positions.
 */
public final class ImmutableEnumSet<E extends Enum<E> & BitFlag> extends AbstractSet<E> {

    private final Class<E> elementType;
    private final int value;

    private ImmutableEnumSet(Class<E> elementType, int value) {
        this.elementType = elementType;
        this.value = value;
    }

    public static <E extends Enum<E> & BitFlag> ImmutableEnumSet<E> of(Class<E> type, Iterable<E> values) {
        if (values instanceof ImmutableEnumSet<E> e) {
            return e;
        }
        Objects.requireNonNull(type);

        int mask = 0;
        for (E e : values) {
            mask |= e.mask();
        }
        return new ImmutableEnumSet<>(type, mask);
    }

    public static <E extends Enum<E> & BitFlag> ImmutableEnumSet<E> of(Class<E> type, int value) {
        Objects.requireNonNull(type);
        return new ImmutableEnumSet<>(type, value);
    }

    @SuppressWarnings("unchecked")
    public static <E extends Enum<E> & BitFlag> ImmutableEnumSet<E> of(E... values) {
        Class<?> ctype = values.getClass().getComponentType();
        Class<?> etype = (etype = ctype.getSuperclass()) == Enum.class ? ctype : etype;

        int mask = 0;
        for (E e : values) {
            mask |= e.mask();
        }

        return new ImmutableEnumSet<>((Class<E>) etype, mask);
    }

    public Class<E> getElementType() {
        return elementType;
    }

    public int getValue() {
        return value;
    }

    public EnumSet<E> asEnumSet() {
        var e = EnumSet.allOf(elementType);
        e.removeIf(v -> !contains(v));
        return e;
    }

    public ImmutableEnumSet<E> and(ImmutableEnumSet<E> other) {
        return with(other, (a, b) -> a & b);
    }

    public ImmutableEnumSet<E> andNot(ImmutableEnumSet<E> other) {
        return with(other, (a, b) -> a & ~b);
    }

    public ImmutableEnumSet<E> or(ImmutableEnumSet<E> other) {
        return with(other, (a, b) -> a | b);
    }

    public ImmutableEnumSet<E> xor(ImmutableEnumSet<E> other) {
        return with(other, (a, b) -> a ^ b);
    }

    public ImmutableEnumSet<E> set(E flag, boolean state) {
        int value = TlEncodingUtil.mask(this.value, flag.mask(), state);
        if (value == this.value) return this;
        return new ImmutableEnumSet<>(elementType, value);
    }

    public ImmutableEnumSet<E> with(ImmutableEnumSet<E> set, IntBinaryOperator op) {
        int value = op.applyAsInt(this.value, set.value);
        if (value == this.value) return this;
        return new ImmutableEnumSet<>(elementType, value);
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
        if (!(c instanceof ImmutableEnumSet<?> e))
            return super.containsAll(c);
        if (e.elementType != elementType)
            return e.value == 0;
        return (e.value & value) == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o instanceof ImmutableEnumSet<?> e) {
            if (e.elementType != elementType)
                return value == 0 && e.value == 0;
            return e.value == value;
        }
        return super.equals(o);
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
