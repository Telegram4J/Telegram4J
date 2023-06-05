package telegram4j.mtproto.client;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.concurrent.Queues;
import telegram4j.tl.Updates;

import java.util.Objects;

public class SinksUpdateDispatcher implements UpdateDispatcher {
    private final Sinks.Many<Updates> sink;
    private final Sinks.EmitFailureHandler emitFailureHandler;

    public SinksUpdateDispatcher() {
        this(Sinks.many().multicast().onBackpressureBuffer(Queues.XS_BUFFER_SIZE, false),
                Sinks.EmitFailureHandler.FAIL_FAST);
    }

    public SinksUpdateDispatcher(Sinks.Many<Updates> sink, Sinks.EmitFailureHandler emitFailureHandler) {
        this.sink = Objects.requireNonNull(sink);
        this.emitFailureHandler = Objects.requireNonNull(emitFailureHandler);
    }

    @Override
    public Flux<Updates> all() {
        return sink.asFlux();
    }

    @Override
    public void publish(Updates updates) {
        sink.emitNext(updates, emitFailureHandler);
    }
}
