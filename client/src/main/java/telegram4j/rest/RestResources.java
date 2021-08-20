package telegram4j.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import reactor.netty.http.client.HttpClient;

import java.util.Objects;
import java.util.function.Supplier;

public class RestResources {

    public static final Supplier<HttpClient> DEFAULT_HTTP_CLIENT = () -> HttpClient.create().compress(true);
    public static final Supplier<ObjectMapper> DEFAULT_OBJECT_MAPPER = () -> new ObjectMapper()
            .registerModule(new Jdk8Module());

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public RestResources() {
        httpClient = DEFAULT_HTTP_CLIENT.get();
        objectMapper = DEFAULT_OBJECT_MAPPER.get();
    }

    public RestResources(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
