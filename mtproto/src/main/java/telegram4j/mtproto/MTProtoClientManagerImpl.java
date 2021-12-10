package telegram4j.mtproto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MTProtoClientManagerImpl implements MTProtoClientManager {

    private final ConcurrentMap<DataCenter, MTProtoClient> clients = new ConcurrentHashMap<>();

    @Override
    public Optional<MTProtoClient> getClient(DataCenter dataCenter) {
        return Optional.ofNullable(clients.get(dataCenter));
    }

    @Override
    public void add(MTProtoClient client) {
        clients.put(client.getDatacenter(), client);
    }

    @Override
    public void remove(DataCenter dataCenter) {
        clients.remove(dataCenter);
    }

    @Override
    public int activeCount() {
        return clients.size();
    }

    @Override
    public Mono<Void> close() {
        return Flux.fromIterable(clients.values())
                .flatMap(MTProtoClient::close)
                .then();
    }
}
