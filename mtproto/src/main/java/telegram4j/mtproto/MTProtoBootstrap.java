package telegram4j.mtproto;

import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import telegram4j.mtproto.crypto.MTProtoAuthorizationContext;
import telegram4j.mtproto.crypto.MTProtoAuthorizationHandler;

import java.util.Objects;
import java.util.function.Function;

public class MTProtoBootstrap<O extends MTProtoOptions> {

    private final Function<MTProtoOptions, ? extends O> optionsModifier;

    private MTProtoResources mtProtoResources;
    private int acksSendThreshold = 5;

    MTProtoBootstrap(Function<MTProtoOptions, ? extends O> optionsModifier) {
        this.optionsModifier = optionsModifier;
    }

    public <O1 extends MTProtoOptions> MTProtoBootstrap<O1> setExtraOptions(Function<? super O, ? extends O1> optionsModifier) {
        return new MTProtoBootstrap<>(this.optionsModifier.andThen(optionsModifier));
    }

    public MTProtoBootstrap<O> setMTProtoResources(MTProtoResources resources) {
        this.mtProtoResources = Objects.requireNonNull(resources, "resources");
        return this;
    }

    public void setAcksSendThreshold(int acksSendThreshold) {
        if (acksSendThreshold < 0) {
            throw new IllegalArgumentException("acksSendThreshold must be positive.");
        }
        this.acksSendThreshold = acksSendThreshold;
    }

    public Mono<MTProtoTelegramClient> connect() {
        return connect(DefaultMTProtoClient::new);
    }

    public Mono<MTProtoTelegramClient> connect(Function<? super O, ? extends MTProtoClient> clientFactory) {
        MTProtoAuthorizationContext ctx = new MTProtoAuthorizationContext();

        return Mono.fromSupplier(() -> clientFactory.apply(
                optionsModifier.apply(new MTProtoOptions(initMTProtoResources(), ctx, acksSendThreshold))))
                .flatMap(MTProtoClient::openSession)
                .publishOn(Schedulers.boundedElastic())
                .flatMap(session -> Mono.create(sink -> {
                    Disposable.Composite composite = Disposables.composite();

                    Sinks.Empty<Void> onCloseSink = Sinks.empty();
                    Sinks.One<Boolean> onAuthSink = Sinks.one();
                    MTProtoTelegramClient telegramClient = new MTProtoTelegramClient(onCloseSink.asMono(), session);

                    MTProtoAuthorizationHandler authorizationHandler = new MTProtoAuthorizationHandler(session, onAuthSink);
                    RpcHandler rpcHandler = new RpcHandler(session);

                    composite.add(session.receiver()
                            .takeUntilOther(onCloseSink.asMono())
                            .takeUntilOther(onAuthSink.asMono())
                            .checkpoint("Authorization key generation.")
                            .flatMap(authorizationHandler::handle)
                            .subscribe());

                    composite.add(session.receiver()
                            .takeUntilOther(onCloseSink.asMono())
                            .delayUntil(buf -> onAuthSink.asMono())
                            .checkpoint("RPC handler.")
                            .flatMap(rpcHandler::handle)
                            .then()
                            .subscribe());

                    // generate auth-key if it hasn't yet generated.
                    Mono<Void> authorization = ctx.isAuthorized() ? Mono.empty() : authorizationHandler.start().then();

                    composite.add(authorization.subscribe());

                    composite.add(onAuthSink.asMono()
                            .filter(Boolean::booleanValue)
                            .doOnNext(bool -> sink.success(telegramClient))
                            .subscribe());

                    sink.onCancel(composite);
                }));
    }

    private MTProtoResources initMTProtoResources() {
        if (mtProtoResources != null) {
            return mtProtoResources;
        }
        return new MTProtoResources();
    }
}
