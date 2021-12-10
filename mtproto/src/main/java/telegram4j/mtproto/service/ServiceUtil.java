package telegram4j.mtproto.service;

import reactor.util.annotation.Nullable;

import java.util.function.Function;

final class ServiceUtil {

    private ServiceUtil() {
    }

    @Nullable
    static <T, R> R mapNullable(@Nullable T t, Function<? super T, ? extends R> mapper) {
        if (t != null) {
            return mapper.apply(t);
        }
        return null;
    }
}
