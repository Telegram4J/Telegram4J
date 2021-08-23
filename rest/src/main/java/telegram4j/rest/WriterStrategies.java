package telegram4j.rest;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public interface WriterStrategies {

    static WriterStrategies create(ObjectMapper mapper) {
        List<WriterStrategy<?>> strategies = new ArrayList<>();
        strategies.add(new JsonWriterStrategy(mapper));
        strategies.add(new MultipartWriterStrategy(mapper));

        return new DefaultWriterStrategies(strategies);
    }

    List<WriterStrategy<?>> getWriters();
}
