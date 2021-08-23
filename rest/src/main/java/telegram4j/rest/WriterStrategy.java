package telegram4j.rest;

import io.netty.handler.codec.http.HttpHeaders;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.annotation.Nullable;

public interface WriterStrategy<T> {

    boolean canWrite(@Nullable Class<?> type, HttpHeaders headers);

    Mono<HttpClient.ResponseReceiver<?>> write(HttpClient.RequestSender sender, @Nullable T body);
}
