package telegram4j.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.function.Tuple2;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

public class MultipartWriterStrategy implements WriterStrategy<MultipartRequest<?>> {

    private final ObjectMapper mapper;

    public MultipartWriterStrategy(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean canWrite(Class<?> type, HttpHeaders headers) {
        return headers.get(HttpHeaderNames.CONTENT_TYPE, "").equals("multipart/data-form");
    }

    @Override
    public Mono<HttpClient.ResponseReceiver<?>> write(HttpClient.RequestSender sender, MultipartRequest<?> body) {
        return Mono.fromSupplier(() -> sender.sendForm((request, form) -> {
            form.multipart(true);
            if (body.getJson() != null) {
                // Telegram doesn't allow to simply write one part as
                // json and then send a request. It requires that all json attributes/fields are parts
                JsonNode node = mapper.convertValue(body.getJson(), JsonNode.class);

                Iterator<Map.Entry<String, JsonNode>> it = node.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> field = it.next();
                    form.attr(field.getKey(), field.getValue().toString());
                }
            }

            for (Tuple2<String, InputStream> file : body.getFiles()) {
                form.file(file.getT1(), file.getT2(), "application/octet-stream");
            }
        }));
    }
}
