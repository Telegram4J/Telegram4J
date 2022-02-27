package telegram4j.core.util;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

public final class PaginationSupport {

    private PaginationSupport() {}

    public static <T> Flux<T> paginate(IntFunction<? extends Publisher<T>> prod, ToIntFunction<T> countExtractor, int offset, int limit) {
        AtomicInteger localOffset = new AtomicInteger(offset);
        AtomicInteger count = new AtomicInteger(-1);

        return Flux.defer(() -> prod.apply(localOffset.get()))
                .doOnNext(c -> {
                    count.set(countExtractor.applyAsInt(c));
                    localOffset.addAndGet(limit);
                })
                .repeat(() -> count.get() > localOffset.get());
    }
}
