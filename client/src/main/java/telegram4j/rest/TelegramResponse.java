package telegram4j.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;

public class TelegramResponse {

    private final Mono<RestClientResponse> clientResponse;
    private final RestResources restResources;

    public TelegramResponse(Mono<RestClientResponse> clientResponse, RestResources restResources) {
        this.clientResponse = clientResponse;
        this.restResources = restResources;
    }

    public <T> Mono<T> bodyTo(Class<? extends T> type) {
        return clientResponse.flatMap(res -> {
            if (res.getHttpResponse().status().code() >= 400) {
                // TODO: replace to normal exception
                return Mono.error(new RuntimeException());
            }

            return Mono.just(res);
        })
        .flatMap(resp -> {
            ByteBufMono body = resp.getBody();
            ObjectMapper mapper = restResources.getObjectMapper();
            return body.asByteArray()
                    .flatMap(arr -> Mono.fromCallable(() -> mapper.readTree(arr).get("result")))
                    // Skip Dispatch fields and get result
                    .flatMap(node -> Mono.fromCallable(() ->
                            mapper.convertValue(node, type)));
        });
    }
}
