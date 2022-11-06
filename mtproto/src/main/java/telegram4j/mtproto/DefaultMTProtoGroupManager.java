package telegram4j.mtproto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.tl.api.TlMethod;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DefaultMTProtoGroupManager implements MTProtoClientGroupManager {

    private static final Duration DEFAULT_CHECKIN = Duration.ofMinutes(1);
    private static final Duration INACTIVE_REGULAR_DURATION = Duration.ofMinutes(15);
    private static final Duration INACTIVE_MEDIA_DURATION = Duration.ofMinutes(2);

    private final ConcurrentHashMap<DcId, ClientInfo> clients = new ConcurrentHashMap<>();

    private volatile MainMTProtoClient main;

    @Override
    public MainMTProtoClient main() {
        return Objects.requireNonNull(main);
    }

    @Override
    public DcId mainId() {
        return DcId.of(main().getDatacenter(), 0);
    }

    @Override
    public Optional<MTProtoClient> find(DcId id) {
        return Optional.ofNullable(clients.get(id))
                .map(c -> c.inner);
    }

    @Override
    public <R, M extends TlMethod<R>> Mono<R> send(DcId id, M method) {
        return Mono.justOrEmpty(clients.get(id))
                .switchIfEmpty(Mono.error(() -> new IllegalArgumentException("No client found for dcId: " + id)))
                .map(c -> {
                    c.lastQueryTimestamp = Instant.now();
                    return c.inner;
                })
                .flatMap(client -> client.sendAwait(method));
    }

    @Override
    public Mono<Void> close() {
        return Mono.whenDelayError(clients.values().stream()
                .map(c -> c.inner)
                .map(MTProtoClient::close)
                .collect(Collectors.toList()));
    }

    @Override
    public Mono<Void> start() {
        return Flux.interval(DEFAULT_CHECKIN)
                .flatMap(tick -> {
                    List<Mono<Void>> toClose = new ArrayList<>();
                    clients.forEach((dcId, clientInfo) -> {
                        if (clientInfo.inner == main())
                            return;

                        Duration inactivity;
                        switch (clientInfo.inner.getDatacenter().getType()) {
                            case REGULAR:
                                inactivity = INACTIVE_REGULAR_DURATION;
                                break;
                            case CDN:
                            case MEDIA:
                                inactivity = INACTIVE_MEDIA_DURATION;
                                break;
                            default:
                                throw new IllegalStateException();
                        }
                        Instant lastQueryTimestamp = clientInfo.lastQueryTimestamp;
                        if (lastQueryTimestamp == null || lastQueryTimestamp.plus(inactivity).isBefore(Instant.now())) {
                            clients.remove(dcId);
                            toClose.add(clientInfo.inner.close());
                        }
                    });

                    return Mono.whenDelayError(toClose);
                })
                .then();
    }

    @Override
    public void setMain(MainMTProtoClient client) {
        main = client;
        DcId k = DcId.of(client.getDatacenter().getInternalId(), 0);
        clients.put(k, new ClientInfo(client));
    }

    @Override
    public DcId add(MTProtoClient client) {
        DcId dcId = nextId(client.getDatacenter());
        clients.put(dcId, new ClientInfo(client));
        return dcId;
    }

    @Override
    public MTProtoClient getOrCreateMediaClient(DcId id, DataCenter dc) {
        return clients.computeIfAbsent(id, i -> new ClientInfo(main().createMediaClient(dc))).inner;
    }

    private DcId nextId(DataCenter dc) {
        int id = dc.getInternalId();
        var shift = clients.keySet().stream()
                .filter(dcId -> dcId.getId() == id)
                .mapToInt(DcId::getShift)
                .max();

        return DcId.of(id, shift.isEmpty() ? 0 : shift.getAsInt() + 1);
    }

    static class ClientInfo {
        final MTProtoClient inner;
        volatile Instant lastQueryTimestamp;

        ClientInfo(MTProtoClient inner) {
            this.inner = inner;
        }
    }
}
