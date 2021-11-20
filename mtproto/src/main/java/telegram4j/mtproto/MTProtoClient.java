package telegram4j.mtproto;

import reactor.core.publisher.Mono;

public interface MTProtoClient {

    Mono<MTProtoSession> openSession();

    MTProtoOptions getOptions();

    Mono<Void> close();
}
