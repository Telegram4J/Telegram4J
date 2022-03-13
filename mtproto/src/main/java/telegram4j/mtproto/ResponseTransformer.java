package telegram4j.mtproto;

import reactor.core.publisher.Mono;
import telegram4j.tl.api.EmptyObject;
import telegram4j.tl.api.TlMethod;

import java.util.function.Function;

@FunctionalInterface
public interface ResponseTransformer {

    static ResponseTransformer ignoreEmpty(MethodPredicate methodPredicate) {
        return new ResponseTransformer() {
            @Override
            public <R> Function<Mono<R>, Mono<R>> transform(TlMethod<R> method) {
                if (methodPredicate.test(method)) {
                    return mono -> mono.filter(r -> !(r instanceof EmptyObject));
                }

                return Function.identity();
            }
        };
    }

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

    <R> Function<Mono<R>, Mono<R>> transform(TlMethod<R> method);
}
