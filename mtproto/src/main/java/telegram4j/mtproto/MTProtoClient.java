package telegram4j.mtproto;

import reactor.core.publisher.Mono;

public interface MTProtoClient {

    Mono<MTProtoSession> getSession(DataCenter dc);

    Mono<Void> close();
}
