package telegram4j.core;

import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.tcp.TcpClient;
import reactor.scheduler.forkjoin.ForkJoinPoolScheduler;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.annotation.Nullable;
import reactor.util.concurrent.Queues;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;
import telegram4j.core.AuthorizationResources.Type;
import telegram4j.core.event.DefaultEventDispatcher;
import telegram4j.core.event.EventDispatcher;
import telegram4j.core.event.dispatcher.UpdatesHandlers;
import telegram4j.core.object.Id;
import telegram4j.core.retriever.EntityRetriever;
import telegram4j.core.retriever.RpcEntityRetriever;
import telegram4j.core.util.EntityParser;
import telegram4j.mtproto.*;
import telegram4j.mtproto.service.ServiceHolder;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.mtproto.store.StoreLayoutImpl;
import telegram4j.mtproto.transport.IntermediateTransport;
import telegram4j.mtproto.transport.Transport;
import telegram4j.mtproto.util.EmissionHandlers;
import telegram4j.tl.InputUserSelf;
import telegram4j.tl.api.TlObject;
import telegram4j.tl.request.InitConnection;
import telegram4j.tl.request.InvokeWithLayer;
import telegram4j.tl.request.auth.ImmutableImportBotAuthorization;
import telegram4j.tl.request.updates.GetState;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

public final class MTProtoBootstrap<O extends MTProtoOptions> {

    private static final boolean parseBotIdFromToken = Boolean.getBoolean("telegram4j.core.MTProtoBootstrap.parseBotIdFromToken");
    private static final Logger log = Loggers.getLogger(MTProtoBootstrap.class);

    private final Function<MTProtoOptions, ? extends O> optionsModifier;
    private final AuthorizationResources authResources;

    private TcpClient tcpClient;
    private Supplier<Transport> transport;
    private int acksSendThreshold = 3;
    private RetryBackoffSpec retry;
    private RetryBackoffSpec authRetry;
    private IntPredicate gzipPackingPredicate;

    @Nullable
    private Function<String, EntityParser> defaultEntityParserFactory;
    private Function<MTProtoTelegramClient, EntityRetriever> entityRetrieverFactory;
    private UpdatesHandlers updatesHandlers = UpdatesHandlers.instance;

    private InitConnectionParams initConnectionParams;
    private StoreLayout storeLayout;
    private EventDispatcher eventDispatcher;
    private DataCenter dataCenter;

    MTProtoBootstrap(Function<MTProtoOptions, ? extends O> optionsModifier, AuthorizationResources authResources) {
        this.optionsModifier = optionsModifier;
        this.authResources = authResources;
    }

    public <O1 extends MTProtoOptions> MTProtoBootstrap<O1> setExtraOptions(Function<? super O, ? extends O1> optionsModifier) {
        return new MTProtoBootstrap<>(this.optionsModifier.andThen(optionsModifier), authResources);
    }

    public MTProtoBootstrap<O> setStoreLayout(StoreLayout storeLayout) {
        this.storeLayout = Objects.requireNonNull(storeLayout, "storeLayout");
        return this;
    }

    public MTProtoBootstrap<O> setAcksSendThreshold(int acksSendThreshold) {
        this.acksSendThreshold = acksSendThreshold;
        return this;
    }

    public MTProtoBootstrap<O> setTransport(Supplier<Transport> transport) {
        this.transport = Objects.requireNonNull(transport, "transport");
        return this;
    }

    public MTProtoBootstrap<O> setTcpClient(TcpClient tcpClient) {
        this.tcpClient = Objects.requireNonNull(tcpClient, "tcpClient");
        return this;
    }

    public MTProtoBootstrap<O> setInitConnectionParams(InitConnectionParams initConnectionParams) {
        this.initConnectionParams = Objects.requireNonNull(initConnectionParams, "initConnectionParams");
        return this;
    }

    public MTProtoBootstrap<O> setEventDispatcher(EventDispatcher eventDispatcher) {
        this.eventDispatcher = Objects.requireNonNull(eventDispatcher, "eventDispatcher");
        return this;
    }

    public MTProtoBootstrap<O> setDataCenter(DataCenter dataCenter) {
        this.dataCenter = Objects.requireNonNull(dataCenter, "dataCenter");
        return this;
    }

    public MTProtoBootstrap<O> setUpdatesHandlers(UpdatesHandlers updatesHandlers) {
        this.updatesHandlers = Objects.requireNonNull(updatesHandlers, "updatesHandlers");
        return this;
    }

    public MTProtoBootstrap<O> setRetry(RetryBackoffSpec retry) {
        this.retry = Objects.requireNonNull(retry, "retry");
        return this;
    }

    public MTProtoBootstrap<O> setAuthRetry(RetryBackoffSpec authRetry) {
        this.authRetry = Objects.requireNonNull(authRetry, "authRetry");
        return this;
    }

    public MTProtoBootstrap<O> setEntityRetrieverFactory(Function<MTProtoTelegramClient, EntityRetriever> entityRetrieverFactory) {
        this.entityRetrieverFactory = Objects.requireNonNull(entityRetrieverFactory, "entityRetrieverFactory");
        return this;
    }

    public MTProtoBootstrap<O> setGzipPackingPredicate(IntPredicate gzipPackingPredicate) {
        this.gzipPackingPredicate = Objects.requireNonNull(gzipPackingPredicate, "gzipPackingPredicate");
        return this;
    }

    public MTProtoBootstrap<O> setDefaultEntityParserFactory(Function<String, EntityParser> defaultEntityParserFactory) {
        this.defaultEntityParserFactory = Objects.requireNonNull(defaultEntityParserFactory, "defaultEntityParserFactory");
        return this;
    }

    public Mono<Void> withConnection(Function<MTProtoTelegramClient, ? extends Publisher<?>> func) {
        return Mono.usingWhen(connect(), client -> Flux.from(func.apply(client)).then(client.onDisconnect()),
                MTProtoTelegramClient::disconnect);
    }

    public Mono<MTProtoTelegramClient> connect() {
        return connect(DefaultMTProtoClient::new);
    }

    public Mono<MTProtoTelegramClient> connect(Function<? super O, ? extends MTProtoClient> clientFactory) {
        return Mono.create(sink -> {
            StoreLayout storeLayout = initStoreLayout();
            EventDispatcher eventDispatcher = initEventDispatcher();
            Sinks.Empty<Void> onDisconnect = Sinks.empty();

            MTProtoClient mtProtoClient = clientFactory.apply(optionsModifier.apply(
                    new MTProtoOptions(initDataCenter(), initTcpClient(), initTransport(),
                            storeLayout, acksSendThreshold, EmissionHandlers.DEFAULT_PARKING,
                            initRetry(), initAuthRetry(), initGzipPackingPredicate())));

            MTProtoResources mtProtoResources = new MTProtoResources(storeLayout, eventDispatcher, defaultEntityParserFactory);
            ServiceHolder serviceHolder = new ServiceHolder(mtProtoClient, storeLayout);
            var invokeWithLayout = InvokeWithLayer.builder()
                    .layer(MTProtoTelegramClient.LAYER)
                    .query(initConnection())
                    .build();

            AtomicReference<Id> selfId = new AtomicReference<>();
            MTProtoTelegramClient telegramClient = new MTProtoTelegramClient(
                    authResources, mtProtoClient,
                    mtProtoResources, updatesHandlers, selfId,
                    serviceHolder, initEntityRetrieverFactory(), onDisconnect.asMono());

            Mono<Void> disconnect = Mono.fromRunnable(() -> {
                eventDispatcher.shutdown();
                telegramClient.getUpdatesManager().shutdown();
                onDisconnect.emitEmpty(Sinks.EmitFailureHandler.FAIL_FAST);
                log.info("All mtproto clients disconnected.");
            });

            Disposable.Composite composite = Disposables.composite();

            composite.add(mtProtoClient.connect()
                    .doFinally(signal -> {
                        sink.success();
                        onDisconnect.emitEmpty(Sinks.EmitFailureHandler.FAIL_FAST);
                    })
                    .subscribe(null, t -> log.error("MTProto client terminated with an error", t),
                            () -> log.debug("MTProto client completed")));

            composite.add(mtProtoClient.updates().asFlux()
                    .takeUntilOther(onDisconnect.asMono())
                    .checkpoint("Event dispatch handler.")
                    .flatMap(telegramClient.getUpdatesManager()::handle)
                    .doOnNext(eventDispatcher::publish)
                    .subscribe(null, t -> log.error("Event dispatcher terminated with an error", t),
                            () -> log.debug("Event dispatcher completed")));

            composite.add(telegramClient.getUpdatesManager().start().subscribe(null,
                    t -> log.error("Updates manager terminated with an error", t),
                    () -> log.debug("Updates manager completed")));

            composite.add(mtProtoClient.state()
                    .takeUntilOther(onDisconnect.asMono())
                    .flatMap(state -> {
                        switch (state) {
                            case CLOSED: return disconnect;
                            case CONNECTED:
                                // delegate all auth work to the user and trigger authorization only if auth key is new
                                Mono<Void> userAuth = Mono.justOrEmpty(authResources.getAuthHandler())
                                        .flatMapMany(f -> Flux.from(f.apply(telegramClient)))
                                        .then();

                                Mono<Void> fetchSelfId = Mono.defer(() -> {
                                            // bot user id writes before ':' char
                                            if (authResources.getType() == Type.BOT && parseBotIdFromToken) {
                                                return Mono.fromSupplier(() -> Id.ofUser(authResources.getBotAuthToken()
                                                        .map(t -> Long.parseLong(t.split(":")[0]))
                                                        .orElseThrow(), null));
                                            }
                                            return storeLayout.getSelfId()
                                                    .filter(l -> l != -1)
                                                    .map(l -> Id.ofUser(l, null));
                                        })
                                        .switchIfEmpty(serviceHolder.getUserService()
                                                .getFullUser(InputUserSelf.instance())
                                                .flatMap(user -> storeLayout.updateSelfId(user.fullUser().id())
                                                        .thenReturn(Id.ofUser(user.fullUser().id(), null))))
                                        .doOnNext(id -> selfId.compareAndSet(null, id))
                                        .then();

                                return mtProtoClient.sendAwait(invokeWithLayout)
                                        // The best way to check that authorization is needed
                                        .retryWhen(Retry.indefinitely()
                                                .filter(e -> authResources.getType() == Type.USER &&
                                                        e instanceof RpcException &&
                                                        ((RpcException) e).getError().errorCode() == 401)
                                                .doBeforeRetryAsync(signal -> userAuth))
                                        .then(fetchSelfId)
                                        .then(telegramClient.getUpdatesManager().fillGap())
                                        .thenReturn(telegramClient) // FIXME: reconnections drop signals
                                        .doOnNext(sink::success);
                            default:
                                return Mono.empty();
                        }
                    })
                    .subscribe(null, t -> log.error("State handler terminated with an error", t),
                            () -> log.debug("State handler completed")));

            sink.onCancel(composite);
        });
    }

    // Resources initialization
    // ==========================

    private InitConnection<TlObject> initConnection() {
        InitConnectionParams params = initConnectionParams != null
                ? initConnectionParams
                : InitConnectionParams.getDefault();

        var initConnection = InitConnection.builder()
                .apiId(authResources.getApiId())
                .appVersion(params.getAppVersion())
                .deviceModel(params.getDeviceModel())
                .langCode(params.getLangCode())
                .langPack(params.getLangPack())
                .systemVersion(params.getSystemVersion())
                .systemLangCode(params.getSystemLangCode());

        if (authResources.getType() == Type.BOT) {
            initConnection.query(ImmutableImportBotAuthorization.of(0, authResources.getApiId(),
                    authResources.getApiHash(), authResources.getBotAuthToken().orElseThrow()));
        } else {
            initConnection.query(GetState.instance());
        }

        params.getProxy().ifPresent(initConnection::proxy);
        params.getParams().ifPresent(initConnection::params);

        return initConnection.build();
    }

    private Supplier<Transport> initTransport() {
        if (transport != null) {
            return transport;
        }
        return () -> new IntermediateTransport(true);
    }

    private TcpClient initTcpClient() {
        if (tcpClient != null) {
            return tcpClient;
        }
        return TcpClient.create();
    }

    private EventDispatcher initEventDispatcher() {
        if (eventDispatcher != null) {
            return eventDispatcher;
        }
        return new DefaultEventDispatcher(ForkJoinPoolScheduler.create("t4j-events"),
                Sinks.many().multicast().onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false),
                EmissionHandlers.DEFAULT_PARKING);
    }

    private StoreLayout initStoreLayout() {
        if (storeLayout != null) {
            return storeLayout;
        }
        return new StoreLayoutImpl(c -> c.maximumSize(1000));
    }

    private DataCenter initDataCenter() {
        if (dataCenter != null) {
            return dataCenter;
        }
        return DataCenter.productionDataCentersIpv4.get(1); // dc#2
    }

    private RetryBackoffSpec initRetry() {
        if (retry != null) {
            return retry;
        }
        return Retry.fixedDelay(Integer.MAX_VALUE, Duration.ofSeconds(5));
    }

    private RetryBackoffSpec initAuthRetry() {
        if (authRetry != null) {
            return authRetry;
        }
        return Retry.fixedDelay(5, Duration.ofSeconds(3));
    }

    private Function<MTProtoTelegramClient, EntityRetriever> initEntityRetrieverFactory() {
        if (entityRetrieverFactory != null) {
            return entityRetrieverFactory;
        }
        return RpcEntityRetriever::new;
    }

    private IntPredicate initGzipPackingPredicate() {
        if (gzipPackingPredicate != null) {
            return gzipPackingPredicate;
        }
        return i -> i >= 1024 * 16; // gzip packets if size is larger of 16kb
    }
}
