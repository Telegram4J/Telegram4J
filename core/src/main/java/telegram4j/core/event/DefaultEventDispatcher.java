package telegram4j.core.event;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import telegram4j.core.event.domain.Event;

public class DefaultEventDispatcher implements EventDispatcher {
    private final Scheduler eventScheduler;
    private final Sinks.Many<Event> sink;
    private final Sinks.EmitFailureHandler emissionHandler;

    public DefaultEventDispatcher(Scheduler eventScheduler, Sinks.Many<Event> sink, Sinks.EmitFailureHandler emissionHandler) {
        this.eventScheduler = eventScheduler;
        this.sink = sink;
        this.emissionHandler = emissionHandler;
    }

    @Override
    public <E extends Event> Flux<E> on(Class<E> type) {
        return sink.asFlux()
                .subscribeOn(eventScheduler)
                .ofType(type);
    }

    @Override
    public void publish(Event event) {
        sink.emitNext(event, emissionHandler);
    }

    @Override
    public void shutdown() {
        sink.emitComplete(emissionHandler);
    }
}
