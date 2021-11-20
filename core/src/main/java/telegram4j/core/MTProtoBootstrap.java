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

import java.util.Objects;
import java.util.function.Function;

public class MTProtoBootstrap<O extends MTProtoOptions> {

    private final Function<MTProtoOptions, ? extends O> optionsModifier;
    private final TelegramResources telegramResources;

    private MTProtoResources mtProtoResources;
    private int acksSendThreshold = 5;

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

    public void setAcksSendThreshold(int acksSendThreshold) {
        this.acksSendThreshold = acksSendThreshold;
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
                .flatMap(session -> Mono.create(sink -> {
                    Disposable.Composite composite = Disposables.composite();

                    Sinks.Empty<Void> onCloseSink = Sinks.empty();
                    Sinks.One<AuthorizationKeyHolder> onAuthSink = Sinks.one();

                    MTProtoOptions options = session.getClient().getOptions();
                    MTProtoTelegramClient telegramClient = new MTProtoTelegramClient(telegramResources, onCloseSink.asMono(), session);

                    AuthorizationHandler authorizationHandler = new AuthorizationHandler(session, onAuthSink);
                    RpcHandler rpcHandler = new RpcHandler(session);

                    composite.add(options.getResources().getStoreLayout()
                            .getAuthorizationKey()
                            .doOnNext(key -> onAuthSink.emitValue(key, Sinks.EmitFailureHandler.FAIL_FAST))
                            .switchIfEmpty(authorizationHandler.start().then(onAuthSink.asMono()))
                            .doOnNext(session::setAuthorizationKey)
                            .doOnNext(ignored -> sink.success(telegramClient))
                            .subscribe());

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
