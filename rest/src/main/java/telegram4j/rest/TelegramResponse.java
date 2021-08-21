package telegram4j.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;

public class TelegramResponse {

    private final TelegramRequest request;
    private final Mono<RestClientResponse> clientResponse;
    private final RestResources restResources;

    public TelegramResponse(TelegramRequest request, Mono<RestClientResponse> clientResponse, RestResources restResources) {
        this.request = request;
        this.clientResponse = clientResponse;
        this.restResources = restResources;
    }

    public <T> Mono<T> bodyTo(Class<? extends T> type) {
        ObjectMapper mapper = restResources.getObjectMapper();
        return clientResponse.flatMap(res -> {
            if (res.getHttpResponse().status().code() >= 400) {
                return res.getBody().asByteArray()
                        .flatMap(arr -> Mono.fromCallable(() -> mapper.readValue(arr, ErrorResponse.class)))
                        .flatMap(errorResp -> Mono.error(new ClientException(request, res, errorResp)));
            }

            return Mono.just(res);
        })
        .flatMap(resp -> {
            ByteBufMono body = resp.getBody();
            return body.asByteArray()
                    .flatMap(arr -> Mono.fromCallable(() -> mapper.readTree(arr)))
                    // Skip Dispatch fields and get result
                    .flatMap(node -> Mono.fromCallable(() ->
                            mapper.convertValue(node.get("result"), type)));
        });
    }
}
