package telegram4j.rest;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;
import reactor.util.annotation.Nullable;
import telegram4j.rest.route.Routes;

import java.net.URLEncoder;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class DefaultRouter implements RestRouter {

    private final RestResources restResources;
    private final HttpClient httpClient;
    private final HttpHeaders defaultHeaders;

    public DefaultRouter(RouterResources routerResources) {
        this.restResources = routerResources.getRestResources();
        this.httpClient = restResources.getHttpClient()
                .baseUrl(Routes.BASE_URL + "/bot" + routerResources.getToken());
        this.defaultHeaders = new DefaultHttpHeaders();
        defaultHeaders.add(HttpHeaderNames.CONTENT_TYPE, "application/json");
    }

    @Override
    public TelegramResponse exchange(TelegramRequest request) {

        HttpHeaders requestHeaders = buildHttpHeaders(request);

        Mono<String> computeUri = Mono.justOrEmpty(request.getParameters())
                .flatMapIterable(Map::entrySet)
                .flatMap(e -> serializeParameter(e.getValue())
                        .flatMap(json -> Mono.fromCallable(() -> URLEncoder.encode(e.getKey(), "UTF-8") +
                                "=" + URLEncoder.encode(json, "UTF-8"))))
                .collect(Collectors.joining("&", "?", ""))
                .map(parameters -> request.getRoute().getUriTemplate() + parameters)
                .defaultIfEmpty(request.getRoute().getUriTemplate());

        return new TelegramResponse(computeUri.flatMap(uri ->
                        httpClient.headers(headers -> headers.setAll(requestHeaders))
                                .request(request.getRoute().getMethod())
                                .uri(uri)
                                .send(ByteBufFlux.fromString(Mono.fromCallable(() -> restResources.getObjectMapper()
                                        .writeValueAsString(request.getBody()))))
                                .responseConnection((resp, conn) -> Mono.just(new RestClientResponse(resp, conn.inbound())))
                                .singleOrEmpty())
                .subscribeOn(Schedulers.boundedElastic()), restResources);
    }

    private Mono<String> serializeParameter(Object parameter) {
        if (parameter instanceof Number || parameter instanceof String ||
                parameter instanceof Boolean) {
            return Mono.just(Objects.toString(parameter));
        }

        return Mono.fromCallable(() -> restResources.getObjectMapper()
                .writeValueAsString(parameter));
    }

    private HttpHeaders buildHttpHeaders(TelegramRequest request) {
        HttpHeaders requestHeaders = Optional.ofNullable(request.getHeaders())
                .map(map -> map.entrySet().stream()
                        .reduce((HttpHeaders) new DefaultHttpHeaders(), (headers, header) ->
                                        headers.add(header.getKey(), header.getValue()),
                                HttpHeaders::add))
                .orElseGet(DefaultHttpHeaders::new);

        HttpHeaders headers = new DefaultHttpHeaders()
                .add(defaultHeaders)
                .setAll(requestHeaders);

        if (request.getBody() == null) {
            headers.remove(HttpHeaderNames.CONTENT_TYPE);
        }
        return headers;
    }
}
