package telegram4j.mtproto;

import reactor.core.publisher.Mono;

public interface MTProtoClient {

    Mono<MTProtoSession> getSession(DataCenter dc);

    MTProtoOptions getOptions();

    Mono<Void> close();
}
