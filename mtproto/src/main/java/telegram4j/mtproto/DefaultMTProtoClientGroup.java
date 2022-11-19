package telegram4j.mtproto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.api.TlMethod;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultMTProtoClientGroup implements MTProtoClientGroup {

    private final ClientCategory[] categories = {
            new ClientCategory(DcId.Type.REGULAR),
            new ClientCategory(DcId.Type.UPLOAD),
            new ClientCategory(DcId.Type.DOWNLOAD)
    };

    private final StoreLayout storeLayout;
    private final MainMTProtoClient main;
    private final Options options;

    private volatile DcOptions dcOptions;

    static class ClientCategory {
        final DcId.Type type;
        final AtomicInteger counter = new AtomicInteger();
        final Queue<Tuple3<Integer, Integer, MTProtoClient>> clients = new ConcurrentLinkedQueue<>();

        ClientCategory(DcId.Type type) {
            this.type = type;
        }
    }

    public DefaultMTProtoClientGroup(MTProtoClientGroupOptions options, Options options1) {
        this.storeLayout = options.storeLayout;
        this.main = options.mainClient;
        this.dcOptions = options.dcOptions;
        this.options = options1;
    }

    @Override
    public MainMTProtoClient main() {
        return main;
    }

    @Override
    public DcId mainId() {
        return DcId.main(main.getDatacenter().getId());
    }

    @Override
    public <R, M extends TlMethod<R>> Mono<R> send(DcId id, M method) {
        if (id.equals(mainId())) {
            return main.sendAwait(method);
        }

        var cat = categories[id.getType().ordinal() - 1];

        Mono<MTProtoClient> client;
        if (id.getShift() == DcId.AUTO_SHIFT) {
            client = Mono.justOrEmpty(cat.clients.stream()
                    .filter(e -> e.getT1() == id.getId())
                    .min(Comparator.comparingInt(c -> c.getT3().getStats().getQueriesCount()))
                    .map(Tuple3::getT3));
        } else {
            client = Mono.justOrEmpty(cat.clients.stream()
                    .filter(e -> e.getT1() == id.getId() && e.getT2() == id.getShift())
                    .findFirst()
                    .map(Tuple3::getT3));
        }

        return client
                .switchIfEmpty(Mono.error(() -> new IllegalArgumentException("No client found for dcId: " + id)))
                .flatMap(c -> c.sendAwait(method));
    }

    @Override
    public Mono<Void> close() {
        return Mono.whenDelayError(Stream.concat(Arrays.stream(categories)
                        .flatMap(c -> c.clients.stream())
                        .map(Tuple3::getT3), Stream.of(main))
                .map(MTProtoClient::close)
                .collect(Collectors.toList()));
    }

    @Override
    public Mono<Void> start() {
        var initDcOptions = Mono.defer(() -> {
            var curr = dcOptions;
            if (curr != null) {
                return storeLayout.updateDcOptions(dcOptions);
            }
            return storeLayout.getDcOptions()
                    .doOnNext(opts -> dcOptions = opts);
        });

        var inactivitySchedule = Flux.interval(options.checkinPeriod)
                .flatMap(tick -> {
                    List<Mono<Void>> toClose = new LinkedList<>();

                    for (ClientCategory category : categories) {
                        Duration inactivity;
                        switch (category.type) {
                            case REGULAR:
                                inactivity = options.inactiveRegularPeriod;
                                break;
                            case UPLOAD:
                                inactivity = options.inactiveUploadPeriod;
                                break;
                            case DOWNLOAD:
                                inactivity = options.inactiveDownloadPeriod;
                                break;
                            default:
                                throw new IllegalStateException();
                        }

                        for (var it = category.clients.iterator(); it.hasNext();) {
                            var entry = it.next();
                            var isInactive = entry.getT3().getStats().getLastQueryTimestamp()
                                    .map(ts -> ts.plus(inactivity).isBefore(Instant.now()))
                                    .orElse(true);

                            if (isInactive) {
                                it.remove();
                                toClose.add(entry.getT3().close());
                            }
                        }
                    }

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
        if (id.getType() == DcId.Type.REGULAR && id.getId() != mainId().getId())
            return Mono.error(new IllegalArgumentException("Regular clients in this group can't be connected different DC"));
        if (id.equals(mainId()))
            return Mono.just(main);

        var cat = categories[id.getType().ordinal() - 1];

        if (id.getShift() == DcId.AUTO_SHIFT) {
            return Mono.justOrEmpty(cat.clients.stream()
                            .filter(e -> e.getT1() == id.getId())
                            .min(Comparator.comparingInt(c -> c.getT3().getStats().getQueriesCount()))
                            .map(Tuple3::getT3))
                    .switchIfEmpty(Mono.defer(() -> {
                        var dc = dcOptions.find(id)
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "No dc found for specified id: " + id));
                        var c = main.createChildClient(id.getType(), dc);
                        cat.clients.add(Tuples.of(id.getId(), cat.counter.getAndIncrement(), c));
                        return c.connect().thenReturn(c);
                    }));
        }

        return Mono.justOrEmpty(cat.clients.stream()
                        .filter(e -> e.getT1() == id.getId() && e.getT2() == id.getShift())
                        .findFirst()
                        .map(Tuple3::getT3))
                .switchIfEmpty(Mono.defer(() -> {
                    var dc = dcOptions.find(id)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "No dc found for specified id: " + id));
                    var c = main.createChildClient(id.getType(), dc);
                    cat.clients.add(Tuples.of(id.getId(), id.getShift(), c));
                    return c.connect().thenReturn(c);
                }));
    }

    @Override
    public DcOptions getDcOptions() {
        return dcOptions;
    }

    public static class Options {

        static final Duration DEFAULT_CHECKIN = Duration.ofMinutes(1);
        static final Duration INACTIVE_REGULAR_DURATION = Duration.ofMinutes(15);
        static final Duration INACTIVE_UPLOAD_DURATION = Duration.ofMinutes(2);
        static final Duration INACTIVE_DOWNLOAD_DURATION = Duration.ofMinutes(3);

        public final Duration checkinPeriod;
        public final Duration inactiveRegularPeriod;
        public final Duration inactiveUploadPeriod;
        public final Duration inactiveDownloadPeriod;

        public Options() {
            this.checkinPeriod = DEFAULT_CHECKIN;
            this.inactiveRegularPeriod = INACTIVE_REGULAR_DURATION;
            this.inactiveUploadPeriod = INACTIVE_UPLOAD_DURATION;
            this.inactiveDownloadPeriod = INACTIVE_DOWNLOAD_DURATION;
        }

        public Options(Duration checkinPeriod, Duration inactiveRegularPeriod,
                       Duration inactiveUploadPeriod, Duration inactiveDownloadPeriod) {
            this.checkinPeriod = checkinPeriod;
            this.inactiveRegularPeriod = inactiveRegularPeriod;
            this.inactiveUploadPeriod = inactiveUploadPeriod;
            this.inactiveDownloadPeriod = inactiveDownloadPeriod;
        }
    }
}
