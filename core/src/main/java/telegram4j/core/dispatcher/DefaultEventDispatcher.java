package telegram4j.core.dispatcher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.annotation.Nullable;
import reactor.util.concurrent.Queues;
import telegram4j.core.event.Event;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class DefaultEventDispatcher implements EventDispatcher {

    private final Sinks.Many<Event> events;
    private final Scheduler eventScheduler;

    public DefaultEventDispatcher(Sinks.Many<Event> events, Scheduler eventScheduler) {
        this.events = Objects.requireNonNull(events, "events");
        this.eventScheduler = Objects.requireNonNull(eventScheduler, "eventScheduler");
    }

    public static EventDispatcher create() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public <E extends Event> Flux<E> on(Class<E> type) {
        return events.asFlux().ofType(type).publishOn(eventScheduler);
    }

    @Override
    public <E extends Event> void publish(E event) {
        events.emitNext(event, Sinks.EmitFailureHandler.FAIL_FAST);
    }

    public static class Builder {

        public static final Function<Sinks.ManySpec, Sinks.Many<Event>> DEFAULT_SINKS_FACTORY =
                spec -> spec.multicast().onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);

        public static final Supplier<Scheduler> DEFAULT_EVENT_SCHEDULER = Schedulers::boundedElastic;

        @Nullable
        private Function<Sinks.ManySpec, Sinks.Many<Event>> sinksFactory;
        @Nullable
        private Scheduler eventScheduler;

        private Builder() {}

        public Builder setSinksFactory(Function<Sinks.ManySpec, Sinks.Many<Event>> sinksFactory) {
            this.sinksFactory = Objects.requireNonNull(sinksFactory, "sinksFactory");
            return this;
        }

        public Builder setEventScheduler(Scheduler eventScheduler) {
            this.eventScheduler = Objects.requireNonNull(eventScheduler, "eventScheduler");
            return this;
        }

        public DefaultEventDispatcher build() {
            if (sinksFactory == null) {
                sinksFactory = DEFAULT_SINKS_FACTORY;
            }

            if (eventScheduler == null) {
                eventScheduler = DEFAULT_EVENT_SCHEDULER.get();
            }

            return new DefaultEventDispatcher(sinksFactory.apply(Sinks.many()), eventScheduler);
        }
    }
}
