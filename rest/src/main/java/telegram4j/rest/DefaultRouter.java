package telegram4j.rest;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import telegram4j.rest.route.Routes;

import java.util.Optional;

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

    @SuppressWarnings("unchecked")
    private static WriterStrategy<Object> cast(WriterStrategy<?> writerStrategy) {
        return (WriterStrategy<Object>) writerStrategy;
    }

    @Override
    public TelegramResponse exchange(TelegramRequest request) {

        return new TelegramResponse(request, Mono.defer(() -> {
                    HttpHeaders requestHeaders = buildHttpHeaders(request);

                    HttpClient.RequestSender sender = httpClient.headers(headers -> headers.setAll(requestHeaders))
                            .request(request.getRoute().getMethod())
                            .uri(request.getRoute().getUri());

                    Object body = request.getBody();
                    if (body == null) {
                        return Mono.just(sender.send(Mono.empty()));
                    }

                    return Flux.fromIterable(restResources.getWriterStrategies().getWriters())
                            .filter(strategy -> strategy.canWrite(body.getClass(), requestHeaders))
                            .next()
                            .switchIfEmpty(Mono.error(() -> new IllegalStateException("No write strategies for body: " + body)))
                            .map(DefaultRouter::cast)
                            .flatMap(strategy -> strategy.write(sender, body));
                })
                .flatMap(receiver -> receiver.responseConnection((resp, conn) -> Mono.just(
                                new RestClientResponse(resp, conn.inbound())))
                        .singleOrEmpty())
                .subscribeOn(Schedulers.boundedElastic())
                .checkpoint("Request to " + request.getRoute().getMethod() +
                        " " + request.getRoute().getMethod()), restResources);
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
