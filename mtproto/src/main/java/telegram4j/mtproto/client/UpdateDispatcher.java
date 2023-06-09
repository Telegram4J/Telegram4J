package telegram4j.mtproto.client;

import reactor.core.publisher.Flux;
import telegram4j.tl.Updates;

public interface UpdateDispatcher {

    Flux<Updates> all();

    default <T extends Updates> Flux<T> on(Class<T> type) {
        return all()
                .ofType(type);
    }

    void publish(Updates updates);

    void shutdown();
}
