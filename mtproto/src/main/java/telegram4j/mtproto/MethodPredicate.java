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
package telegram4j.mtproto;

import telegram4j.tl.api.TlMethod;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** Functional interface for {@link Predicate} with {@link TlMethod} type. */
@FunctionalInterface
public interface MethodPredicate extends Predicate<TlMethod<?>> {

    /**
     * Create {@code MethodPredicate} which matches on one of the specified rpc method types.
     *
     * @param methods The types of rpc methods to match.
     * @return The new {@code MethodPredicate} which matches on one of the specified rpc method types.
     */
    @SafeVarargs
    static MethodPredicate any(Class<? extends TlMethod<?>>... methods) {
        return m -> Stream.of(methods).anyMatch(c -> m.getClass().isAssignableFrom(c));
    }

    /**
     * Create {@code MethodPredicate} which matches on one of the specified {@link TlMethod#identifier() tl object id}.
     *
     * @param ids The type ids of rpc methods to match.
     * @return The new {@code MethodPredicate} which matches on one of the specified tl object ids.
     */
    static MethodPredicate any(int... ids) {
        return m -> IntStream.of(ids).anyMatch(c -> m.identifier() == c);
    }

    /**
     * Create {@code MethodPredicate} which always predicates methods as {@code true}.
     *
     * @return The new {@code MethodPredicate} which always predicates methods as {@code true}.
     */
    static MethodPredicate all() {
        return m -> true;
    }

    @Override
    default MethodPredicate and(Predicate<? super TlMethod<?>> other) {
        Objects.requireNonNull(other);
        return t -> test(t) && other.test(t);
    }

    @Override
    default Predicate<TlMethod<?>> negate() {
        return t -> !test(t);
    }

    @Override
    default Predicate<TlMethod<?>> or(Predicate<? super TlMethod<?>> other) {
        Objects.requireNonNull(other);
        return t -> test(t) || other.test(t);
    }
}
