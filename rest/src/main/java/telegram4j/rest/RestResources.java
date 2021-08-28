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
import telegram4j.rest.response.ResponseTransformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private final List<ResponseTransformer> responseTransformers;

    /** Constructs a {@link RestResources} with default settings. */
    public RestResources() {
        this.httpClient = DEFAULT_HTTP_CLIENT.get();
        this.objectMapper = DEFAULT_OBJECT_MAPPER.get();
        this.writerStrategies = WriterStrategies.create(objectMapper);
        this.responseTransformers = Collections.emptyList();
    }

    public RestResources(HttpClient httpClient, ObjectMapper objectMapper,
                         WriterStrategies writerStrategies,
                         List<ResponseTransformer> responseTransformers) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.writerStrategies = Objects.requireNonNull(writerStrategies, "writerStrategies");
        this.responseTransformers = Collections.unmodifiableList(responseTransformers);
    }

    public static Builder builder() {
        return new Builder();
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

    public List<ResponseTransformer> getResponseTransformers() {
        return responseTransformers;
    }

    public static class Builder {

        private HttpClient httpClient;
        private ObjectMapper objectMapper;
        private WriterStrategies writerStrategies;
        private List<ResponseTransformer> responseTransformers;

        private Builder() {}

        public Builder setHttpClient(HttpClient httpClient) {
            this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
            return this;
        }

        public Builder setObjectMapper(ObjectMapper objectMapper) {
            this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
            return this;
        }

        public Builder setWriterStrategies(WriterStrategies writerStrategies) {
            this.writerStrategies = Objects.requireNonNull(writerStrategies, "writerStrategies");
            return this;
        }

        public Builder addResponseTransformers(ResponseTransformer... responseTransformers) {
            if (this.responseTransformers == null) {
                this.responseTransformers = new ArrayList<>();
            }
            Collections.addAll(this.responseTransformers, responseTransformers);
            return this;
        }

        public RestResources build() {
            if (httpClient == null) {
                httpClient = DEFAULT_HTTP_CLIENT.get();
            }
            if (objectMapper == null) {
                objectMapper = DEFAULT_OBJECT_MAPPER.get();
            }
            if (writerStrategies == null) {
                writerStrategies = WriterStrategies.create(objectMapper);
            }
            if (responseTransformers == null) {
                responseTransformers = Collections.emptyList();
            }

            return new RestResources(httpClient, objectMapper, writerStrategies, responseTransformers);
        }
    }
}
