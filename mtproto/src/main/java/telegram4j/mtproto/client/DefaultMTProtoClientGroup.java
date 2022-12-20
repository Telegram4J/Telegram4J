package telegram4j.mtproto.client;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.concurrent.Queues;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.DcId;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.mtproto.util.ResettableInterval;
import telegram4j.tl.api.TlMethod;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultMTProtoClientGroup implements MTProtoClientGroup {

    private static final VarHandle CA = MethodHandles.arrayElementVarHandle(MTProtoClient[].class);

    private final Options options;
    private final ResettableInterval activityMonitoring = new ResettableInterval(Schedulers.parallel(),
            Sinks.many().unicast().onBackpressureBuffer(Queues.<Long>get(Queues.XS_BUFFER_SIZE).get()));
    private final ConcurrentMap<Integer, Dc> dcs = new ConcurrentHashMap<>();
    private final AtomicReference<MainMTProtoClient> main = new AtomicReference<>();

    public DefaultMTProtoClientGroup(Options options) {
        this.options = options;

        this.main.set(options.mainClient);
    }

    @Override
    public MainMTProtoClient main() {
        return main.get();
    }

    @Override
    public Mono<MainMTProtoClient> setMain(DataCenter dc) {
        MainMTProtoClient upd = options.clientFactory.createMain(dc);
        MainMTProtoClient old = main.getAndSet(upd);
        return old.close()
                .and(upd.connect())
                .thenReturn(upd);
    }

    @Override
    public <R, M extends TlMethod<R>> Mono<R> send(DcId id, M method) {
        return getOrCreateClient(id)
                .flatMap(client -> client.sendAwait(method));
    }

    @Override
    public Mono<Void> close() {
        activityMonitoring.dispose();

        return Mono.whenDelayError(Stream.concat(dcs.values().stream()
                .flatMap(dc -> Stream.concat(Arrays.stream(dc.downloadClients),
                        Arrays.stream(dc.uploadClients))),
                        Stream.of(main.get()))
                .map(MTProtoClient::close)
                .collect(Collectors.toList()));
    }

    @Override
    public Mono<Void> start() {
        activityMonitoring.start(options.checkinPeriod);

        // TODO: disconnect client instead of closing them
        return activityMonitoring.ticks()
                .flatMap(tick -> {
                    List<Mono<Void>> toDisconnect = new LinkedList<>();

                    for (Dc dc : dcs.values()) {
                        for (int i = 0; i < dc.downloadClients.length; i++) {
                            MTProtoClient d = (MTProtoClient) CA.getVolatile(dc.downloadClients, i);
                            if (d == null) continue;

                            if (d.getStats().getLastQueryTimestamp()
                                    .map(ts -> ts.plus(options.inactiveDownloadPeriod).isBefore(Instant.now()))
                                    .orElse(true)) {
                                CA.setVolatile(dc.downloadClients, i, null);
                                dc.activeDownloadClientsCount.decrementAndGet();
                                toDisconnect.add(d.close());
                            }
                        }

                        for (int i = 0; i < dc.uploadClients.length; i++) {
                            MTProtoClient u = (MTProtoClient) CA.getVolatile(dc.uploadClients, i);
                            if (u == null) continue;

                            if (u.getStats().getLastQueryTimestamp()
                                    .map(ts -> ts.plus(options.inactiveUploadPeriod).isBefore(Instant.now()))
                                    .orElse(true)) {
                                CA.setVolatile(dc.uploadClients, i, null);
                                dc.activeUploadClientsCount.decrementAndGet();
                                toDisconnect.add(u.close());
                            }
                        }
                    }
                    return Mono.whenDelayError(toDisconnect);
                })
                .then();
    }

    @Override
    public Mono<MTProtoClient> getOrCreateClient(DcId id) {
        switch (id.getType()) {
            case MAIN:
                return Mono.just(main.get());
            case UPLOAD:
            case DOWNLOAD: {
                int dcId = id.getId().orElseThrow();
                if (id.isAutoShift()) {
                    Dc dcInfo = dcs.computeIfAbsent(dcId, k -> new Dc(options));
                    var arr = id.getType() == DcId.Type.UPLOAD ? dcInfo.uploadClients : dcInfo.downloadClients;
                    MTProtoClient lessLoaded = null;
                    for (int i = 0; i < arr.length; i++) {
                        MTProtoClient v = (MTProtoClient) CA.getVolatile(arr, i);
                        if (v == null) {
                            continue;
                        }

                        if (lessLoaded == null || v.getStats().getQueriesCount() < lessLoaded.getStats().getQueriesCount()) {
                            lessLoaded = v;
                        }
                    }

                    if (lessLoaded == null || lessLoaded.getStats().getQueriesCount() != 0 &&
                            dcInfo.activeDownloadClientsCount.get() < arr.length) {
                        return options.storeLayout.getDcOptions()
                                .map(dcOpts -> dcOpts.find(id.getType(), dcId)
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                "No dc found for specified id: " + id)))
                                .flatMap(dc -> {
                                    MTProtoClient created = options.clientFactory.create(dc);
                                    MTProtoClient c = dcInfo.setClient(id.getType(), created);
                                    return c == created ? c.connect().thenReturn(c) : Mono.just(c);
                                });
                    }
                    return Mono.just(lessLoaded);
                }

                int index = id.getShift().orElseThrow();
                int max = id.getType() == DcId.Type.UPLOAD ? options.maxUploadClientsCount : options.maxDownloadClientsCount;
                if (index >= max) {
                    return Mono.error(new IllegalArgumentException("Too big " + id.getType().name()
                            .toLowerCase(Locale.US) + " client shift: " + id.getShift() +
                            " >= " + max));
                }

                Dc dcInfo = dcs.computeIfAbsent(dcId, k -> new Dc(options));
                var arr = id.getType() == DcId.Type.UPLOAD ? dcInfo.uploadClients : dcInfo.downloadClients;
                return Mono.justOrEmpty((MTProtoClient) CA.getVolatile(arr, index))
                        .switchIfEmpty(Mono.defer(() -> options.storeLayout.getDcOptions()
                                .map(dcOpts -> dcOpts.find(id.getType(), dcId)
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                "No dc found for specified id: " + id)))
                                .flatMap(dc -> {
                                    MTProtoClient created = options.clientFactory.create(dc);
                                    MTProtoClient c = dcInfo.setClient(id.getType(), index, created);
                                    return c == created ? c.connect().thenReturn(c) : Mono.just(c);
                                })));
            }
            default:
                return Mono.error(new IllegalStateException());
        }
    }

    public static class Options extends MTProtoClientGroupOptions {

        static final Duration DEFAULT_CHECKIN = Duration.ofMinutes(1);
        static final Duration INACTIVE_UPLOAD_DURATION = Duration.ofMinutes(2);
        static final Duration INACTIVE_DOWNLOAD_DURATION = Duration.ofMinutes(1);
        static final int DEFAULT_MAX_DOWNLOAD_CLIENTS_COUNT = 4;
        static final int DEFAULT_MAX_UPLOAD_CLIENTS_COUNT = 4;

        public final Duration checkinPeriod;
        public final Duration inactiveUploadPeriod;
        public final Duration inactiveDownloadPeriod;
        public final int maxDownloadClientsCount;
        public final int maxUploadClientsCount;

        public Options(MTProtoClientGroupOptions options) {
            this(options.mainClient, options.clientFactory, options.storeLayout);
        }

        public Options(MainMTProtoClient mainClient, ClientFactory clientFactory, StoreLayout storeLayout) {
            this(mainClient, clientFactory, storeLayout, DEFAULT_CHECKIN, INACTIVE_UPLOAD_DURATION,
                    INACTIVE_DOWNLOAD_DURATION, DEFAULT_MAX_DOWNLOAD_CLIENTS_COUNT, DEFAULT_MAX_UPLOAD_CLIENTS_COUNT);
        }

        public Options(MainMTProtoClient mainClient, ClientFactory clientFactory, StoreLayout storeLayout,
                       Duration checkinPeriod, Duration inactiveUploadPeriod, Duration inactiveDownloadPeriod,
                       int maxDownloadClientsCount, int maxUploadClientsCount) {
            super(mainClient, clientFactory, storeLayout);
            if (maxDownloadClientsCount <= 0)
                throw new IllegalArgumentException("Download client count must be equal or greater than 1");
            if (maxUploadClientsCount <= 0)
                throw new IllegalArgumentException("Upload client count must be equal or greater than 1");
            this.checkinPeriod = checkinPeriod;
            this.inactiveUploadPeriod = inactiveUploadPeriod;
            this.inactiveDownloadPeriod = inactiveDownloadPeriod;
            this.maxDownloadClientsCount = maxDownloadClientsCount;
            this.maxUploadClientsCount = maxUploadClientsCount;
        }
    }

    static class Dc {
        final MTProtoClient[] downloadClients;
        final MTProtoClient[] uploadClients;
        final AtomicInteger activeDownloadClientsCount = new AtomicInteger();
        final AtomicInteger activeUploadClientsCount = new AtomicInteger();

        Dc(Options options) {
            this.downloadClients = new MTProtoClient[options.maxDownloadClientsCount];
            this.uploadClients = new MTProtoClient[options.maxUploadClientsCount];
        }

        MTProtoClient setClient(DcId.Type type, MTProtoClient client) {
            var arr = type == DcId.Type.UPLOAD ? uploadClients : downloadClients;
            MTProtoClient latest = client;
            for (int i = 0; i < arr.length; i++) {
                // trying to find first *unused* positing and CAS client on it.
                // otherwise just return latest seen client.
                if ((latest = (MTProtoClient)CA.compareAndExchange(arr, i, null, client)) == null) {
                    if (type == DcId.Type.UPLOAD) {
                        activeUploadClientsCount.incrementAndGet();
                    } else {
                        activeDownloadClientsCount.incrementAndGet();
                    }
                    return client;
                }
            }
            return latest;
        }

        MTProtoClient setClient(DcId.Type type, int index, MTProtoClient client) {
            var arr = type == DcId.Type.UPLOAD ? uploadClients : downloadClients;
            MTProtoClient res = (MTProtoClient)CA.compareAndExchange(arr, index, null, client);
            if (res == null) {
                if (type == DcId.Type.UPLOAD) {
                    activeUploadClientsCount.incrementAndGet();
                } else {
                    activeDownloadClientsCount.incrementAndGet();
                }
                return client;
            }
            return res;
        }
    }
}
