/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package telegram4j.mtproto.client;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.annotation.Nullable;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.DcId;
import telegram4j.mtproto.util.ResettableInterval;
import telegram4j.tl.api.TlMethod;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;

import static telegram4j.mtproto.internal.Preconditions.requireArgument;

/**
 * Default implementation of {@code MTProtoClientManager} with fixed
 * count of download/upload clients.
 */
public class DefaultMTProtoClientGroup implements MTProtoClientManager {

    // TODO:
    //  Client load should be determined by load on the socket,
    //  not on the RPC, since there are such requests as upload.getFile and upload.save*FilePart
    protected static final int FORK_REQUESTS_THRESHOLD = 20;

    protected static final VarHandle MAIN;

    static {
        var lookup = MethodHandles.lookup();
        try {
            MAIN = lookup.findVarHandle(DefaultMTProtoClientGroup.class, "main", MTProtoClient.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    protected final Options options;
    protected final ResettableInterval activityMonitoring = new ResettableInterval(Schedulers.single(),
            Sinks.many().unicast().onBackpressureError());
    protected final ConcurrentMap<Integer, Dc> dcs = new ConcurrentHashMap<>();
    protected volatile MTProtoClient main;

    protected volatile boolean terminated;

    public DefaultMTProtoClientGroup(Options options) {
        this.options = options;

        MAIN.set(this, createClient(DcId.Type.MAIN, options.mainDc));
    }

    @Override
    public MTProtoClient main() {
        return main;
    }

    @Override
    public Mono<MTProtoClient> setMain(DataCenter dc) {
        var newClient = createClient(DcId.Type.MAIN, dc);
        var oldClient = (MTProtoClient) MAIN.getAndSet(this, newClient);
        return Mono.when(oldClient.close(), newClient.connect())
                .thenReturn(newClient);
    }

    @Override
    public <R> Mono<R> send(DcId id, TlMethod<? extends R> method) {
        return getOrCreateClient(id)
                .flatMap(client -> client.send(method));
    }

    @Override
    public UpdateDispatcher updates() {
        return options.updateDispatcher;
    }

    @Override
    public Mono<Void> close() {
        return Mono.defer(() -> {
            terminated = true;

            activityMonitoring.close();

            var closeAll = new ArrayList<Mono<Void>>();
            for (Dc dc : dcs.values()) {
                dc.all((kind, clientSet) -> {
                    for (int i = 0; i < clientSet.size(); i++) {
                        var old = clientSet.get(i);
                        if (old != null) {
                            closeAll.add(old.close());
                        }
                    }
                });
            }
            closeAll.add(main.close());

            closeAll.add(options.updateDispatcher.close());

            closeAll.add(Mono.defer(() -> {
                if (options.mtProtoOptions.disposeResultPublisher()) {
                    options.mtProtoOptions.resultPublisher().shutdown();
                }
                return shutdownEventLoopGroup();
            }));

            return Mono.whenDelayError(closeAll);
        });
    }

    private Mono<Void> shutdownEventLoopGroup() {

        return Mono.create(sink -> {
            var group = options.mtProtoOptions.tcpClientResources().eventLoopGroup();

            var handle = group.shutdownGracefully();

            sink.onCancel(() -> handle.cancel(false));

            handle.addListener(future -> {
                if (handle.isCancelled()) {
                    return;
                }
                var t = future.cause();
                if (t != null) {
                    sink.error(t);
                } else {
                    sink.success();
                }
            });
        });
    }

    @Override
    public Mono<Void> start() {
        if (terminated) {
            return Mono.error(new IllegalStateException("Client group has been closed"));
        }

        activityMonitoring.start(options.checkinPeriod);

        return activityMonitoring.ticks()
                .flatMap(tick -> {
                    Instant now = Instant.now();

                    var toClose = new ArrayList<Mono<Void>>();
                    for (Dc dc : dcs.values()) {
                        dc.all((kind, clientSet) -> {
                            Duration inactivePeriod = kind == DcId.Type.DOWNLOAD
                                    ? options.inactiveDownloadPeriod
                                    : options.inactiveUploadPeriod;

                            for (int i = 0; i < clientSet.size(); i++) {
                                var client = clientSet.get(i);

                                if (client != null && isInactive(client, inactivePeriod, now)) {
                                    clientSet.remove(i);
                                    toClose.add(client.close());
                                }
                            }
                        });
                    }
                    return Mono.whenDelayError(toClose);
                })
                .then();
    }

    @Override
    public Mono<MTProtoClient> getOrCreateClient(DcId id) {
        if (terminated) {
            return Mono.error(new IllegalStateException("Client group has been closed"));
        }

        var type = id.getType();
        return switch (type) {
            case MAIN -> Mono.just(main);
            case UPLOAD, DOWNLOAD -> {
                int dcId = id.getId().orElseThrow();
                Dc dcInfo = dcs.computeIfAbsent(dcId, k -> new Dc(options));
                var clientSet = dcInfo.clientsFor(type);

                if (id.isAutoShift()) {
                    MTProtoClient lessLoaded = autoSelect(type, clientSet);

                    // condition `lessLoaded == null` is true when and only when client set is empty
                    if (lessLoaded == null ||
                            clientSet.activeCount() < clientSet.size() && isOverloaded(lessLoaded)) {

                        yield findDcOption(type, dcId)
                                .flatMap(dc -> {
                                    var newClient = createClient(type, dc);
                                    var c = clientSet.tryAdd(newClient);
                                    return c == newClient ? c.connect().thenReturn(c) : Mono.just(c);
                                });
                    }
                    yield Mono.just(lessLoaded);
                }

                int index = id.getShift().orElseThrow();
                if (index >= clientSet.size()) {
                    yield Mono.error(new IllegalArgumentException(
                            "Specified " + type.name().toLowerCase(Locale.US) +
                            " client shift out of bounds for size " + clientSet.size()));
                }

                var client = clientSet.get(index);
                if (client == null) {
                    yield findDcOption(type, dcId)
                            .flatMap(dc -> {
                                var newClient = createClient(type, dc);
                                MTProtoClient c = clientSet.trySet(index, newClient);
                                return c == newClient ? c.connect().thenReturn(c) : Mono.just(c);
                            });
                }

                yield Mono.just(client);
            }
        };
    }

    // Implementation code
    // ======================

    protected MTProtoClient createClient(DcId.Type type, DataCenter dcOption) {
        return options.clientFactory.create(this, type, dcOption);
    }

    protected boolean isInactive(MTProtoClient client, Duration inactivePeriod, Instant now) {
        return client.stats().lastQueryTimestamp()
                .map(ts -> ts.plus(inactivePeriod).isBefore(now))
                .orElse(true);
    }

    @Nullable
    protected MTProtoClient autoSelect(DcId.Type type, ClientSet clientSet) {
        MTProtoClient lessLoaded = null;
        for (int i = 0; i < clientSet.size(); i++) {
            var v = clientSet.get(i);
            if (v == null) {
                continue;
            }

            if (lessLoaded == null || v.stats().queriesCount() < lessLoaded.stats().queriesCount()) {
                lessLoaded = v;
            }
        }
        return lessLoaded;
    }

    protected boolean isOverloaded(MTProtoClient client) {
        return client.stats().queriesCount() >= FORK_REQUESTS_THRESHOLD;
    }

    protected Mono<DataCenter> findDcOption(DcId.Type type, int dcId) {
        return options.mtProtoOptions.storeLayout().getDcOptions()
                .handle((dcOpts, sink) -> dcOpts.find(type, dcId)
                        .ifPresentOrElse(sink::next, () -> sink.error(noDcOption(dcId))));
    }

    static IllegalArgumentException noDcOption(int id) {
        return new IllegalArgumentException("No DC option found for specified id: " + id);
    }

    public record Options(DataCenter mainDc, ClientFactory clientFactory,
                          UpdateDispatcher updateDispatcher, MTProtoOptions mtProtoOptions,
                          Duration checkinPeriod, Duration inactiveUploadPeriod,
                          Duration inactiveDownloadPeriod, int maxDownloadClientsCount,
                          int maxUploadClientsCount)
            implements MTProtoClientGroup.Options {

        public static final Duration DEFAULT_CHECKIN = Duration.ofMinutes(1);
        public static final Duration INACTIVE_UPLOAD_DURATION = Duration.ofMinutes(3);
        public static final Duration INACTIVE_DOWNLOAD_DURATION = Duration.ofMinutes(3);
        public static final int DEFAULT_MAX_DOWNLOAD_CLIENTS_COUNT = 4;
        public static final int DEFAULT_MAX_UPLOAD_CLIENTS_COUNT = 4;

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
            requireArgument(maxDownloadClientsCount >= 1, "maxDownloadClientsCount must be equal or greater than 1");
            requireArgument(maxUploadClientsCount >= 1, "maxUploadClientsCount must be equal or greater than 1");
        }
    }

    protected static class ClientSet {
        protected static final VarHandle CA;
        protected static final VarHandle AC;

        static {
            var lookup = MethodHandles.lookup();
            try {
                AC = lookup.findVarHandle(ClientSet.class, "activeCount", int.class);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }

            CA = MethodHandles.arrayElementVarHandle(MTProtoClient[].class);
        }

        private final MTProtoClient[] array;
        private volatile int activeCount;

        protected ClientSet(int clientCount) {
            this.array = new MTProtoClient[clientCount];
        }

        protected int size() {
            return array.length;
        }

        protected int activeCount() {
            return activeCount;
        }

        @Nullable
        protected MTProtoClient get(int index) {
            return (MTProtoClient) CA.getVolatile(array, index);
        }

        protected MTProtoClient trySet(int index, MTProtoClient client) {
            var result = (MTProtoClient) CA.compareAndExchange(array, index, null, client);
            if (result == null) {
                AC.getAndAdd(this, 1);
                return client;
            }
            return result;
        }

        protected MTProtoClient tryAdd(MTProtoClient client) {
            // trying to find first free positing and CAS client on it.
            // otherwise just return latest seen client.

            MTProtoClient latest = client;
            for (int i = 0; i < array.length; i++) {
                if ((latest = (MTProtoClient) CA.compareAndExchange(array, i, null, client)) == null) {
                    AC.getAndAdd(this, 1);
                    return client;
                }
            }
            return latest;
        }

        protected void remove(int index) {
            CA.setVolatile(array, index, null);
            AC.getAndAdd(this, -1);
        }
    }

    protected static class Dc {
        protected final ClientSet download;
        protected final ClientSet upload;

        protected Dc(Options options) {
            this.download = new ClientSet(options.maxDownloadClientsCount);
            this.upload = new ClientSet(options.maxUploadClientsCount);
        }

        protected ClientSet clientsFor(DcId.Type type) {
            return type == DcId.Type.DOWNLOAD ? download : upload;
        }

        protected void all(BiConsumer<DcId.Type, ClientSet> consumer) {
            consumer.accept(DcId.Type.DOWNLOAD, download);
            consumer.accept(DcId.Type.UPLOAD, upload);
        }
    }
}
