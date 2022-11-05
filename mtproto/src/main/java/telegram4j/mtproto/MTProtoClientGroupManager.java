package telegram4j.mtproto;

import reactor.core.publisher.Mono;

public interface MTProtoClientGroupManager extends MTProtoClientGroup {

    Mono<Void> start();

    void setMain(MainMTProtoClient client);

    void add(MTProtoClient client);

    void remove(DcId id);

    MTProtoClient getOrCreateMediaClient(DcId id, DataCenter dc);
}
