package telegram4j.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.io.IOException;
import java.util.function.Function;

public abstract class DeserializationTest {

    private static final Function<Logger, DeserializationProblemHandler> defaultProblemHandler = (logger) -> new DeserializationProblemHandler() {
        @Override
        public boolean handleUnknownProperty(DeserializationContext ctx, JsonParser p, JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName) throws IOException {
            logger.warn("Unknown property in {}: {}", beanOrClass, propertyName);
            p.skipChildren();
            return true;
        }
    };

    private final Logger logger = Loggers.getLogger(getClass());

    private final ObjectMapper mapper = JsonMapper.builder()
            .addModules(new Jdk8Module())
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .addHandler(defaultProblemHandler.apply(logger))
            .build();

    private final Class<?> type;

    <T> DeserializationTest(Class<T> type) {
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    protected <T> T read(String from) {
        try {
            return (T) mapper.readValue(getClass().getResourceAsStream(from), type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Logger getLogger() {
        return logger;
    }

    protected ObjectMapper getMapper() {
        return mapper;
    }
}
