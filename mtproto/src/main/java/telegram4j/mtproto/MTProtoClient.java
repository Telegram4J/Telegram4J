package telegram4j.mtproto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import telegram4j.tl.Updates;
import telegram4j.tl.api.TlMethod;

public interface MTProtoClient {

    Mono<Void> connect();

    Sinks.Many<Updates> updates();

    <R, T extends TlMethod<R>> Mono<R> sendAwait(T object);

    Mono<Void> send(TlMethod<?> object);

    Flux<State> state();

    DataCenter getDatacenter();

    Type getType();

    boolean updateTimeOffset(long serverTime);

    MTProtoClient createMediaClient(DataCenter dc);

    Mono<Void> close();

    enum State {
        DISCONNECTED,
        CLOSED,
        CONNECTED,
        RECONNECT
    }

    enum Type {
        DEFAULT,
        MEDIA,
        CDN
    }
}
