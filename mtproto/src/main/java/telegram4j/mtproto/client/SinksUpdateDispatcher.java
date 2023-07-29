package telegram4j.mtproto.client;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import telegram4j.tl.Updates;

import java.util.Objects;

public class SinksUpdateDispatcher implements UpdateDispatcher {
    protected final Scheduler scheduler;
    protected final boolean disposeScheduler;
    protected final Sinks.Many<Updates> sink;
    protected final Sinks.EmitFailureHandler emitFailureHandler;

    public SinksUpdateDispatcher(Scheduler scheduler, boolean disposeScheduler,
                                 Sinks.Many<Updates> sink,
                                 Sinks.EmitFailureHandler emitFailureHandler) {
        this.scheduler = Objects.requireNonNull(scheduler);
        this.disposeScheduler = disposeScheduler;
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

    @Override
    public Mono<Void> close() {
        return Mono.defer(() -> {
            sink.emitComplete(emitFailureHandler);

            if (disposeScheduler) {
                return scheduler.disposeGracefully();
            }
            return Mono.empty();
        });
    }
}
