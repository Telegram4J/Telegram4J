package telegram4j.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.function.Tuple2;
import telegram4j.json.InputFile;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class MultipartWriterStrategy implements WriterStrategy<MultipartRequest<?>> {

    private final ObjectMapper mapper;

    public MultipartWriterStrategy(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean canWrite(Class<?> type, HttpHeaders headers) {
        return headers.get(HttpHeaderNames.CONTENT_TYPE, "").equals("multipart/form-data");
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

            for (Tuple2<String, InputFile> file : body.getFiles()) {
                String name = file.getT1();
                InputFile data = file.getT2();
                if (data.getContent() != null) {
                    // 'thumb' and 'media' fields allow reference to 'attach://<file_attach_name>'
                    String filename0 = data.getFilename();
                    String filename = filename0;
                    if (name.equals("thumb") || name.equals("media")) {
                        filename = "attach://" + filename0;
                    }

                    if (name.equals("media")) {
                        name = filename0;
                    }

                    form.file(name, filename, data.getContent(), null);
                } else {
                    String url = data.getUrl();
                    Objects.requireNonNull(url, "url");
                    form.attr(name, url);
                }
            }
        }));
    }
}
