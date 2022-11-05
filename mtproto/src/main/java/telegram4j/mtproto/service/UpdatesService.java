package telegram4j.mtproto.service;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.MTProtoClientGroupManager;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.request.updates.GetChannelDifference;
import telegram4j.tl.request.updates.GetDifference;
import telegram4j.tl.request.updates.GetState;
import telegram4j.tl.updates.ChannelDifference;
import telegram4j.tl.updates.Difference;
import telegram4j.tl.updates.State;

@BotCompatible
public class UpdatesService extends RpcService {

    public UpdatesService(MTProtoClientGroupManager groupManager, StoreLayout storeLayout) {
        super(groupManager, storeLayout);
    }

    // updates namespace
    // =========================

    /**
     * Retrieve a current state of updates.
     *
     * @return A {@link Mono} emitting on successful completion current state of updates.
     */
    public Mono<State> getState() {
        return sendMain(GetState.instance());
    }

    /**
     * Retrieve a <b>common</b> updates difference from specified parameters.
     *
     * @return A {@link Mono} emitting on successful completion difference in the <b>common</b> updates from specified parameters.
     */
    public Mono<Difference> getDifference(GetDifference request) {
        return sendMain(request);
    }

    /**
     * Retrieve a <b>channel</b> updates difference from specified parameters.
     *
     * @return A {@link Mono} emitting on successful completion difference in the <b>channel</b> updates from specified parameters.
     */
    public Mono<ChannelDifference> getChannelDifference(GetChannelDifference request) {
        return sendMain(request);
    }
}
