package telegram4j.mtproto;

import reactor.core.publisher.Mono;
import telegram4j.tl.api.TlMethod;

import java.util.function.Function;

/** Interface for mapping rpc responses. */
@FunctionalInterface
public interface ResponseTransformer {

    /**
     * Create new {@code ResponseTransformer} which returns
     * on specified error codes {@link Mono#empty()} as response for given methods scope.
     *
     * @param methodPredicate The method scope.
     * @param codes The array of method error codes.
     * @return The new {@code ResponseTransformer} which returns {@link Mono#empty()} on matched errors.
     */
    static ResponseTransformer emptyOnErrorCodes(MethodPredicate methodPredicate, int... codes) {
        return new ResponseTransformer() {
            @Override
            public <R> Function<Mono<R>, Mono<R>> transform(TlMethod<R> method) {
                if (methodPredicate.test(method)) {
                    return mono -> mono.onErrorResume(RpcException.isErrorCode(codes), t -> Mono.empty());
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
