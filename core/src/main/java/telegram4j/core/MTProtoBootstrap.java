package telegram4j.core;

import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.netty.tcp.TcpClient;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.concurrent.Queues;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;
import telegram4j.core.event.DefaultEventDispatcher;
import telegram4j.core.event.EventDispatcher;
import telegram4j.core.event.dispatcher.UpdatesHandlers;
import telegram4j.mtproto.*;
import telegram4j.mtproto.service.ServiceHolder;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.mtproto.store.StoreLayoutImpl;
import telegram4j.mtproto.transport.IntermediateTransport;
import telegram4j.mtproto.transport.Transport;
import telegram4j.mtproto.util.EmissionHandlers;
import telegram4j.tl.InputUserSelf;
import telegram4j.tl.api.TlMethod;
import telegram4j.tl.api.TlObject;
import telegram4j.tl.auth.Authorization;
import telegram4j.tl.request.InitConnection;
import telegram4j.tl.request.InvokeWithLayer;
import telegram4j.tl.request.auth.ImportBotAuthorization;
import telegram4j.tl.request.help.GetConfig;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public final class MTProtoBootstrap<O extends MTProtoOptions> {

    private static final Logger log = Loggers.getLogger(MTProtoBootstrap.class);

    private final Function<MTProtoOptions, ? extends O> optionsModifier;
    private final AuthorizationResources authorizationResources;

    private TcpClient tcpClient;
    private Supplier<Transport> transport;
    private int acksSendThreshold = 3;
    private RetryBackoffSpec retry;

    private UpdatesHandlers updatesHandlers = UpdatesHandlers.instance;
    private MTProtoClientManager mtProtoClientManager;

    private InitConnectionParams initConnectionParams;
    private StoreLayout storeLayout;
    private EventDispatcher eventDispatcher;
    private DataCenter dataCenter;

    MTProtoBootstrap(Function<MTProtoOptions, ? extends O> optionsModifier, AuthorizationResources authorizationResources) {
        this.optionsModifier = optionsModifier;
        this.authorizationResources = authorizationResources;
    }

    public <O1 extends MTProtoOptions> MTProtoBootstrap<O1> setExtraOptions(Function<? super O, ? extends O1> optionsModifier) {
        return new MTProtoBootstrap<>(this.optionsModifier.andThen(optionsModifier), authorizationResources);
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

    public MTProtoBootstrap<O> setUpdatesHandlers(MTProtoClientManager mtProtoClientManager) {
        this.mtProtoClientManager = Objects.requireNonNull(mtProtoClientManager, "mtProtoClientManager");
        return this;
    }

    public MTProtoBootstrap<O> setRetry(RetryBackoffSpec retry) {
        this.retry = Objects.requireNonNull(retry, "retry");
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
            MTProtoClientManager mtProtoClientManager = initMtProtoClientManager();
            Sinks.Empty<Void> onDisconnect = Sinks.empty();

            MTProtoClient mtProtoClient = clientFactory.apply(optionsModifier.apply(
                    new MTProtoOptions(initDataCenter(), initTcpClient(), initTransport(),
                            storeLayout, acksSendThreshold, EmissionHandlers.DEFAULT_PARKING, initRetry())));

            MTProtoResources mtProtoResources = new MTProtoResources(mtProtoClientManager, storeLayout, eventDispatcher);
            ServiceHolder serviceHolder = new ServiceHolder(mtProtoClient, storeLayout);

            MTProtoTelegramClient telegramClient = new MTProtoTelegramClient(
                    authorizationResources, mtProtoClient,
                    mtProtoResources, updatesHandlers,
                    serviceHolder, onDisconnect.asMono());

            Mono<Void> disconnect = Mono.fromRunnable(() -> {
                eventDispatcher.shutdown();
                onDisconnect.emitEmpty(Sinks.EmitFailureHandler.FAIL_FAST);
                log.info("All mtproto clients disconnected.");
            });

            Disposable.Composite composite = Disposables.composite();

            composite.add(mtProtoClient.connect().subscribe());

            composite.add(mtProtoClient.updates().asFlux()
                    .takeUntilOther(onDisconnect.asMono())
                    .checkpoint("Event dispatch handler.")
                    .flatMap(telegramClient.getUpdatesManager()::handle)
                    .doOnNext(eventDispatcher::publish)
                    .subscribe());

            composite.add(mtProtoClient.state()
                    .takeUntilOther(onDisconnect.asMono())
                    .flatMap(state -> {
                        switch (state) {
                            case CLOSED:
                                mtProtoClientManager.remove(mtProtoClient.getDatacenter());

                                return Mono.just(mtProtoClientManager.activeCount())
                                        .filter(i -> i == 0)
                                        .flatMap(ig -> disconnect);
                            case CONNECTED:
                                mtProtoClientManager.add(mtProtoClient);

                                Mono<Void> fetchSelfId = storeLayout.getSelfId()
                                        .filter(l -> l != 0)
                                        .switchIfEmpty(serviceHolder.getUserService().getFullUser(InputUserSelf.instance())
                                                .flatMap(user -> storeLayout.updateSelfId(user.fullUser().id()))
                                                .then(Mono.empty()))
                                        .then();

                                return mtProtoClient.sendAwait(InvokeWithLayer.builder()
                                                .layer(MTProtoTelegramClient.LAYER)
                                                .query(initConnection())
                                                .build())
                                        .then(mtProtoClient.sendAwait(importAuthorization()))
                                        .then(fetchSelfId)
                                        .then(telegramClient.getUpdatesManager().fillGap())
                                        .thenReturn(telegramClient) // FIXME: reconnections drop signals
                                        .doOnNext(sink::success);
                            default:
                                return Mono.empty();
                        }
                    })
                    .subscribe());

            sink.onCancel(composite);
        });
    }

    // Resources initialization
    // ==========================

    private TlMethod<Authorization> importAuthorization() {
        switch (authorizationResources.getType()) {
            case BOT:
                return ImportBotAuthorization.builder()
                        .flags(0)
                        .apiId(authorizationResources.getAppId())
                        .apiHash(authorizationResources.getAppHash())
                        .botAuthToken(authorizationResources.getBotAuthToken()
                                .orElseThrow(IllegalStateException::new))
                        .build();
            case USER:
                throw new UnsupportedOperationException("User authorization hasn't yet implemented.");
            default:
                throw new IllegalStateException();
        }
    }

    private InitConnection<TlObject> initConnection() {
        InitConnectionParams params = initConnectionParams != null
                ? initConnectionParams
                : InitConnectionParams.getDefault();

        var initConnection = InitConnection.builder()
                .query(GetConfig.instance())
                .apiId(authorizationResources.getAppId())
                .appVersion(params.getAppVersion())
                .deviceModel(params.getDeviceModel())
                .langCode(params.getLangCode())
                .langPack(params.getLangPack())
                .systemVersion(params.getSystemVersion())
                .systemLangCode(params.getSystemLangCode());

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
        return new DefaultEventDispatcher(Schedulers.boundedElastic(),
                Sinks.many().multicast().onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false),
                EmissionHandlers.park(Duration.ofNanos(10)));
    }

    private StoreLayout initStoreLayout() {
        return storeLayout != null ? storeLayout : new StoreLayoutImpl();
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
        return Retry.fixedDelay(Integer.MAX_VALUE, Duration.ofSeconds(10));
    }

    private MTProtoClientManager initMtProtoClientManager() {
        return mtProtoClientManager != null ? mtProtoClientManager : new MTProtoClientManagerImpl();
    }
}
