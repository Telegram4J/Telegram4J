package telegram4j.rest.response;

import reactor.core.publisher.Mono;
import telegram4j.rest.RestClientResponse;
import telegram4j.rest.TelegramRequest;

import java.util.function.Function;
import java.util.function.Predicate;

public class EmptyResponseTransformer implements ResponseTransformer {

    private final Predicate<? super Throwable> predicate;

    public EmptyResponseTransformer(Predicate<? super Throwable> predicate) {
        this.predicate = predicate;
    }

    @Override
    public Function<Mono<RestClientResponse>, Mono<RestClientResponse>> transform(TelegramRequest request) {
        return mono -> mono.onErrorResume(predicate, t -> Mono.empty());
    }
}
