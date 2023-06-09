package telegram4j.mtproto.client;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import telegram4j.tl.Updates;

import java.util.Objects;

public class SinksUpdateDispatcher implements UpdateDispatcher {
    private final Scheduler scheduler;
    private final Sinks.Many<Updates> sink;
    private final Sinks.EmitFailureHandler emitFailureHandler;

    public SinksUpdateDispatcher(Scheduler scheduler, Sinks.Many<Updates> sink,
                                 Sinks.EmitFailureHandler emitFailureHandler) {
        this.scheduler = Objects.requireNonNull(scheduler);
        this.sink = Objects.requireNonNull(sink);
        this.emitFailureHandler = Objects.requireNonNull(emitFailureHandler);
    }

    @Override
    public Flux<Updates> all() {
        return sink.asFlux()
                .publishOn(scheduler);
    }

    @Override
    public void publish(Updates updates) {
        sink.emitNext(updates, emitFailureHandler);
    }
}
