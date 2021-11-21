package telegram4j.core;

import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import telegram4j.mtproto.*;
import telegram4j.mtproto.auth.AuthorizationContext;
import telegram4j.mtproto.auth.AuthorizationHandler;
import telegram4j.mtproto.auth.AuthorizationKeyHolder;
import telegram4j.mtproto.payload.PayloadMapperStrategy;
import telegram4j.tl.TlObject;
import telegram4j.tl.mtproto.FutureSalt;
import telegram4j.tl.request.InitConnection;
import telegram4j.tl.request.InvokeWithLayer;
import telegram4j.tl.request.auth.ImportBotAuthorization;
import telegram4j.tl.request.help.GetConfig;
import telegram4j.tl.request.mtproto.GetFutureSalts;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

public class MTProtoBootstrap<O extends MTProtoOptions> {

    private static final Duration FUTURE_SALT_QUERY_PERIOD = Duration.ofMinutes(45);

    private final Function<MTProtoOptions, ? extends O> optionsModifier;
    private final TelegramResources telegramResources;

    private MTProtoResources mtProtoResources;
    private int acksSendThreshold = 5;
    private InitConnection<TlObject> initConnection;

    MTProtoBootstrap(Function<MTProtoOptions, ? extends O> optionsModifier, TelegramResources telegramResources) {
        this.optionsModifier = optionsModifier;
        this.telegramResources = telegramResources;
    }

    public <O1 extends MTProtoOptions> MTProtoBootstrap<O1> setExtraOptions(Function<? super O, ? extends O1> optionsModifier) {
        return new MTProtoBootstrap<>(this.optionsModifier.andThen(optionsModifier), telegramResources);
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
                    MTProtoTelegramClient telegramClient = new MTProtoTelegramClient(telegramResources, onCloseSink.asMono(), session);

                    AuthorizationHandler authorizationHandler = new AuthorizationHandler(session, onAuthSink);
                    RpcHandler rpcHandler = new RpcHandler(session);

                    composite.add(session.receiver()
                            .takeUntilOther(onCloseSink.asMono())
                            .takeUntilOther(onAuthSink.asMono())
                            .checkpoint("Authorization key generation.")
                            .flatMap(authorizationHandler::handle)
                            .subscribe());

                    composite.add(session.receiver()
                            .takeUntilOther(onCloseSink.asMono())
                            .filter(buf -> buf.getLongLE(buf.readerIndex()) != 0)
                            .checkpoint("RPC handler.")
                            .flatMap(rpcHandler::handle)
                            .then()
                            .subscribe());

                    composite.add(session.dispatch().asFlux()
                            .takeUntilOther(onCloseSink.asMono())
                            .checkpoint("Event dispatch handler.")
                            .log()
                            .subscribe());

                    ResettableInterval futureSalt = new ResettableInterval(Schedulers.boundedElastic());

                    composite.add(futureSalt.ticks()
                            .takeUntilOther(onCloseSink.asMono())
                            .checkpoint("Future salt loop.")
                            .flatMap(tick -> session
                                    .withPayloadMapper(PayloadMapperStrategy.ENCRYPTED)
                                    .send(GetFutureSalts.builder().num(1).build()))
                            .doOnNext(futureSalts -> {
                                FutureSalt salt = futureSalts.salts().get(0);
                                int delaySeconds = salt.validUntil() - futureSalts.now() - 900;
                                session.setServerSalt(salt.salt());

                                futureSalt.start(Duration.ofSeconds(delaySeconds), FUTURE_SALT_QUERY_PERIOD);
                            })
                            .subscribe());

                    Mono<Void> initializeConnection = session
                            .withPayloadMapper(PayloadMapperStrategy.ENCRYPTED)
                            .send(InvokeWithLayer.builder()
                                    .layer(MTProtoTelegramClient.LAYER)
                                    .query(initConnectionParams())
                                    .build())
                            .then(session.withPayloadMapper(PayloadMapperStrategy.ENCRYPTED)
                                    .send(InvokeWithLayer.builder()
                                            .layer(MTProtoTelegramClient.LAYER)
                                            .query(importAuthorization())
                                            .build()))
                            .doOnNext(ignored -> futureSalt.start(FUTURE_SALT_QUERY_PERIOD))
                            .then();

                    composite.add(options.getResources().getStoreLayout()
                            .getAuthorizationKey()
                            .doOnNext(key -> onAuthSink.emitValue(key, Sinks.EmitFailureHandler.FAIL_FAST))
                            .switchIfEmpty(authorizationHandler.start().then(onAuthSink.asMono()))
                            .doOnNext(session::setAuthorizationKey)
                            .delayUntil(ignored -> initializeConnection)
                            .doOnNext(ignored -> sink.success(telegramClient))
                            .subscribe());

                    sink.onCancel(composite);
                }));
    }

    private TlObject importAuthorization() {
        switch (telegramResources.getAuthorizationType()) {
            case BOT:
                return ImportBotAuthorization.builder()
                        .flags(0)
                        .apiId(telegramResources.getAppId())
                        .apiHash(telegramResources.getAppHash())
                        .botAuthToken(telegramResources.getBotAuthToken()
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
                .apiId(telegramResources.getAppId())
                .appVersion("0.1.0")
                .deviceModel("telegram4j")
                .langCode("en")
                .langPack("")
                .systemVersion("0.1.0")
                .systemLangCode("en")
                .query(GetConfig.instance())
                .build();
    }
}
