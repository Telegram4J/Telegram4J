package telegram4j.core.auth;

import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.annotation.Nullable;
import telegram4j.core.AuthorizationHandler;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.DcId;
import telegram4j.mtproto.RpcException;
import telegram4j.mtproto.internal.Preconditions;
import telegram4j.tl.CodeSettings;
import telegram4j.tl.ImmutableCodeSettings;
import telegram4j.tl.auth.Authorization;
import telegram4j.tl.auth.BaseAuthorization;
import telegram4j.tl.auth.BaseSentCode;
import telegram4j.tl.auth.SentCode;
import telegram4j.tl.mtproto.RpcError;
import telegram4j.tl.request.auth.*;

import java.util.Objects;

public class CodeAuthorizationHandler implements AuthorizationHandler {
    protected static final Logger log = Loggers.getLogger(CodeAuthorizationHandler.class);

    protected final Callback callback;

    public CodeAuthorizationHandler(Callback callback) {
        this.callback = Objects.requireNonNull(callback);
    }

    @Override
    public Mono<BaseAuthorization> process(Resources res) {
        return Mono.defer(() -> {
            var ctx = new Context();

            return sendCode(res, ctx)
                    .flatMap(sentCode -> onSentCode(res, ctx, sentCode))
                    .onErrorResume(RpcException.isErrorMessage("SESSION_PASSWORD_NEEDED"), e -> {
                        var tfa = new TwoFactorHandler(res, callback, ctx.id);

                        return tfa.process();
                    })
                    // sign up unsupported
                    .cast(BaseAuthorization.class);

        });
    }

    public interface Callback extends TwoFactorHandler.Callback {

        default Mono<CodeSettings> getCodeSettings(Resources res) {
            return Mono.just(ImmutableCodeSettings.of());
        }

        Mono<PhoneNumberAction> onPhoneNumber(Resources res, PhoneNumberContext ctx);

        default Mono<ActionType> onPhoneNumberError(Resources res, PhoneNumberContext ctx, RpcError rpcError) {
            return Mono.fromSupplier(() -> {
                if (rpcError.errorCode() == 500) {
                    ctx.logInfo("Internal error occurred during authorization, code: " + rpcError.errorCode()
                            + ", message: " + rpcError.errorMessage());
                    return ActionType.STOP;
                }

                ctx.logInfo("Specified phone number is invalid, retrying...");
                return ActionType.RETRY;
            });
        }

        Mono<CodeAction> onSentCode(Resources res, PhoneCodeContext ctx);

        default Mono<ActionType> onSentCodeError(Resources res, PhoneCodeContext ctx, RpcError rpcError) {
            return Mono.fromSupplier(() -> {
                if (rpcError.errorCode() == 500) {
                    ctx.logInfo("Internal error occurred during authorization, code: " + rpcError.errorCode()
                            + ", message: " + rpcError.errorMessage());
                    return ActionType.STOP;
                }

                ctx.logInfo("Specified phone code is invalid, resending new...");
                return ActionType.RETRY;
            });
        }
    }

    public interface PhoneNumberContext {

        void logInfo(String message);
    }

    public sealed interface PhoneNumberAction {
        static PhoneNumber of(String value) {
            return new PhoneNumber(value);
        }

        static CancelAction cancel() {
            return CancelAction.instance;
        }
    }
    public record PhoneNumber(String value) implements PhoneNumberAction {
        public PhoneNumber {
            Objects.requireNonNull(value);
        }
    }
    public static final class CancelAction implements PhoneNumberAction {
        private static final CancelAction instance = new CancelAction();

        private CancelAction() {}
    }

    public interface PhoneCodeContext extends PhoneNumberContext {

        /** {@return A cached phone number produced by previous auth step} */
        String phoneNumber();

        /** {@return Information about current verification code} */
        BaseSentCode sentCode();
    }

    public sealed interface CodeAction {
        static Code of(String value) {
            return new Code(value);
        }

        static CancelCodeAction cancel() {
            return CancelCodeAction.instance;
        }

        static ResendCodeAction resend() {
            return ResendCodeAction.instance;
        }
    }
    public record Code(String value) implements CodeAction {
        public Code {
            Objects.requireNonNull(value);
        }
    }
    public static final class CancelCodeAction implements CodeAction {
        private static final CancelCodeAction instance = new CancelCodeAction();

        private CancelCodeAction() {}
    }
    public static final class ResendCodeAction implements CodeAction {
        private static final ResendCodeAction instance = new ResendCodeAction();

        private ResendCodeAction() {}
    }

    // Implementation code
    // ======================

    protected Mono<SentCode> migrateTo(Resources res, Context ctx,
                                       int dcId, SendCode originalRequest) {

        ctx.logInfo("Redirecting to DC " + dcId);

        return res.storeLayout().getDcOptions()
                .map(dcOpts -> dcOpts.find(DataCenter.Type.REGULAR, dcId)
                        .orElseThrow(() -> new IllegalStateException("Could not find DC " + dcId
                                + " for redirecting main client")))
                .flatMap(res.clientGroup()::setMain)
                .flatMap(client -> client.send(originalRequest));
    }

    protected Mono<Authorization> onSentCode(Resources res, Context ctx, SentCode sentCode) {
        // TODO: why new subtype of SentCode was added?
        if (!(sentCode instanceof BaseSentCode b)) {
            return Mono.error(new IllegalStateException());
        }

        ctx.sentCode = b;
        return sentCode(res, ctx);
    }

    protected Mono<Authorization> sentCode(Resources res, Context ctx) {
        String phoneNumber = ctx.phoneNumber();
        BaseSentCode sentCode = ctx.sentCode();
        return callback.onSentCode(res, ctx)
                .flatMap(codeAction -> {
                    if (codeAction == CodeAction.cancel()) {
                        return cancelCode(res, ctx);
                    } else if (codeAction == CodeAction.resend()) {
                        return resendCode(res, ctx);
                    } else if (codeAction instanceof Code c) {
                        return res.clientGroup().send(DcId.main(), SignIn.builder()
                                .phoneNumber(phoneNumber)
                                .phoneCode(c.value)
                                .phoneCodeHash(sentCode.phoneCodeHash())
                                .build());
                    } else {
                        throw new IllegalStateException();
                    }
                })
                .onErrorResume(RpcException.class, e ->
                        callback.onSentCodeError(res, ctx, e.getError())
                                .flatMap(actionType -> switch (actionType) {
                                    case RETRY -> resendCode(res, ctx);
                                    case STOP -> this.<Authorization>cancelCode(res, ctx);
                                }));
    }

    protected Mono<Authorization> resendCode(Resources res, Context ctx) {
        String phoneNumber = ctx.phoneNumber();
        BaseSentCode sentCode = ctx.sentCode();
        return res.clientGroup().send(DcId.main(), ImmutableResendCode.of(phoneNumber, sentCode.phoneCodeHash()))
                .flatMap(newSentCode -> onSentCode(res, ctx, newSentCode));
    }

    protected <T> Mono<T> cancelCode(Resources res, Context ctx) {
        String phoneNumber = ctx.phoneNumber();
        BaseSentCode sentCode = ctx.sentCode();
        return res.clientGroup().send(DcId.main(), ImmutableCancelCode.of(phoneNumber, sentCode.phoneCodeHash()))
                .then(cancel(res, ctx));
    }

    protected <T> Mono<T> cancel(Resources res, Context ctx) {
        return Mono.empty();
    }

    protected Mono<SentCode> sendCode(Resources res, Context ctx) {
        return callback.onPhoneNumber(res, ctx)
                .flatMap(action -> {
                    if (action == PhoneNumberAction.cancel()) {
                        return cancel(res, ctx);
                    } else if (action instanceof PhoneNumber p) {
                        return callback.getCodeSettings(res)
                                .map(sett -> ImmutableSendCode.of(p.value, res.authResources().getApiId(),
                                        res.authResources().getApiHash(), sett));
                    } else {
                        throw new IllegalStateException();
                    }
                })
                .flatMap(sendCode -> {
                    ctx.phoneNumber = sendCode.phoneNumber();

                    return res.clientGroup().send(DcId.main(), sendCode);
                })
                .onErrorResume(RpcException.isErrorCode(303), e -> {
                    var rpcExc = (RpcException) e;
                    String msg = rpcExc.getError().errorMessage();
                    if (!msg.startsWith("PHONE_MIGRATE_"))
                        return Mono.error(new IllegalStateException("Unexpected type of DC redirection", e));

                    int dcId = Integer.parseInt(msg.substring(14));

                    return migrateTo(res, ctx, dcId, (SendCode) rpcExc.getMethod());
                })
                .onErrorResume(RpcException.class, e ->
                        callback.onPhoneNumberError(res, ctx, e.getError())
                                .flatMap(type -> switch (type) {
                                    case STOP -> this.<SentCode>cancel(res, ctx);
                                    case RETRY -> sendCode(res, ctx);
                                }));
    }

    protected static class Context extends AuthContext implements PhoneCodeContext {
        @Nullable
        protected String phoneNumber;
        @Nullable
        protected BaseSentCode sentCode;

        @Override
        protected void logInfo0(String message) {
            log.info(message);
        }

        // Public API
        // =============

        @Override
        public final String phoneNumber() {
            String pn = phoneNumber;
            Preconditions.requireState(pn != null, "phoneNumber has not initialized yet");
            return pn;
        }

        @Override
        public final BaseSentCode sentCode() {
            BaseSentCode sc = sentCode;
            Preconditions.requireState(sc != null, "sentCode has not initialized yet");
            return sc;
        }
    }
}
