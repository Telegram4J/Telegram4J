package telegram4j.mtproto.client;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.DcId;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultMTProtoClientGroup implements MTProtoClientGroup {

    private static final int FORK_THRESHOLD = 20; // TODO
    private static final VarHandle CA;
    private static final VarHandle MAIN;

    static {
        var lookup = MethodHandles.lookup();
        try {
            MAIN = lookup.findVarHandle(DefaultMTProtoClientGroup.class, "main", MTProtoClient.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }

        CA = MethodHandles.arrayElementVarHandle(MTProtoClient[].class);
    }

    private final Options options;
    private final ResettableInterval activityMonitoring = new ResettableInterval(Schedulers.single(),
            Sinks.many().unicast().onBackpressureError());
    private final ConcurrentMap<Integer, Dc> dcs = new ConcurrentHashMap<>();
    private volatile MTProtoClient main;

    public DefaultMTProtoClientGroup(Options options) {
        this.options = options;

        MAIN.set(this, options.clientFactory.create(this, DcId.Type.MAIN, options.mainDc));
    }

    @Override
    public MTProtoClient main() {
        return main;
    }

    @Override
    public Mono<MTProtoClient> setMain(DataCenter dc) {
        var upd = options.clientFactory.create(this, DcId.Type.MAIN, dc);
        var old = (MTProtoClient) MAIN.getAndSet(this, upd);
        return old.close()
                .and(upd.connect())
                .thenReturn(upd);
    }

    @Override
    public <R> Mono<R> send(DcId id, TlMethod<? extends R> method) {
        return getOrCreateClient(id)
                .flatMap(client -> client.sendAwait(method));
    }

    @Override
    public UpdateDispatcher updates() {
        return options.updateDispatcher;
    }

    @Override
    public Mono<Void> close() {
        activityMonitoring.dispose();

        return Mono.whenDelayError(Stream.concat(dcs.values().stream()
                .flatMap(dc -> Stream.concat(Arrays.stream(dc.downloadClients),
                        Arrays.stream(dc.uploadClients))), Stream.of(main))
                .filter(c -> c != null)
                .map(MTProtoClient::close)
                .collect(Collectors.toList()))
                .then(Mono.fromRunnable(() -> {
                    options.updateDispatcher().shutdown();
                    options.mtProtoOptions().tcpClientResources().dispose();
                    options.mtProtoOptions().resultPublisher().shutdown();
                }));
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

                            if (d.stats().lastQueryTimestamp()
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

                            if (u.stats().lastQueryTimestamp()
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
        return switch (id.getType()) {
            case MAIN -> Mono.just(main);
            case UPLOAD, DOWNLOAD -> {
                int dcId = id.getId().orElseThrow();
                int maxCnt = id.getType() == DcId.Type.UPLOAD ? options.maxUploadClientsCount : options.maxDownloadClientsCount;
                Dc dcInfo = dcs.computeIfAbsent(dcId, k -> new Dc(options));
                var arr = id.getType() == DcId.Type.UPLOAD ? dcInfo.uploadClients : dcInfo.downloadClients;
                if (id.isAutoShift()) {
                    MTProtoClient lessLoaded = null;
                    for (int i = 0; i < arr.length; i++) {
                        MTProtoClient v = (MTProtoClient) CA.getVolatile(arr, i);
                        if (v == null) {
                            continue;
                        }

                        if (lessLoaded == null || v.stats().queriesCount() < lessLoaded.stats().queriesCount()) {
                            lessLoaded = v;
                        }
                    }

                    var activeCount = id.getType() == DcId.Type.UPLOAD ? dcInfo.activeUploadClientsCount : dcInfo.activeDownloadClientsCount;
                    if ((lessLoaded == null || lessLoaded.stats().queriesCount() >= FORK_THRESHOLD) && activeCount.get() < maxCnt) {

                        yield options.mtProtoOptions.storeLayout().getDcOptions()
                                .map(dcOpts -> dcOpts.find(id.getType(), dcId)
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                "No dc found for specified id: " + id)))
                                .flatMap(dc -> {
                                    MTProtoClient created = options.clientFactory.create(this, id.getType(), dc);
                                    MTProtoClient c = dcInfo.setClient(id.getType(), created);
                                    return c == created ? c.connect().thenReturn(c) : Mono.just(c);
                                });
                    }
                    yield Mono.just(lessLoaded);
                }

                int index = id.getShift().orElseThrow();
                if (index >= maxCnt) {
                    yield Mono.error(new IllegalArgumentException("Too big " + id.getType().name()
                            .toLowerCase(Locale.US) + " client shift: " + id.getShift() +
                            " >= " + maxCnt));
                }

                yield Mono.justOrEmpty((MTProtoClient) CA.getVolatile(arr, index))
                        .switchIfEmpty(Mono.defer(() -> options.mtProtoOptions.storeLayout().getDcOptions()
                                .map(dcOpts -> dcOpts.find(id.getType(), dcId)
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                "No dc found for specified id: " + id)))
                                .flatMap(dc -> {
                                    MTProtoClient created = options.clientFactory.create(this, id.getType(), dc);
                                    MTProtoClient c = dcInfo.setClient(id.getType(), index, created);
                                    return c == created ? c.connect().thenReturn(c) : Mono.just(c);
                                })));
            }
        };
    }

    public record Options(DataCenter mainDc, ClientFactory clientFactory,
                          UpdateDispatcher updateDispatcher, MTProtoOptions mtProtoOptions,
                          Duration checkinPeriod, Duration inactiveUploadPeriod,
                          Duration inactiveDownloadPeriod, int maxDownloadClientsCount,
                          int maxUploadClientsCount)
            implements MTProtoClientGroup.Options {

        static final Duration DEFAULT_CHECKIN = Duration.ofMinutes(1);
        static final Duration INACTIVE_UPLOAD_DURATION = Duration.ofMinutes(3);
        static final Duration INACTIVE_DOWNLOAD_DURATION = Duration.ofMinutes(3);
        static final int DEFAULT_MAX_DOWNLOAD_CLIENTS_COUNT = 4;
        static final int DEFAULT_MAX_UPLOAD_CLIENTS_COUNT = 4;

        public Options(MTProtoClientGroup.Options options) {
            this(options.mainDc(), options.clientFactory(), options.updateDispatcher(), options.mtProtoOptions());
        }

        public Options(DataCenter mainDc, ClientFactory clientFactory,
                       UpdateDispatcher updateDispatcher, MTProtoOptions mtProtoOptions) {
            this(mainDc, clientFactory, updateDispatcher, mtProtoOptions,
                    DEFAULT_CHECKIN, INACTIVE_UPLOAD_DURATION,
                    INACTIVE_DOWNLOAD_DURATION, DEFAULT_MAX_DOWNLOAD_CLIENTS_COUNT,
                    DEFAULT_MAX_UPLOAD_CLIENTS_COUNT);
        }

        public Options {
            if (maxDownloadClientsCount <= 0)
                throw new IllegalArgumentException("Download client count must be equal or greater than 1");
            if (maxUploadClientsCount <= 0)
                throw new IllegalArgumentException("Upload client count must be equal or greater than 1");
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
