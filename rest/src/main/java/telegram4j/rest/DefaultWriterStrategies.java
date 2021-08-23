package telegram4j.rest;

import java.util.Collections;
import java.util.List;

class DefaultWriterStrategies implements WriterStrategies {

    private final List<WriterStrategy<?>> strategies;

    DefaultWriterStrategies(List<WriterStrategy<?>> strategies) {
        this.strategies = Collections.unmodifiableList(strategies);
    }

    @Override
    public List<WriterStrategy<?>> getWriters() {
        return strategies;
    }
}
