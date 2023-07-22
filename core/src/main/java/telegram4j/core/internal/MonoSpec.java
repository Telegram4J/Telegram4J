package telegram4j.core.internal;

import reactor.core.publisher.Mono;
import telegram4j.core.spec.ResolvableSpec;

public interface MonoSpec<T> extends ResolvableSpec<Mono<? extends T>> {
}
