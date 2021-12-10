package telegram4j.mtproto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import telegram4j.tl.TlMethod;
import telegram4j.tl.TlObject;
import telegram4j.tl.Updates;

public interface MTProtoClient {

    Mono<Void> connect();

    Flux<TlObject> rpcReceiver();

    Sinks.Many<Updates> updates();

    boolean updateTimeOffset(long serverTime);

    <R, T extends TlMethod<R>> Mono<R> sendAwait(T object);

    Mono<Void> send(TlMethod<?> object);

    Flux<State> state();

    DataCenter getDatacenter();

    Mono<Void> close();

    enum State {
        DISCONNECTED,
        CONNECTED,
        RECONNECT
    }
}
