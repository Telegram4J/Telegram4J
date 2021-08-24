package telegram4j.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;

public class JsonWriterStrategy implements WriterStrategy<Object> {

    private final ObjectMapper mapper;

    public JsonWriterStrategy(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean canWrite(Class<?> type, HttpHeaders headers) {
        return headers.get(HttpHeaderNames.CONTENT_TYPE, "").equals("application/json") && mapper.canSerialize(type);
    }

    @Override
    public Mono<HttpClient.ResponseReceiver<?>> write(HttpClient.RequestSender sender, Object body) {
        return Mono.fromSupplier(() -> sender.send(ByteBufFlux.fromString(
                Mono.fromCallable(() -> mapper.writeValueAsString(body)),
                StandardCharsets.UTF_8, ByteBufAllocator.DEFAULT)));
    }
}
