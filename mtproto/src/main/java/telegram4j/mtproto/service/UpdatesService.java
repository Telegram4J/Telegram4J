package telegram4j.mtproto.service;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.request.updates.GetChannelDifference;
import telegram4j.tl.request.updates.GetDifference;
import telegram4j.tl.request.updates.GetState;
import telegram4j.tl.updates.ChannelDifference;
import telegram4j.tl.updates.Difference;
import telegram4j.tl.updates.State;

public class UpdatesService extends RpcService {

    public UpdatesService(MTProtoClient client, StoreLayout storeLayout) {
        super(client, storeLayout);
    }

    public Mono<State> getState() {
        return client.sendAwait(GetState.instance())
                .flatMap(s -> storeLayout.updateState(s)
                        .thenReturn(s));
    }

    public Mono<Difference> getDifference(GetDifference request) {
        return client.sendAwait(request);
    }

    public Mono<ChannelDifference> getChannelDifference(GetChannelDifference request) {
        return client.sendAwait(request);
    }
}
