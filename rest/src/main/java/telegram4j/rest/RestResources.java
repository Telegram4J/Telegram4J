package telegram4j.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import reactor.netty.http.client.HttpClient;

import java.util.Objects;
import java.util.function.Supplier;

/** Resources used across REST module. */
public class RestResources {

    /** The {@link Supplier} which creates a default {@link HttpClient}s. */
    public static final Supplier<HttpClient> DEFAULT_HTTP_CLIENT = () -> HttpClient.create().compress(true);

    /**
     * The {@link Supplier} which creates a {@link ObjectMapper} instances
     * with required for serialization/deserialization options.
     */
    public static final Supplier<ObjectMapper> DEFAULT_OBJECT_MAPPER = () -> JsonMapper.builder()
            .addModules(new Jdk8Module())
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .visibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .visibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY)
            .visibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY)
            .serializationInclusion(JsonInclude.Include.NON_ABSENT)
            .build();

    /** Netty http client used in REST API. */
    private final HttpClient httpClient;

    /** Jackson mapper for json serializing/deserializing. */
    private final ObjectMapper objectMapper;
    private final WriterStrategies writerStrategies;

    /** Constructs a {@link RestResources} with default settings. */
    public RestResources() {
        this.httpClient = DEFAULT_HTTP_CLIENT.get();
        this.objectMapper = DEFAULT_OBJECT_MAPPER.get();
        this.writerStrategies = WriterStrategies.create(objectMapper);
    }

    public RestResources(HttpClient httpClient, ObjectMapper objectMapper, WriterStrategies writerStrategies) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.writerStrategies = Objects.requireNonNull(writerStrategies, "writerStrategies");
    }

    /** @return The {@link HttpClient} for HTTP requests. */
    public HttpClient getHttpClient() {
        return httpClient;
    }

    /** @return The {@link ObjectMapper} mapper for json serializing/deserializing. */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public WriterStrategies getWriterStrategies() {
        return writerStrategies;
    }
}
