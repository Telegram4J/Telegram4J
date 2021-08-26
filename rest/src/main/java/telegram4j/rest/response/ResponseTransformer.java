package telegram4j.rest.response;

import reactor.core.publisher.Mono;
import telegram4j.rest.ClientException;
import telegram4j.rest.RestClientResponse;
import telegram4j.rest.TelegramRequest;

import java.util.Arrays;
import java.util.function.Function;

@FunctionalInterface
public interface ResponseTransformer {

    Function<Mono<RestClientResponse>, Mono<RestClientResponse>> transform(TelegramRequest request);

    static ResponseTransformer emptyIfNotFound() {
        return emptyOnErrorStatus(404);
    }

    static ResponseTransformer emptyOnErrorStatus(int... codes) {
        return new EmptyResponseTransformer(t -> {
            if (t instanceof ClientException) {
                ClientException ex = (ClientException) t;
                int code = ex.getResponse().getHttpResponse().status().code();
                return Arrays.stream(codes).anyMatch(i -> code == i);
            }
            return false;
        });
    }
}
