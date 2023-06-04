package telegram4j.mtproto;

import reactor.core.publisher.Mono;
import telegram4j.tl.api.TlMethod;

import java.util.function.Function;
import java.util.function.Predicate;

/** Interface for mapping rpc responses. */
public interface ResponseTransformer {

    /**
     * Create new {@code ResponseTransformer} which returns
     * on matched exceptions {@link Mono#empty()} as response for given methods scope.
     *
     * @param methodPredicate The method scope.
     * @param predicate The predicate for exceptions.
     * @return The new {@code ResponseTransformer} which returns {@link Mono#empty()} on matched errors.
     */
    static ResponseTransformer empty(MethodPredicate methodPredicate, Predicate<? super Throwable> predicate) {
        return new ResponseTransformer() {
            @Override
            public <R> Function<Mono<R>, Mono<R>> transform(TlMethod<R> method) {
                if (methodPredicate.test(method)) {
                    return mono -> mono.onErrorResume(predicate, t -> Mono.empty());
                }
                return Function.identity();
            }
        };
    }

    /**
     * Create new {@code ResponseTransformer} which reties signals
     * on flood wait errors for given methods scope.
     *
     * @param methodPredicate The method scope.
     * @param retrySpec The retry spec for flood wait errors.
     * @return The new {@code ResponseTransformer} which retries signals on flood wait for matched methods.
     */
    static ResponseTransformer retryFloodWait(MethodPredicate methodPredicate, MTProtoRetrySpec retrySpec) {
        return new ResponseTransformer() {
            @Override
            public <R> Function<Mono<R>, Mono<R>> transform(TlMethod<R> method) {
                if (methodPredicate.test(method)) {
                    return mono -> mono.retryWhen(retrySpec);
                }
                return Function.identity();
            }
        };
    }

    /**
     * Modifies specified reactive sequence with rpc response.
     *
     * @param <R> The type of method response.
     * @param method The method which returns this response.
     * @return A {@link Function} which modifies response sequence.
     */
    <R> Function<Mono<R>, Mono<R>> transform(TlMethod<R> method);
}
