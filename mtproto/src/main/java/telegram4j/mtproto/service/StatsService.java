package telegram4j.mtproto.service;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.StatsGraph;
import telegram4j.tl.messages.Messages;
import telegram4j.tl.request.stats.*;
import telegram4j.tl.stats.BroadcastStats;
import telegram4j.tl.stats.MegagroupStats;
import telegram4j.tl.stats.MessageStats;

public class StatsService extends RpcService {

    public StatsService(MTProtoClient client, StoreLayout storeLayout) {
        super(client, storeLayout);
    }

    public Mono<BroadcastStats> getBroadcastStats(GetBroadcastStats request) {
        return client.sendAwait(request);
    }

    public Mono<StatsGraph> loadAsyncGraph(String token, @Nullable Long x) {
        return client.sendAwait(LoadAsyncGraph.builder().token(token).x(x).build());
    }

    public Mono<MegagroupStats> getMegagroupStats(GetMegagroupStats request) {
        return client.sendAwait(request);
    }

    public Mono<Messages> getMessagePublicForwards(GetMessagePublicForwards request) {
        return client.sendAwait(request);
    }

    public Mono<StatsGraph> getMessageStats(GetMessageStats request) {
        return client.sendAwait(request).map(MessageStats::viewsGraph);
    }
}
