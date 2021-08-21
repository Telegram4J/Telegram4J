package telegram4j.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
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

    protected <T> T readJson(String from) {
        try {
            return mapper.readerFor(type).readValue(getClass().getResourceAsStream(from));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Incorrect path to JSON file: " + from);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected <T extends List<?>> T readJsonList(String from) {
        try {
            return mapper.readerForListOf(type).readValue(getClass().getResourceAsStream(from));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Incorrect path to JSON file: " + from);
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
