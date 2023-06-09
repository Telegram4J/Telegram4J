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
            public <R> Mono<R> transform(Mono<R> mono, TlMethod<? extends R> method) {
                if (methodPredicate.test(method)) {
                    return mono.onErrorResume(predicate, t -> Mono.empty());
                }
                return mono;
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
            public <R> Mono<R> transform(Mono<R> mono, TlMethod<? extends R> method) {
                if (methodPredicate.test(method)) {
                    return mono.retryWhen(retrySpec);
                }
                return mono;
            }
        };
    }

    /**
     * Modifies specified reactive sequence with rpc response.
     *
     * @param <R> The type of method response.
     * @param mono The mono for transformation.
     * @param method The method which returns this response.
     * @return A {@link Function} which modifies response sequence.
     */
    <R> Mono<R> transform(Mono<R> mono, TlMethod<? extends R> method);
}
