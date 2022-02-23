package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import telegram4j.core.event.domain.Event;
import telegram4j.tl.Update;

public interface UpdatesMapper {

    <U extends Update> Flux<Event> handle(UpdateContext<U> context);
}
