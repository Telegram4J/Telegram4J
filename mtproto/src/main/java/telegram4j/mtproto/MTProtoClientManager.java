package telegram4j.mtproto;

import reactor.core.publisher.Mono;

import java.util.Optional;

public interface MTProtoClientManager {

    Optional<MTProtoClient> getClient(DataCenter dataCenter);

    void add(MTProtoClient client);

    void remove(DataCenter dataCenter);

    int activeCount();

    Mono<Void> close();
}
