package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import telegram4j.core.event.Event;

public class DefaultEventDispatcher implements EventDispatcher {
    private final Scheduler eventScheduler;
    private final Sinks.Many<Event> sink;

    public DefaultEventDispatcher(Scheduler eventScheduler, Sinks.Many<Event> sink) {
        this.eventScheduler = eventScheduler;
        this.sink = sink;
    }

    @Override
    public <E extends Event> Flux<E> on(Class<E> type) {
        return sink.asFlux()
                .subscribeOn(eventScheduler)
                .ofType(type);
    }

    @Override
    public void publish(Event event) {
        sink.emitNext(event, Sinks.EmitFailureHandler.FAIL_FAST);
    }
}
