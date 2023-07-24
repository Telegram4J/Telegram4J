package telegram4j.core.auth;

import reactor.core.Disposables;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.core.AuthorizationHandler;
import telegram4j.core.util.Timeout;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.DcId;
import telegram4j.mtproto.RpcException;
import telegram4j.mtproto.internal.Preconditions;
import telegram4j.mtproto.util.CryptoUtil;
import telegram4j.tl.UpdateLoginToken;
import telegram4j.tl.UpdateShort;
import telegram4j.tl.auth.*;
import telegram4j.tl.request.auth.ImmutableExportLoginToken;
import telegram4j.tl.request.auth.ImmutableImportLoginToken;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public class QRAuthorizationHandler implements AuthorizationHandler {

    protected static final Logger log = Loggers.getLogger(QRAuthorizationHandler.class);

    protected final Callback callback;

    public QRAuthorizationHandler(Callback callback) {
        this.callback = Objects.requireNonNull(callback);
    }

    @Override
    public Mono<BaseAuthorization> process(Resources res) {
        return Mono.create(sink -> {
            var ctx = new Context();

            var updateToken = res.clientGroup().updates().on(UpdateShort.class)
                    .takeUntil(u -> ctx.complete)
                    .filter(u -> u.update() == UpdateLoginToken.instance())
                    .flatMap(notify -> exportLoginToken(res))
                    .flatMap(token -> {
                        ctx.setComplete();

                        return switch (token.identifier()) {
                            case LoginTokenSuccess.ID -> {
                                var success = (LoginTokenSuccess) token;
                                yield Mono.just(success.authorization());
                            }
                            case LoginTokenMigrateTo.ID -> {
                                var migrate = (LoginTokenMigrateTo) token;
                                yield onMigrateTo(res, ctx, migrate);
                            }
                            default -> Mono.error(new IllegalStateException("Unexpected type of LoginToken: " + token));
                        };
                    })
                    .onErrorResume(RpcException.isErrorMessage("SESSION_PASSWORD_NEEDED"), e -> {
                        var tfa = new TwoFactorHandler(res, callback, ctx.id);

                        return tfa.process();
                    })
                    .cast(BaseAuthorization.class)
                    .subscribe(sink::success, sink::error);

            var tokenExport = ctx.regenerateTimeout.asFlux()
                    .flatMap(tick -> exportLoginToken(res))
                    .cast(BaseLoginToken.class)
                    .flatMap(token -> {
                        ctx.init(token);

                        return callback.onLoginToken(res, ctx);
                    })
                    .subscribe(null, sink::error);

            ctx.regenerateTimeout.restart(Duration.ZERO);

            sink.onCancel(Disposables.composite(updateToken, tokenExport));
        });
    }

    protected Mono<LoginToken> exportLoginToken(Resources res) {
        return res.clientGroup().send(DcId.main(),
                ImmutableExportLoginToken.of(res.authResources().getApiId(),
                        res.authResources().getApiHash(), List.of()));
    }

    protected Mono<Authorization> onMigrateTo(Resources res, Context ctx, LoginTokenMigrateTo migrate) {
        ctx.logInfo("Redirecting to the DC " + migrate.dcId());

        return res.storeLayout().getDcOptions()
                // TODO: request help.getConfig if DC not found
                .map(dcOpts -> dcOpts.find(DataCenter.Type.REGULAR, migrate.dcId())
                        .orElseThrow(() -> new IllegalStateException("Could not find DC " + migrate.dcId()
                                + " for redirecting main client")))
                .flatMap(res.clientGroup()::setMain)
                .flatMap(client -> client.send(ImmutableImportLoginToken.of(migrate.token())))
                .cast(LoginTokenSuccess.class)
                .map(LoginTokenSuccess::authorization);
    }

    public interface Callback extends TwoFactorHandler.Callback {

        Mono<Void> onLoginToken(Resources res, Context ctx);
    }

    public static class Context extends AuthContext {
        protected final Timeout regenerateTimeout = Timeout.create(Schedulers.single(), Sinks.many().unicast().onBackpressureError());

        protected volatile boolean complete;
        protected String loginUrl;
        protected Duration expiresIn;

        public final String loginUrl() {
            var lt = loginUrl;
            Preconditions.requireState(lt != null, "loginUrl has not initialized yet");
            return lt;
        }

        public final Duration expiresIn() {
            Duration ei = expiresIn;
            Preconditions.requireState(ei != null, "expiresIn has not initialized yet");
            return ei;
        }

        protected void init(BaseLoginToken loginToken) {
            String token = java.util.Base64.getUrlEncoder().encodeToString(CryptoUtil.toByteArray(loginToken.token()));

            loginUrl = "tg://login?token=" + token;

            long expiresInSeconds = loginToken.expires() - (System.currentTimeMillis()/1000);
            expiresIn = Duration.ofSeconds(expiresInSeconds);
            regenerateTimeout.restart(expiresIn);
        }

        protected void setComplete() {
            regenerateTimeout.close();
            loginUrl = null;
            expiresIn = null;
            complete = true;
        }

        @Override
        protected void logInfo0(String message) {
            log.info(message);
        }
    }

    // Implementation code
    // ======================
}
