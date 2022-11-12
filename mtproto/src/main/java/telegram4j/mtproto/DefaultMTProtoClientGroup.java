package telegram4j.mtproto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.api.TlMethod;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DefaultMTProtoClientGroup implements MTProtoClientGroup {

    private static final Duration DEFAULT_CHECKIN = Duration.ofMinutes(1);
    private static final Duration INACTIVE_REGULAR_DURATION = Duration.ofMinutes(15);
    private static final Duration INACTIVE_UPLOAD_DURATION = Duration.ofMinutes(2);
    private static final Duration INACTIVE_DOWNLOAD_DURATION = Duration.ofMinutes(3);

    private final ConcurrentHashMap<DcId, MTProtoClient> clients = new ConcurrentHashMap<>();
    private final StoreLayout storeLayout;
    private final MainMTProtoClient main;

    private volatile DcOptions dcOptions;

    public DefaultMTProtoClientGroup(MTProtoClientGroupOptions options) {
        this.storeLayout = options.storeLayout;
        this.main = options.mainClient;
        this.dcOptions = options.dcOptions;

        clients.put(mainId(), main);
    }

    @Override
    public MainMTProtoClient main() {
        return main;
    }

    @Override
    public DcId mainId() {
        return DcId.of(DcId.Type.MAIN, main.getDatacenter(), 0);
    }

    @Override
    public Optional<MTProtoClient> find(DcId id) {
        return Optional.ofNullable(clients.get(id));
    }

    @Override
    public <R, M extends TlMethod<R>> Mono<R> send(DcId id, M method) {
        return Mono.justOrEmpty(clients.get(id))
                .switchIfEmpty(Mono.error(() -> new IllegalArgumentException("No client found for dcId: " + id)))
                .flatMap(client -> client.sendAwait(method));
    }

    @Override
    public Mono<Void> close() {
        return Mono.whenDelayError(clients.values().stream()
                .map(MTProtoClient::close)
                .collect(Collectors.toList()));
    }

    @Override
    public Mono<Void> start() {
        var initDcOptions = Mono.defer(() -> {
            var curr = dcOptions;
            if (curr != null) {
                return Mono.empty();
            }

            return storeLayout.getDcOptions()
                    .doOnNext(opts -> dcOptions = opts);
        });

        var inactivitySchedule = Flux.interval(DEFAULT_CHECKIN)
                .flatMap(tick -> {
                    List<Mono<Void>> toClose = new ArrayList<>();
                    clients.forEach((dcId, clientInfo) -> {
                        Duration inactivity;
                        switch (dcId.getType()) {
                            case REGULAR:
                                inactivity = INACTIVE_REGULAR_DURATION;
                                break;
                            case UPLOAD:
                                inactivity = INACTIVE_UPLOAD_DURATION;
                                break;
                            case DOWNLOAD:
                                inactivity = INACTIVE_DOWNLOAD_DURATION;
                                break;
                            default:
                                return;
                        }

                        var isInactive = clientInfo.getStats().getLastQueryTimestamp()
                                .map(ts -> ts.plus(inactivity).isBefore(Instant.now()))
                                .orElse(true);

                        if (isInactive) {
                            clients.remove(dcId);
                            toClose.add(clientInfo.close());
                        }
                    });

                    return Mono.whenDelayError(toClose);
                })
                .then();

        return Mono.when(initDcOptions, inactivitySchedule);
    }

    @Override
    public Mono<Void> setDcOptions(DcOptions dcOptions) {
        var old = this.dcOptions;
        this.dcOptions = Objects.requireNonNull(dcOptions);
        return old == null ? Mono.empty() : storeLayout.updateDcOptions(dcOptions);
    }

    @Override
    public Mono<MTProtoClient> getOrCreateClient(DcId id) {
        if (id.getType() == DcId.Type.MAIN && !id.equals(mainId()))
            return Mono.error(new IllegalArgumentException("Main clients can't created for this group"));

        var created = new boolean[]{false};
        return Mono.fromSupplier(() -> clients.computeIfAbsent(id, i -> {
            var dc = dcOptions.find(id)
                    .orElseThrow(() -> new IllegalArgumentException("No dc found for specified id: " + id));
            created[0] = true;
            return main.createChildClient(id.getType(), dc);
        }))
        .flatMap(c -> created[0] ? c.connect().thenReturn(c) : Mono.just(c));
    }

    @Override
    public DcOptions getDcOptions() {
        return dcOptions;
    }
}
