package telegram4j.mtproto.client;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.concurrent.Queues;
import telegram4j.tl.Updates;

public class SinksUpdateDispatcher implements UpdateDispatcher {
    private final Sinks.Many<Updates> sink;
    private final Sinks.EmitFailureHandler emitFailureHandler;

    public SinksUpdateDispatcher() {
        this(Sinks.many().multicast().onBackpressureBuffer(Queues.XS_BUFFER_SIZE, false),
                Sinks.EmitFailureHandler.FAIL_FAST);
    }

    public SinksUpdateDispatcher(Sinks.Many<Updates> sink, Sinks.EmitFailureHandler emitFailureHandler) {
        this.sink = sink;
        this.emitFailureHandler = emitFailureHandler;
    }

    @Override
    public Flux<Updates> all() {
        return sink.asFlux();
    }

    @Override
    public <T extends Updates> Flux<T> on(Class<T> type) {
        return sink.asFlux()
                .ofType(type);
    }

    @Override
    public void publish(Updates updates) {
        sink.emitNext(updates, emitFailureHandler);
    }
}
