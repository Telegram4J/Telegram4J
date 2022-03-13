package telegram4j.mtproto;

import telegram4j.tl.api.TlMethod;

import java.util.function.Predicate;
import java.util.stream.Stream;

@FunctionalInterface
public interface MethodPredicate extends Predicate<TlMethod<?>> {

    @SafeVarargs
    static MethodPredicate any(Class<? extends TlMethod<?>>... methods) {
        return m -> Stream.of(methods).anyMatch(c -> m.getClass().isAssignableFrom(c));
    }

    static MethodPredicate all() {
        return m -> true;
    }
}
