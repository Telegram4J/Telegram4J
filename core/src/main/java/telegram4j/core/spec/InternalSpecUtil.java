package telegram4j.core.spec;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class InternalSpecUtil {

    private InternalSpecUtil() {}

    @SafeVarargs
    static <T> List<T> addAllOptional(Optional<T>... opts) {
        return Arrays.stream(opts)
                .flatMap(opt -> opt.map(Stream::of).orElseGet(Stream::empty))
                .collect(Collectors.toList());
    }
}
