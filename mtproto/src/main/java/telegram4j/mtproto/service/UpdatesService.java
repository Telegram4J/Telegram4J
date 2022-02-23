package telegram4j.mtproto.service;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.BotCompatible;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.request.updates.GetChannelDifference;
import telegram4j.tl.request.updates.GetDifference;
import telegram4j.tl.request.updates.GetState;
import telegram4j.tl.updates.ChannelDifference;
import telegram4j.tl.updates.Difference;
import telegram4j.tl.updates.State;

@BotCompatible
public class UpdatesService extends RpcService {

    public UpdatesService(MTProtoClient client, StoreLayout storeLayout) {
        super(client, storeLayout);
    }

    /**
     * Retrieve a current state of updates.
     *
     * @return A {@link Mono} emitting on successful completion current state of updates.
     */
    public Mono<State> getState() {
        return client.sendAwait(GetState.instance());
    }

    /**
     * Retrieve a <b>common</b> updates difference from specified parameters.
     *
     * @return A {@link Mono} emitting on successful completion difference in the <b>common</b> updates from specified parameters.
     */
    public Mono<Difference> getDifference(GetDifference request) {
        return client.sendAwait(request);
    }

    /**
     * Retrieve a <b>channel</b> updates difference from specified parameters.
     *
     * @return A {@link Mono} emitting on successful completion difference in the <b>channel</b> updates from specified parameters.
     */
    public Mono<ChannelDifference> getChannelDifference(GetChannelDifference request) {
        return client.sendAwait(request);
    }
}
