package telegram4j.mtproto;

import reactor.core.publisher.Mono;

public interface MTProtoClientGroupManager extends MTProtoClientGroup {

    Mono<Void> start();

    void setMain(MainMTProtoClient client);

    DcId add(MTProtoClient client);

    MTProtoClient getOrCreateMediaClient(DcId id, DataCenter dc);
}
