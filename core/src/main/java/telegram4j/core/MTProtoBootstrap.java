package telegram4j.core;

import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.core.event.dispatcher.DefaultEventDispatcher;
import telegram4j.core.event.dispatcher.EventDispatcher;
import telegram4j.core.event.dispatcher.UpdatesHandlers;
import telegram4j.mtproto.*;
import telegram4j.mtproto.auth.AuthorizationContext;
import telegram4j.mtproto.auth.AuthorizationHandler;
import telegram4j.mtproto.auth.AuthorizationKeyHolder;
import telegram4j.mtproto.util.CryptoUtil;
import telegram4j.tl.TlMethod;
import telegram4j.tl.TlObject;
import telegram4j.tl.Updates;
import telegram4j.tl.auth.Authorization;
import telegram4j.tl.mtproto.FutureSalt;
import telegram4j.tl.request.InitConnection;
import telegram4j.tl.request.InvokeWithLayer;
import telegram4j.tl.request.auth.ImportBotAuthorization;
import telegram4j.tl.request.help.GetConfig;
import telegram4j.tl.request.mtproto.GetFutureSalts;
import telegram4j.tl.request.mtproto.Ping;
import telegram4j.tl.request.updates.GetState;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

public class MTProtoBootstrap<O extends MTProtoOptions> {

    private static final Logger log = Loggers.getLogger(MTProtoBootstrap.class);

    private static final Duration FUTURE_SALT_QUERY_PERIOD = Duration.ofMinutes(45);
    private static final Duration PING_QUERY_PERIOD = Duration.ofSeconds(10);

    private final Function<MTProtoOptions, ? extends O> optionsModifier;
    private final AuthorizationResources authorizationResources;

    private MTProtoResources mtProtoResources;
    private int acksSendThreshold = 3;
    private InitConnection<TlObject> initConnection;
    private EventDispatcher eventDispatcher;

    MTProtoBootstrap(Function<MTProtoOptions, ? extends O> optionsModifier, AuthorizationResources authorizationResources) {
        this.optionsModifier = optionsModifier;
        this.authorizationResources = authorizationResources;
    }

    public <O1 extends MTProtoOptions> MTProtoBootstrap<O1> setExtraOptions(Function<? super O, ? extends O1> optionsModifier) {
        return new MTProtoBootstrap<>(this.optionsModifier.andThen(optionsModifier), authorizationResources);
    }

    public MTProtoBootstrap<O> setMTProtoResources(MTProtoResources mtProtoResources) {
        this.mtProtoResources = Objects.requireNonNull(mtProtoResources, "mtProtoResources");
        return this;
    }

    public MTProtoBootstrap<O> setAcksSendThreshold(int acksSendThreshold) {
        this.acksSendThreshold = acksSendThreshold;
        return this;
    }

    public MTProtoBootstrap<O> setInitConnectionParams(InitConnection<TlObject> initConnection) {
        this.initConnection = Objects.requireNonNull(initConnection, "initConnection");
        return this;
    }

    public MTProtoBootstrap<O> setEventDispatcher(EventDispatcher eventDispatcher) {
        this.eventDispatcher = Objects.requireNonNull(eventDispatcher, "eventDispatcher");
        return this;
    }

    public Mono<MTProtoTelegramClient> connect() {
        return connect(DefaultMTProtoClient::new);
    }

    public Mono<MTProtoTelegramClient> connect(Function<? super O, ? extends MTProtoClient> clientFactory) {
        AuthorizationContext ctx = new AuthorizationContext();

        return Mono.fromSupplier(() -> clientFactory.apply(
                optionsModifier.apply(new MTProtoOptions(initMTProtoResources(), ctx, acksSendThreshold))))
                .flatMap(MTProtoClient::openSession)
                .publishOn(Schedulers.boundedElastic())
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(session -> Mono.create(sink -> {
                    Disposable.Composite composite = Disposables.composite();

                    Sinks.Empty<Void> onCloseSink = Sinks.empty();
                    Sinks.One<AuthorizationKeyHolder> onAuthSink = Sinks.one();

                    MTProtoOptions options = session.getClient().getOptions();
                    MTProtoTelegramClient telegramClient = new MTProtoTelegramClient(
                            authorizationResources, initEventDispatcher(),
                            onCloseSink.asMono(), session);

                    AuthorizationHandler authorizationHandler = new AuthorizationHandler(session);
                    RpcHandler rpcHandler = new RpcHandler(session);
                    UpdatesHandler updatesHandler = new UpdatesHandler(telegramClient, new UpdatesHandlers());

                    composite.add(session.rpcReceiver()
                            .takeUntilOther(onCloseSink.asMono())
                            .checkpoint("RPC handler.")
                            .flatMap(rpcHandler::handle)
                            .then()
                            .subscribe());

                    composite.add(session.updates().asFlux()
                            .takeUntilOther(onCloseSink.asMono())
                            .ofType(Updates.class)
                            .checkpoint("Event dispatch handler.")
                            .flatMap(updatesHandler::handle)
                            .doOnNext(telegramClient.getEventDispatcher()::publish)
                            .subscribe());

                    ResettableInterval futureSalt = new ResettableInterval(Schedulers.boundedElastic());

                    composite.add(futureSalt.ticks()
                            .takeUntilOther(onCloseSink.asMono())
                            .checkpoint("Future salt loop.")
                            .flatMap(tick -> session.sendEncrypted(GetFutureSalts.builder().num(1).build()))
                            .doOnNext(futureSalts -> {
                                FutureSalt salt = futureSalts.salts().get(0);
                                int delaySeconds = salt.validUntil() - futureSalts.now() - 900;
                                session.setServerSalt(salt.salt());

                                log.debug("Delaying future salt for {} seconds.", delaySeconds);

                                futureSalt.start(Duration.ofSeconds(delaySeconds), FUTURE_SALT_QUERY_PERIOD);
                            })
                            .subscribe());

                    Mono<Void> pingLoop = Flux.interval(PING_QUERY_PERIOD)
                            .takeUntilOther(onCloseSink.asMono())
                            .checkpoint("Ping loop.")
                            .flatMap(tick -> session.sendEncrypted(Ping.builder().pingId(CryptoUtil.random.nextInt()).build()))
                            .then();

                    Mono<Void> initializeConnection = session.sendEncrypted(InvokeWithLayer.builder()
                                    .layer(MTProtoTelegramClient.LAYER)
                                    .query(initConnectionParams())
                                    .build())
                            .then(session.sendEncrypted(InvokeWithLayer.builder()
                                    .layer(MTProtoTelegramClient.LAYER)
                                    .query(importAuthorization())
                                    .build()))
                            .doOnNext(ignored -> futureSalt.start(FUTURE_SALT_QUERY_PERIOD))
                            .then(session.sendEncrypted(GetState.instance()))
                            .then();

                    composite.add(options.getResources().getStoreLayout()
                            .getAuthorizationKey()
                            .doOnNext(key -> onAuthSink.emitValue(key, Sinks.EmitFailureHandler.FAIL_FAST))
                            .switchIfEmpty(authorizationHandler.start().then(onAuthSink.asMono()))
                            .doOnNext(session::setAuthorizationKey)
                            .delayUntil(ignored -> initializeConnection)
                            .doOnNext(ignored -> sink.success(telegramClient))
                            .flatMap(ignored -> pingLoop)
                            .subscribe());

                    sink.onCancel(composite);
                }));
    }

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
            case USER: throw new UnsupportedOperationException("User authorization hasn't yet implemented.");
            default: throw new IllegalStateException();
        }
    }

    private MTProtoResources initMTProtoResources() {
        if (mtProtoResources != null) {
            return mtProtoResources;
        }
        return new MTProtoResources();
    }

    private InitConnection<TlObject> initConnectionParams() {
        if (initConnection != null) {
            return initConnection;
        }
        return InitConnection.builder()
                .apiId(authorizationResources.getAppId())
                .appVersion("0.1.0")
                .deviceModel("telegram4j")
                .langCode("en")
                .langPack("")
                .systemVersion("0.1.0")
                .systemLangCode("en")
                .query(GetConfig.instance())
                .build();
    }

    private EventDispatcher initEventDispatcher() {
        if (eventDispatcher != null) {
            return eventDispatcher;
        }
        return new DefaultEventDispatcher(Schedulers.boundedElastic(),
                Sinks.many().multicast().onBackpressureBuffer());
    }
}
